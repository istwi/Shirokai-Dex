package br.com.shirokaidex.competitions;

import br.com.shirokaidex.rewards.*;
import com.google.gson.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;

public final class CompetitionAwards {
    private CompetitionAwards() { }
    public record Award(String id,String edition,UUID player,int sourcePosition,int band,RewardPlan plan,Instant assignedAt,Instant expiresAt) {
        public boolean available(Instant now){return expiresAt==null||now.isBefore(expiresAt);}
    }
    public static String templates(PrizePolicy policy,List<RewardPlan> definitions){
        var json=new JsonObject();if(policy.enabled())for(var band:policy.bands()){
            var definition=definitions.stream().filter(p->p.id().equals(band.reward())).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown competition reward"));json.addProperty(band.reward(),RewardSnapshot.encode(definition.definition()));
        }return json.toString();
    }
    public static void assign(Connection c,CompetitionStore.Edition edition,Instant now)throws SQLException{
        var policy=edition.definition().prizes();if(!policy.enabled())return;
        var candidates=new ArrayList<PrizePolicy.Candidate>();
        try(var q=c.prepareStatement("SELECT player_uuid,position,score,eligible FROM competition_results WHERE edition_id=? ORDER BY position")){q.setString(1,edition.id());try(var rows=q.executeQuery()){while(rows.next())candidates.add(new PrizePolicy.Candidate(UUID.fromString(rows.getString(1)),rows.getInt(2),rows.getLong(3),rows.getBoolean(4)));}}
        JsonObject templates;
        try(var q=c.prepareStatement("SELECT reward_templates FROM competition_editions WHERE edition_id=?")){q.setString(1,edition.id());try(var rows=q.executeQuery()){if(!rows.next())throw new SQLException("Missing prize templates");templates=JsonParser.parseString(rows.getString(1)).getAsJsonObject();}}
        for(var assignment:policy.assign(candidates)){
            String id="cp_"+hash(edition.id()+":"+assignment.player()).substring(0,40);RewardPlan plan;
            try{plan=RewardSnapshot.awardPlan(RewardSnapshot.decode(templates.get(assignment.band().reward()).getAsString()),id);}catch(RuntimeException invalid){throw new SQLException("Frozen competition award unavailable",invalid);}
            var expires=policy.expiryDays()==0?null:now.plus(Duration.ofDays(policy.expiryDays()));
            try(var q=c.prepareStatement("INSERT INTO competition_awards VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING")){
                q.setString(1,id);q.setString(2,edition.id());q.setString(3,assignment.player().toString());q.setInt(4,assignment.sourcePosition());q.setInt(5,assignment.band().maximumPosition());q.setString(6,assignment.band().reward());q.setString(7,RewardSnapshot.encode(plan));q.setLong(8,now.toEpochMilli());if(expires==null)q.setNull(9,Types.BIGINT);else q.setLong(9,expires.toEpochMilli());q.executeUpdate();
            }
        }
    }
    public static List<Award> list(Connection c,UUID player,int page)throws SQLException {
        if(page<0||page>100000)throw new IllegalArgumentException("Award page out of range");var result=new ArrayList<Award>();
        try(var q=c.prepareStatement("SELECT a.* FROM competition_awards a JOIN competition_editions d ON d.edition_id=a.edition_id WHERE a.player_uuid=? AND d.state='FINAL' ORDER BY a.assigned_at DESC,a.award_id LIMIT 10 OFFSET ?")){q.setString(1,player.toString());q.setInt(2,page*10);try(var rows=q.executeQuery()){while(rows.next())result.add(row(rows));}}return List.copyOf(result);
    }
    public static Optional<Award> find(Connection c,String id,UUID player)throws SQLException {
        try(var q=c.prepareStatement("SELECT a.* FROM competition_awards a JOIN competition_editions d ON d.edition_id=a.edition_id WHERE a.award_id=? AND a.player_uuid=? AND d.state='FINAL'")){q.setString(1,id);q.setString(2,player.toString());try(var rows=q.executeQuery()){return rows.next()?Optional.of(row(rows)):Optional.empty();}}
    }
    private static Award row(ResultSet rows)throws SQLException {
        try{long expires=rows.getLong("expires_at");boolean absent=rows.wasNull();return new Award(rows.getString("award_id"),rows.getString("edition_id"),UUID.fromString(rows.getString("player_uuid")),rows.getInt("source_position"),rows.getInt("band"),RewardSnapshot.decode(rows.getString("plan_snapshot")),Instant.ofEpochMilli(rows.getLong("assigned_at")),absent?null:Instant.ofEpochMilli(expires));}
        catch(RuntimeException invalid){throw new SQLException("Invalid persisted award",invalid);}
    }
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
}
