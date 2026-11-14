package br.com.shirokaidex.achievements;

import br.com.shirokaidex.captures.*;
import br.com.shirokaidex.profiles.ProfileProgress;
import com.google.gson.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;

/** Visual medals only. Evaluation and proof snapshots run in the same database transaction. */
public final class AchievementStore {
    private AchievementStore() { }
    private static final Gson JSON=new GsonBuilder().registerTypeAdapter(Instant.class,(JsonSerializer<Instant>)(v,t,c)->new JsonPrimitive(v.toString())).registerTypeAdapter(Instant.class,(JsonDeserializer<Instant>)(v,t,c)->Instant.parse(v.getAsString())).create();
    private record Existing(boolean active,Long unlocked,String definition,String baseline) { }
    public record Medal(AchievementDefinition definition,boolean active,Instant unlockedAt,Integer favoriteSlot) { }
    public static void preserve(Connection c,List<AchievementDefinition> definitions,Instant now)throws SQLException {
        try(var q=c.prepareStatement("INSERT INTO achievement_definitions VALUES(?,?,?,?) ON CONFLICT(achievement_id) DO UPDATE SET fingerprint=excluded.fingerprint,definition_json=excluded.definition_json WHERE fingerprint<>excluded.fingerprint")){
            for(var definition:definitions){String json=AchievementJson.write(definition);q.setString(1,definition.id());q.setLong(2,now.toEpochMilli());q.setString(3,hash(json));q.setString(4,json);q.addBatch();}q.executeBatch();
        }
    }
    public static AchievementFacts facts(Connection c,UUID player,List<ProfileProgress.Proof> progress)throws SQLException {
        var counts=new HashMap<String,Long>();
        try(var q=c.prepareStatement("SELECT total,shiny,legendary,mythical FROM capture_totals WHERE player_uuid=?")){q.setString(1,player.toString());try(var rows=q.executeQuery()){boolean exists=rows.next();int i=1;for(String metric:List.of("TOTAL","SHINY","LEGENDARY","MYTHICAL"))counts.put(metric+"|",exists?rows.getLong(i++):0L);}}
        try(var q=c.prepareStatement("SELECT kind,entry_key,total FROM capture_counts WHERE player_uuid=?")){q.setString(1,player.toString());try(var rows=q.executeQuery()){while(rows.next())counts.put(rows.getString(1)+"|"+rows.getString(2),rows.getLong(3));}}
        var active=new HashSet<String>();try(var q=c.prepareStatement("SELECT achievement_id FROM player_achievements WHERE player_uuid=? AND active=1")){q.setString(1,player.toString());try(var rows=q.executeQuery()){while(rows.next())active.add(rows.getString(1));}}
        var proofs=new HashMap<String,ProfileProgress.Proof>();for(var proof:progress)proofs.put(proof.collection(),proof);
        return new AchievementFacts(counts,CapturedForms.forms(c,player),proofs,active);
    }
    public static List<String> evaluate(Connection c,UUID player,List<AchievementDefinition> definitions,List<ProfileProgress.Proof> progress,Instant now)throws SQLException {
        var facts=facts(c,player,progress);var active=new HashSet<>(facts.achievements());var notifications=new ArrayList<String>();var existing=new HashMap<String,Existing>();
        try(var q=c.prepareStatement("SELECT achievement_id,active,unlocked_at,definition_json,baseline_json FROM player_achievements WHERE player_uuid=?")){q.setString(1,player.toString());try(var rows=q.executeQuery()){while(rows.next()){long at=rows.getLong(3);Long unlocked=rows.wasNull()?null:at;existing.put(rows.getString(1),new Existing(rows.getBoolean(2),unlocked,rows.getString(4),rows.getString(5)));}}}
        for(var definition:definitions){
            var previous=existing.get(definition.id());String previousJson=previous==null?null:previous.definition(),baselineJson=previous==null?null:previous.baseline();boolean previousActive=previous!=null&&previous.active();Long unlockedAt=previous==null?null:previous.unlocked();String originalBaseline=baselineJson;
            AchievementFacts baseline=null;
            boolean waitingForBaseline=!definition.retroactive()&&baselineJson==null&&!definition.requirement().baselineReady(facts);
            if(!definition.retroactive()&&!waitingForBaseline){
                if(baselineJson==null)baselineJson=JSON.toJson(Requirement.relevant(facts,definition.requirement()));
                try{baseline=JSON.fromJson(baselineJson,AchievementFacts.class);}catch(RuntimeException invalid){throw new SQLException("Invalid achievement baseline",invalid);}
            }
            var evaluation=waitingForBaseline?Requirement.Result.UNKNOWN:definition.dependencies().stream().allMatch(active::contains)?definition.requirement().evaluate(facts.withAchievements(active),baseline):Requirement.Result.NO_MATCH;
            boolean next=previousActive;
            if(evaluation==Requirement.Result.MATCH)next=true;
            else if(evaluation==Requirement.Result.NO_MATCH&&definition.policy()==AchievementDefinition.Policy.RECALCULATE)next=false;
            if(next)active.add(definition.id());else active.remove(definition.id());
            String json=AchievementJson.write(definition);boolean firstUnlock=next&&unlockedAt==null;
            if(firstUnlock)unlockedAt=now.toEpochMilli();
            if(previousJson==null||next!=previousActive||!json.equals(previousJson)||!Objects.equals(originalBaseline,baselineJson)||firstUnlock)try(var q=c.prepareStatement("INSERT INTO player_achievements VALUES(?,?,?,?,?,?,?) ON CONFLICT(player_uuid,achievement_id) DO UPDATE SET active=excluded.active,unlocked_at=excluded.unlocked_at,updated_at=excluded.updated_at,definition_json=excluded.definition_json,baseline_json=excluded.baseline_json")){
                q.setString(1,player.toString());q.setString(2,definition.id());q.setBoolean(3,next);if(unlockedAt==null)q.setNull(4,Types.BIGINT);else q.setLong(4,unlockedAt);q.setLong(5,now.toEpochMilli());q.setString(6,json);q.setString(7,baselineJson);q.executeUpdate();
            }
            if(next!=previousActive){
                try(var q=c.prepareStatement("INSERT INTO achievement_audit(player_uuid,achievement_id,action,definition_fingerprint,evidence_json,recorded_at) VALUES(?,?,?,?,?,?)")){q.setString(1,player.toString());q.setString(2,definition.id());q.setString(3,next?"UNLOCKED":"DEACTIVATED");q.setString(4,hash(json));q.setString(5,JSON.toJson(Requirement.relevant(facts.withAchievements(active),definition.requirement())));q.setLong(6,now.toEpochMilli());q.executeUpdate();}
            }
            if(firstUnlock&&definition.notifyPlayer())notifications.add(definition.id());
        }
        return List.copyOf(notifications);
    }
    public static List<Medal> medals(Connection c,UUID player)throws SQLException {
        var result=new ArrayList<Medal>();try(var q=c.prepareStatement("SELECT p.active,p.unlocked_at,p.definition_json,f.slot FROM player_achievements p LEFT JOIN player_medal_favorites f ON f.player_uuid=p.player_uuid AND f.achievement_id=p.achievement_id WHERE p.player_uuid=? ORDER BY p.achievement_id")){
            q.setString(1,player.toString());try(var rows=q.executeQuery()){while(rows.next()){long unlocked=rows.getLong(2);Instant at=rows.wasNull()?null:Instant.ofEpochMilli(unlocked);int slot=rows.getInt(4);Integer favorite=rows.wasNull()?null:slot;try{result.add(new Medal(AchievementJson.read(JsonParser.parseString(rows.getString(3)).getAsJsonObject()),rows.getBoolean(1),at,favorite));}catch(RuntimeException invalid){throw new SQLException("Invalid saved medal",invalid);}}}
        }return List.copyOf(result);
    }
    public static boolean favorite(Connection c,UUID player,String achievement,int slot)throws SQLException {
        if(slot < -1||slot>2)throw new IllegalArgumentException("Invalid favorite slot");
        try(var q=c.prepareStatement("SELECT active FROM player_achievements WHERE player_uuid=? AND achievement_id=?")){q.setString(1,player.toString());q.setString(2,achievement);try(var rows=q.executeQuery()){if(!rows.next()||!rows.getBoolean(1))return false;}}
        try(var q=c.prepareStatement("DELETE FROM player_medal_favorites WHERE player_uuid=? AND achievement_id=?")){q.setString(1,player.toString());q.setString(2,achievement);q.executeUpdate();}
        if(slot>=0)try(var q=c.prepareStatement("INSERT INTO player_medal_favorites VALUES(?,?,?) ON CONFLICT(player_uuid,slot) DO UPDATE SET achievement_id=excluded.achievement_id")){q.setString(1,player.toString());q.setInt(2,slot);q.setString(3,achievement);q.executeUpdate();}return true;
    }
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
}
