package br.com.shirokaidex.competitions;

import br.com.shirokaidex.captures.CaptureRecord;
import br.com.shirokaidex.categories.*;
import br.com.shirokaidex.config.CategoriesCodec;
import br.com.shirokaidex.pokedex.ProgressCalculator;
import com.google.gson.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** All methods execute on the SQLite worker inside the caller's transaction. */
public final class CompetitionStore {
    private CompetitionStore() { }
    public enum State { ACTIVE, REVIEW, FINAL }
    public record Edition(String id,CompetitionDefinition definition,Instant start,Instant end,Instant reviewUntil,State state,String rulesJson,String rulesFingerprint) { }
    public record Standing(UUID player,String name,long score,Instant reachedAt,boolean cancelled,long position) { }
    public static void synchronize(Connection c,List<CompetitionDefinition> definitions,CategoryCatalog categories,Instant now)throws SQLException {
        synchronize(c,definitions,categories,List.of(),now);
    }
    public static void synchronize(Connection c,List<CompetitionDefinition> definitions,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards,Instant now)throws SQLException {
        for(var edition:editions(c,false)){
            if(edition.state()==State.ACTIVE&&!now.isBefore(edition.end())){
                try(var q=c.prepareStatement("UPDATE competition_editions SET state='REVIEW' WHERE edition_id=? AND state='ACTIVE'")){q.setString(1,edition.id());q.executeUpdate();}
                results(c,edition.id());audit(c,edition.id(),null,null,"SYSTEM","CLOSED","Calendar deadline",now);
            }
        }
        for(var edition:editions(c,false))if(edition.state()==State.REVIEW&&edition.definition().autoFinalize()&&!now.isBefore(edition.reviewUntil()))finalizeEdition(c,edition.id(),"SYSTEM","Automatic review deadline",now);
        for(var definition:definitions){
            boolean active=false;Instant lastEnd=null;
            try(var q=c.prepareStatement("SELECT starts_at,ends_at,state FROM competition_editions WHERE competition_id=? ORDER BY ends_at DESC LIMIT 1")){
                q.setString(1,definition.id());try(var rows=q.executeQuery()){while(rows.next()){
                    Instant end=Instant.ofEpochMilli(rows.getLong(2));if(lastEnd==null)lastEnd=end;
                    if(rows.getString(3).equals("ACTIVE")&&!now.isBefore(Instant.ofEpochMilli(rows.getLong(1)))&&now.isBefore(end))active=true;
                }}
            }
            if(active)continue;var window=definition.window(now);if(window.isEmpty())continue;var start=window.get().start();var end=window.get().end();
            if(lastEnd!=null&&lastEnd.isAfter(start))start=lastEnd;
            if(!start.isBefore(end)||now.isBefore(start))continue;
            var rules=CategoryRules.of(categories);
            if(definition.metric()==br.com.shirokaidex.rankings.CaptureRanking.Metric.CATEGORY&&!categories.definitions().containsKey(definition.key()))throw new SQLException("Competition category unavailable");
            String id=definition.id()+":"+start.toEpochMilli();
            try(var q=c.prepareStatement("INSERT INTO competition_editions(edition_id,competition_id,starts_at,ends_at,review_until,state,definition_json,rules_json,rules_fingerprint,finalized_at,reward_templates) VALUES(?,?,?,?,?,'ACTIVE',?,?,?,NULL,?) ON CONFLICT DO NOTHING")){
                q.setString(1,id);q.setString(2,definition.id());q.setLong(3,start.toEpochMilli());q.setLong(4,end.toEpochMilli());q.setLong(5,end.plus(Duration.ofHours(definition.reviewHours())).toEpochMilli());q.setString(6,CompetitionJson.write(definition));q.setString(7,rules.json());q.setString(8,rules.fingerprint());q.setString(9,CompetitionAwards.templates(definition.prizes(),rewards));
                if(q.executeUpdate()==1)audit(c,id,null,null,"SYSTEM","OPENED","Calendar edition",now);
            }
        }
    }
    public static List<Edition> editions(Connection c,boolean includeFinal)throws SQLException {
        var result=new ArrayList<Edition>();
        try(var q=c.prepareStatement("SELECT * FROM competition_editions "+(includeFinal?"":"WHERE state<>'FINAL' ")+"ORDER BY starts_at DESC,edition_id "+(includeFinal?"LIMIT 1000":""));var rows=q.executeQuery()){
            while(rows.next())result.add(row(rows));
        }
        return List.copyOf(result);
    }
    public record HistoryFilter(String competition,String category,Instant from,Instant until) {
        public HistoryFilter {competition=competition==null?"":competition;category=category==null?"":category;if(!competition.isEmpty()&&!competition.matches("[a-z][a-z0-9_]{0,47}")||!category.isEmpty()&&!category.matches("[a-z][a-z0-9_]{0,47}"))throw new IllegalArgumentException("Invalid history filter");if(from!=null&&until!=null&&!from.isBefore(until))throw new IllegalArgumentException("Invalid history interval");}
        public static HistoryFilter all(){return new HistoryFilter("","",null,null);}
    }
    public record HistoryPage(List<Edition> editions,boolean hasMore) {public HistoryPage{editions=List.copyOf(editions);}}
    public static HistoryPage history(Connection c,HistoryFilter filter,int page)throws SQLException {
        if(page<0||page>100000)throw new IllegalArgumentException("History page out of range");var values=new ArrayList<Edition>();
        try(var q=c.prepareStatement("SELECT * FROM competition_editions WHERE state<>'ACTIVE' AND (?='' OR competition_id=?) AND (?='' OR (json_extract(definition_json,'$.metric')='CATEGORY' AND json_extract(definition_json,'$.key')=?)) AND ends_at>? AND starts_at<? ORDER BY ends_at DESC,edition_id LIMIT 19 OFFSET ?")){
            q.setString(1,filter.competition());q.setString(2,filter.competition());q.setString(3,filter.category());q.setString(4,filter.category());q.setLong(5,filter.from()==null?Long.MIN_VALUE:filter.from().toEpochMilli());q.setLong(6,filter.until()==null?Long.MAX_VALUE:filter.until().toEpochMilli());q.setInt(7,page*18);try(var rows=q.executeQuery()){while(rows.next())values.add(row(rows));}
        }
        boolean more=values.size()>18;if(more)values.removeLast();return new HistoryPage(values,more);
    }
    public static List<String> adminRows(Connection c,String edition,String mode,String event,int page)throws SQLException {
        if(page<0||page>100000||!Set.of("points","audit","inspect").contains(mode))throw new IllegalArgumentException("Invalid audit query");var values=new ArrayList<String>();
        String sql=switch(mode){
            case "points"->"SELECT event_key,player_uuid,state,points,rule_fingerprint,recorded_at FROM competition_points WHERE edition_id=? ORDER BY recorded_at,event_key LIMIT 10 OFFSET ?";
            case "audit"->"SELECT sequence,player_uuid,event_key,actor,action,reason,created_at FROM competition_audit WHERE edition_id=? ORDER BY sequence DESC LIMIT 10 OFFSET ?";
            default->"SELECT e.event_key,e.player_uuid,e.species,e.form,e.shiny,e.origin,e.native_source,e.categories_json,e.category_rules,e.captured_at,e.dimension,e.x,e.y,e.z FROM capture_events e JOIN competition_points p ON p.event_key=e.event_key WHERE p.edition_id=? AND e.event_key=?";
        };
        try(var q=c.prepareStatement(sql)){q.setString(1,edition);if(mode.equals("inspect"))q.setString(2,event);else q.setInt(2,page*10);try(var rows=q.executeQuery()){int count=rows.getMetaData().getColumnCount();while(rows.next()){var fields=new ArrayList<String>();for(int i=1;i<=count;i++)fields.add(String.valueOf(rows.getObject(i)));values.add(String.join(" | ",fields));}}}
        return List.copyOf(values);
    }
    public static Optional<Edition> edition(Connection c,String id)throws SQLException {
        try(var q=c.prepareStatement("SELECT * FROM competition_editions WHERE edition_id=?")){q.setString(1,id);try(var rows=q.executeQuery()){return rows.next()?Optional.of(row(rows)):Optional.empty();}}
    }
    private static Edition row(ResultSet rows)throws SQLException {
        try{return new Edition(rows.getString("edition_id"),CompetitionJson.read(JsonParser.parseString(rows.getString("definition_json")).getAsJsonObject()),Instant.ofEpochMilli(rows.getLong("starts_at")),Instant.ofEpochMilli(rows.getLong("ends_at")),Instant.ofEpochMilli(rows.getLong("review_until")),State.valueOf(rows.getString("state")),rows.getString("rules_json"),rows.getString("rules_fingerprint"));}
        catch(RuntimeException invalid){throw new SQLException("Invalid persisted competition definition",invalid);}
    }
    public static boolean enroll(Connection c,String id,UUID player,Instant now)throws SQLException {
        var found=edition(c,id);if(found.isEmpty()||!active(found.get(),now))return false;
        try(var q=c.prepareStatement("INSERT INTO competition_entries(edition_id,player_uuid,joined_at) VALUES(?,?,?) ON CONFLICT DO NOTHING")){
            q.setString(1,id);q.setString(2,player.toString());q.setLong(3,now.toEpochMilli());boolean inserted=q.executeUpdate()==1;
            if(inserted)audit(c,id,player,null,player.toString(),"JOINED","Player enrollment",now);return inserted;
        }
    }
    public static boolean cancel(Connection c,String id,UUID player,boolean confirmed,Instant now)throws SQLException {
        var found=edition(c,id);if(!confirmed||found.isEmpty()||!active(found.get(),now))return false;
        try(var q=c.prepareStatement("UPDATE competition_entries SET cancelled_at=? WHERE edition_id=? AND player_uuid=? AND cancelled_at IS NULL")){
            q.setLong(1,now.toEpochMilli());q.setString(2,id);q.setString(3,player.toString());boolean changed=q.executeUpdate()==1;
            if(changed)audit(c,id,player,null,player.toString(),"CANCELLED","Confirmed permanent withdrawal from edition",now);return changed;
        }
    }
    private static boolean active(Edition edition,Instant now){return edition.state()==State.ACTIVE&&!now.isBefore(edition.start())&&now.isBefore(edition.end());}
    public static void capture(Connection c,CaptureRecord capture,List<CompetitionDefinition> definitions,CategoryCatalog categories)throws SQLException {
        if(!capture.verified())return;
        for(var edition:editions(c,false)){
            if(!active(edition,capture.capturedAt()))continue;
            try(var q=c.prepareStatement("SELECT joined_at,cancelled_at FROM competition_entries WHERE edition_id=? AND player_uuid=?")){
                q.setString(1,edition.id());q.setString(2,capture.player().toString());try(var rows=q.executeQuery()){if(!rows.next()||rows.getObject(2)!=null||capture.capturedAt().toEpochMilli()<=rows.getLong(1))continue;}
            }
            if(!matches(edition,capture))continue;
            var definition=edition.definition();boolean suspect=false;
            if(definition.hourlyReviewThreshold()>0)try(var q=c.prepareStatement("SELECT count(*) FROM competition_points WHERE edition_id=? AND player_uuid=? AND recorded_at>=?")){
                q.setString(1,edition.id());q.setString(2,capture.player().toString());q.setLong(3,capture.capturedAt().minus(Duration.ofHours(1)).toEpochMilli());try(var rows=q.executeQuery()){rows.next();suspect=rows.getLong(1)>=definition.hourlyReviewThreshold();}
            }
            boolean held=suspect&&definition.reviewMode()==CompetitionDefinition.ReviewMode.HOLD_FOR_REVIEW;
            String fingerprint=definition.categoryMode()==CompetitionDefinition.CategoryMode.SNAPSHOT?edition.rulesFingerprint():capture.categoryRules();
            try(var q=c.prepareStatement("INSERT INTO competition_points VALUES(?,?,?,1,?,?,?) ON CONFLICT DO NOTHING")){
                q.setString(1,edition.id());q.setString(2,capture.eventKey());q.setString(3,capture.player().toString());q.setString(4,held?"HELD":"COUNTED");q.setString(5,fingerprint);q.setLong(6,capture.capturedAt().toEpochMilli());
                if(q.executeUpdate()==1){if(!held)increment(c,edition.id(),capture.player(),capture.capturedAt());if(suspect)audit(c,edition.id(),capture.player(),capture.eventKey(),"SYSTEM",held?"HELD":"RATE_OBSERVED","Configured hourly threshold; rate alone is not proof of abuse",capture.capturedAt());}
            }
        }
    }
    public static boolean needsCaptureDetail(Connection c,UUID player,Instant at)throws SQLException {
        try(var q=c.prepareStatement("SELECT 1 FROM competition_entries p JOIN competition_editions d ON d.edition_id=p.edition_id WHERE p.player_uuid=? AND p.cancelled_at IS NULL AND p.joined_at<? AND d.state='ACTIVE' AND d.starts_at<=? AND d.ends_at>? LIMIT 1")){
            q.setString(1,player.toString());q.setLong(2,at.toEpochMilli());q.setLong(3,at.toEpochMilli());q.setLong(4,at.toEpochMilli());try(var rows=q.executeQuery()){return rows.next();}
        }
    }
    private static boolean matches(Edition edition,CaptureRecord capture)throws SQLException {
        var definition=edition.definition();return switch(definition.metric()){
            case COLLECTION->throw new SQLException("Pokedex metrics cannot score a capture competition");case TOTAL->true;case SHINY->capture.shiny();case LEGENDARY->capture.labels().contains("legendary");case MYTHICAL->capture.labels().contains("mythical");case SPECIES->capture.species().equals(definition.key());
            case CATEGORY->{
                if(definition.categoryMode()==CompetitionDefinition.CategoryMode.LIVE)yield capture.categories().contains(definition.key());
                try{var rules=new CategoryRules(1,edition.rulesJson(),edition.rulesFingerprint());var catalog=CategoriesCodec.parse(JsonParser.parseString(rules.json()).getAsJsonObject().getAsJsonArray("categories").toString());yield catalog.matches(definition.key(),capture.species(),new ProgressCalculator.Form(capture.form(),capture.labels(),capture.types(),false,false,false));}
                catch(Exception failure){throw new SQLException("Archived competition category invalid",failure);}
            }
        };
    }
    private static void increment(Connection c,String edition,UUID player,Instant at)throws SQLException {
        try(var q=c.prepareStatement("UPDATE competition_entries SET score=score+1,reached_at=CASE WHEN reached_at IS NULL THEN ? ELSE max(reached_at,?) END WHERE edition_id=? AND player_uuid=? AND cancelled_at IS NULL")){q.setLong(1,at.toEpochMilli());q.setLong(2,at.toEpochMilli());q.setString(3,edition);q.setString(4,player.toString());q.executeUpdate();}
    }
    public static List<Standing> standings(Connection c,String edition,int page,boolean official)throws SQLException {
        var definition=edition(c,edition);return standings(c,edition,page,official,br.com.shirokaidex.profiles.StatisticsPrivacy.defaultsPolicy(),definition.isPresent()?definition.get().definition().metric():br.com.shirokaidex.rankings.CaptureRanking.Metric.TOTAL);
    }
    public static List<Standing> standings(Connection c,String edition,int page,boolean official,br.com.shirokaidex.profiles.StatisticsPrivacy privacy,br.com.shirokaidex.rankings.CaptureRanking.Metric metric)throws SQLException {
        if(page<0||page>100000)throw new IllegalArgumentException("Competition page out of range");var result=new ArrayList<Standing>();
        String sql=official?"SELECT e.player_uuid,p.last_known_name,e.score,e.reached_at,e.position FROM competition_results e LEFT JOIN players p ON p.uuid=e.player_uuid LEFT JOIN player_ranking_privacy v ON v.player_uuid=e.player_uuid WHERE e.edition_id=? AND coalesce(v.hidden,0)=0 AND "+privacy.sql(metric,"e")+" ORDER BY e.position LIMIT 10 OFFSET ?":"SELECT e.player_uuid,p.last_known_name,e.score,e.reached_at,row_number() OVER(ORDER BY e.score DESC,e.reached_at IS NULL,e.reached_at,e.player_uuid) FROM competition_entries e LEFT JOIN players p ON p.uuid=e.player_uuid LEFT JOIN player_ranking_privacy v ON v.player_uuid=e.player_uuid WHERE e.edition_id=? AND e.cancelled_at IS NULL AND coalesce(v.hidden,0)=0 AND "+privacy.sql(metric,"e")+" ORDER BY e.score DESC,e.reached_at IS NULL,e.reached_at,e.player_uuid LIMIT 10 OFFSET ?";
        try(var q=c.prepareStatement(sql)){q.setString(1,edition);q.setInt(2,page*10);try(var rows=q.executeQuery()){while(rows.next()){String name=rows.getString(2);long at=rows.getLong(4);boolean absent=rows.wasNull();result.add(new Standing(UUID.fromString(rows.getString(1)),name==null?rows.getString(1):name,rows.getLong(3),absent?null:Instant.ofEpochMilli(at),false,rows.getLong(5)));}}}
        return List.copyOf(result);
    }
    public record Membership(boolean joined,boolean cancelled,long score) { }
    public static Membership membership(Connection c,String edition,UUID player)throws SQLException {
        try(var q=c.prepareStatement("SELECT cancelled_at,score FROM competition_entries WHERE edition_id=? AND player_uuid=?")){q.setString(1,edition);q.setString(2,player.toString());try(var rows=q.executeQuery()){return rows.next()?new Membership(true,rows.getObject(1)!=null,rows.getLong(2)):new Membership(false,false,0);}}
    }
    public static boolean finalizeEdition(Connection c,String id,String actor,String reason,Instant now)throws SQLException {
        validateReason(reason);
        var found=edition(c,id);if(found.isEmpty()||found.get().state()!=State.REVIEW||now.isBefore(found.get().reviewUntil()))return false;
        try(var q=c.prepareStatement("SELECT count(*) FROM competition_points WHERE edition_id=? AND state='HELD'")){q.setString(1,id);try(var rows=q.executeQuery()){rows.next();if(rows.getLong(1)>0)return false;}}
        results(c,id);
        CompetitionAwards.assign(c,found.get(),now);
        try(var q=c.prepareStatement("UPDATE competition_editions SET state='FINAL',finalized_at=? WHERE edition_id=? AND state='REVIEW'")){q.setLong(1,now.toEpochMilli());q.setString(2,id);if(q.executeUpdate()!=1)return false;}
        audit(c,id,null,null,actor,"FINALIZED",reason,now);return true;
    }
    private static void results(Connection c,String id)throws SQLException {
        try(var q=c.prepareStatement("DELETE FROM competition_results WHERE edition_id=?")){q.setString(1,id);q.executeUpdate();}
        try(var q=c.prepareStatement("INSERT INTO competition_results SELECT e.edition_id,e.player_uuid,row_number() OVER(ORDER BY e.score DESC,e.reached_at IS NULL,e.reached_at,e.player_uuid),e.score,e.reached_at,CASE WHEN coalesce(v.hidden,0)=0 THEN 1 ELSE 0 END FROM competition_entries e LEFT JOIN player_ranking_privacy v ON v.player_uuid=e.player_uuid WHERE e.edition_id=? AND e.cancelled_at IS NULL")){q.setString(1,id);q.executeUpdate();}
    }
    public static boolean reviewPoint(Connection c,String id,String event,String target,String actor,String reason,Instant now)throws SQLException {
        validateReason(reason);if(!Set.of("COUNTED","HELD","REJECTED").contains(target))throw new IllegalArgumentException("Invalid point state");var found=edition(c,id);if(found.isEmpty()||found.get().state()==State.FINAL)return false;
        UUID player;String previous;long points;
        try(var q=c.prepareStatement("SELECT player_uuid,state,points FROM competition_points WHERE edition_id=? AND event_key=?")){q.setString(1,id);q.setString(2,event);try(var rows=q.executeQuery()){if(!rows.next())return false;player=UUID.fromString(rows.getString(1));previous=rows.getString(2);points=rows.getLong(3);}}
        if(previous.equals(target))return false;
        try(var q=c.prepareStatement("UPDATE competition_points SET state=? WHERE edition_id=? AND event_key=?")){q.setString(1,target);q.setString(2,id);q.setString(3,event);q.executeUpdate();}
        long delta=(target.equals("COUNTED")?points:0)-(previous.equals("COUNTED")?points:0);
        try(var q=c.prepareStatement("UPDATE competition_entries SET score=score+?,reached_at=? WHERE edition_id=? AND player_uuid=?")){q.setLong(1,delta);q.setLong(2,now.toEpochMilli());q.setString(3,id);q.setString(4,player.toString());q.executeUpdate();}
        audit(c,id,player,event,actor,"POINT_"+previous+"_TO_"+target,reason,now);
        if(found.get().state()==State.REVIEW)results(c,id);return true;
    }
    public static Optional<String> adjust(Connection c,String edition,UUID player,long delta,String reverses,String actor,String reason,Instant now)throws SQLException {
        validateReason(reason);if(delta==0||delta < -1_000_000_000L||delta>1_000_000_000L)throw new IllegalArgumentException("Invalid competitive adjustment");
        var found=edition(c,edition);if(found.isEmpty()||found.get().state()==State.FINAL)return Optional.empty();
        try(var q=c.prepareStatement("SELECT score FROM competition_entries WHERE edition_id=? AND player_uuid=?")){q.setString(1,edition);q.setString(2,player.toString());try(var rows=q.executeQuery()){if(!rows.next()||rows.getLong(1)+delta<0)return Optional.empty();}}
        String id=UUID.randomUUID().toString();
        try(var q=c.prepareStatement("INSERT INTO competition_adjustments VALUES(?,?,?,?,?,?,?,?)")){
            q.setString(1,id);q.setString(2,edition);q.setString(3,player.toString());q.setLong(4,delta);q.setString(5,reverses);q.setString(6,actor);q.setString(7,reason);q.setLong(8,now.toEpochMilli());q.executeUpdate();
        }
        try(var q=c.prepareStatement("UPDATE competition_entries SET score=score+?,reached_at=? WHERE edition_id=? AND player_uuid=?")){q.setLong(1,delta);q.setLong(2,now.toEpochMilli());q.setString(3,edition);q.setString(4,player.toString());q.executeUpdate();}
        audit(c,edition,player,null,actor,reverses==null?"ADJUSTED":"ADJUSTMENT_REVERSED",id+" | "+reason,now);
        if(found.get().state()==State.REVIEW)results(c,edition);return Optional.of(id);
    }
    public static Optional<String> revertAdjustment(Connection c,String id,String actor,String reason,Instant now)throws SQLException {
        validateReason(reason);String edition;UUID player;long delta;
        try(var q=c.prepareStatement("SELECT edition_id,player_uuid,delta,reverses FROM competition_adjustments WHERE adjustment_id=? AND NOT EXISTS(SELECT 1 FROM competition_adjustments r WHERE r.reverses=?)")){
            q.setString(1,id);q.setString(2,id);try(var rows=q.executeQuery()){if(!rows.next()||rows.getString(4)!=null)return Optional.empty();edition=rows.getString(1);player=UUID.fromString(rows.getString(2));delta=rows.getLong(3);}
        }
        return adjust(c,edition,player,-delta,id,actor,reason,now);
    }
    private static void validateReason(String reason){if(reason==null||reason.strip().length()<8||reason.length()>2048||reason.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("A meaningful audit reason is required");}
    private static void audit(Connection c,String edition,UUID player,String event,String actor,String action,String reason,Instant at)throws SQLException {
        try(var q=c.prepareStatement("INSERT INTO competition_audit(edition_id,player_uuid,event_key,actor,action,reason,created_at) VALUES(?,?,?,?,?,?,?)")){q.setString(1,edition);q.setString(2,player==null?null:player.toString());q.setString(3,event);q.setString(4,actor);q.setString(5,action);q.setString(6,reason);q.setLong(7,at.toEpochMilli());q.executeUpdate();}
    }
}
