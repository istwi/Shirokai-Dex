package br.com.shirokaidex.rankings;

import br.com.shirokaidex.captures.CaptureRecord;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Worker-confined persistent scores; no rewards or competition enrollment. */
public final class CaptureRankings {
    private CaptureRankings() { }
    public record Entry(UUID player,String name,long score,Instant reachedAt,Integer registered,Integer total,Instant observedAt) {
        public Entry(UUID player,String name,long score,Instant reachedAt){this(player,name,score,reachedAt,null,null,null);}
    }
    public record Page(List<Entry> entries, boolean hasMore) { public Page { entries=List.copyOf(entries); } }
    public static void record(Connection c, CaptureRecord capture) throws SQLException {
        if(!capture.verified())return;
        increment(c,capture,"TOTAL","");
        if(capture.shiny())increment(c,capture,"SHINY","");
        if(capture.labels().contains("legendary"))increment(c,capture,"LEGENDARY","");
        if(capture.labels().contains("mythical"))increment(c,capture,"MYTHICAL","");
        increment(c,capture,"SPECIES",capture.species());
        for(String category:capture.categories())increment(c,capture,"CATEGORY",category);
    }
    private static void increment(Connection c,CaptureRecord capture,String metric,String key)throws SQLException {
        try(var q=c.prepareStatement("INSERT INTO capture_ranking_scores VALUES(?,?,?,1,?) ON CONFLICT(player_uuid,metric,entry_key) DO UPDATE SET score=score+1,reached_at=CASE WHEN reached_at IS NULL THEN excluded.reached_at ELSE max(reached_at,excluded.reached_at) END")) {
            q.setString(1,capture.player().toString());q.setString(2,metric);q.setString(3,key);q.setLong(4,capture.capturedAt().toEpochMilli());q.executeUpdate();
        }
    }
    public static void backfill(Connection c)throws SQLException {
        // Existing totals are authoritative; never reconstruct missing actions or a timestamp from a subset.
        for(String metric:List.of("TOTAL","SHINY","LEGENDARY","MYTHICAL")) {
            String column=metric.toLowerCase(Locale.ROOT);
            try(var q=c.createStatement()) { q.executeUpdate("INSERT INTO capture_ranking_scores SELECT player_uuid,'"+metric+"','',"+column+",NULL FROM capture_totals WHERE "+column+">0"); }
        }
        try(var q=c.createStatement()){q.executeUpdate("INSERT INTO capture_ranking_scores SELECT player_uuid,kind,entry_key,total,NULL FROM capture_counts WHERE total>0");}
        // Receipt timestamps are complete exactly when their verified count matches the permanent total.
        try(var q=c.createStatement()){q.executeUpdate("UPDATE capture_ranking_scores SET reached_at=(SELECT max(captured_at) FROM capture_receipts r WHERE r.player_uuid=capture_ranking_scores.player_uuid AND verified=1) WHERE metric='TOTAL' AND score=(SELECT count(*) FROM capture_receipts r WHERE r.player_uuid=capture_ranking_scores.player_uuid AND verified=1)");}
        backfillDetails(c);
    }
    private record HistoricalKey(String player,String metric,String key) { }
    private static void backfillDetails(Connection c)throws SQLException {
        var observed=new HashMap<HistoricalKey,long[]>();
        try(var q=c.createStatement();var rows=q.executeQuery("SELECT player_uuid,species,shiny,labels_json,categories_json,captured_at FROM capture_events WHERE origin='NATIVE_PLAYER_SPAWN'")){
            while(rows.next()){
                String player=rows.getString(1);long at=rows.getLong(6);
                observe(observed,new HistoricalKey(player,"SPECIES",rows.getString(2)),at);
                if(rows.getBoolean(3))observe(observed,new HistoricalKey(player,"SHINY",""),at);
                try {
                    for(var label:com.google.gson.JsonParser.parseString(rows.getString(4)).getAsJsonArray()){
                        String name=label.getAsString();if(name.equals("legendary")||name.equals("mythical"))observe(observed,new HistoricalKey(player,name.toUpperCase(Locale.ROOT),""),at);
                    }
                    var categories=new HashSet<String>();for(var category:com.google.gson.JsonParser.parseString(rows.getString(5)).getAsJsonArray())categories.add(category.getAsString());
                    for(String category:categories)observe(observed,new HistoricalKey(player,"CATEGORY",category),at);
                } catch(RuntimeException invalid){throw new SQLException("Invalid historical capture classification",invalid);}
            }
        }
        try(var q=c.prepareStatement("UPDATE capture_ranking_scores SET reached_at=? WHERE player_uuid=? AND metric=? AND entry_key=? AND score=?")){
            for(var entry:observed.entrySet()){var key=entry.getKey();var value=entry.getValue();q.setLong(1,value[1]);q.setString(2,key.player());q.setString(3,key.metric());q.setString(4,key.key());q.setLong(5,value[0]);q.addBatch();}q.executeBatch();
        }
    }
    private static void observe(Map<HistoricalKey,long[]> observed,HistoricalKey key,long at){var value=observed.computeIfAbsent(key,unused->new long[]{0,Long.MIN_VALUE});value[0]++;value[1]=Math.max(value[1],at);}
    public static Page page(Connection c,CaptureRanking definition,int page)throws SQLException {
        return page(c,definition,page,br.com.shirokaidex.profiles.StatisticsPrivacy.defaultsPolicy(),null);
    }
    public static Page page(Connection c,CaptureRanking definition,int page,br.com.shirokaidex.profiles.StatisticsPrivacy privacy,String version)throws SQLException {
        return page(c,definition,page,privacy,version,null);
    }
    public static Page page(Connection c,CaptureRanking definition,int page,br.com.shirokaidex.profiles.StatisticsPrivacy privacy,String version,br.com.shirokaidex.profiles.StatisticsPrivacy.Key sensitivity)throws SQLException {
        if(page<0||page>100000)throw new IllegalArgumentException("Ranking page out of range");
        if(definition.metric()==CaptureRanking.Metric.COLLECTION)return progressPage(c,definition,page,version,privacy,sensitivity);
        var entries=new ArrayList<Entry>();
        try(var q=c.prepareStatement("SELECT s.player_uuid,p.last_known_name,s.score,s.reached_at FROM capture_ranking_scores s LEFT JOIN players p ON p.uuid=s.player_uuid LEFT JOIN player_ranking_privacy v ON v.player_uuid=s.player_uuid WHERE s.metric=? AND s.entry_key=? AND s.score>0 AND coalesce(v.hidden,0)=0 AND "+privacy.sql(definition.metric(),"s")+" ORDER BY s.score DESC,s.reached_at IS NULL,s.reached_at ASC,s.player_uuid ASC LIMIT 11 OFFSET ?")) {
            q.setString(1,definition.metric().name());q.setString(2,definition.key());q.setInt(3,page*10);
            try(var rows=q.executeQuery()){while(rows.next()){long at=rows.getLong(4);Instant reached=rows.wasNull()?null:Instant.ofEpochMilli(at);String name=rows.getString(2);entries.add(new Entry(UUID.fromString(rows.getString(1)),name==null?rows.getString(1):name,rows.getLong(3),reached));}}
        }
        boolean more=entries.size()>10;if(more)entries.removeLast();return new Page(entries,more);
    }
    private static Page progressPage(Connection c,CaptureRanking definition,int page,String version,br.com.shirokaidex.profiles.StatisticsPrivacy privacy,br.com.shirokaidex.profiles.StatisticsPrivacy.Key sensitivity)throws SQLException {
        if(version==null||!version.matches("[a-f0-9]{64}"))return new Page(List.of(),false);var entries=new ArrayList<Entry>();
        try(var q=c.prepareStatement("SELECT s.player_uuid,p.last_known_name,s.registered,s.total,s.reached_at,s.observed_at FROM profile_collection_points s LEFT JOIN players p ON p.uuid=s.player_uuid LEFT JOIN player_ranking_privacy v ON v.player_uuid=s.player_uuid WHERE s.collection_id=? AND s.definition_version=? AND s.ratio IS NOT NULL AND coalesce(v.hidden,0)=0 AND "+privacy.sql(sensitivity,"s")+" ORDER BY s.ratio COLLATE DEX_RATIO DESC,s.reached_at IS NULL,s.reached_at,s.player_uuid LIMIT 11 OFFSET ?")){
            q.setString(1,definition.key());q.setString(2,version);q.setInt(3,page*10);try(var rows=q.executeQuery()){while(rows.next()){String name=rows.getString(2);long at=rows.getLong(5);Instant reached=rows.wasNull()?null:Instant.ofEpochMilli(at);entries.add(new Entry(UUID.fromString(rows.getString(1)),name==null?rows.getString(1):name,rows.getInt(3),reached,rows.getInt(3),rows.getInt(4),Instant.ofEpochMilli(rows.getLong(6))));}}
        }boolean more=entries.size()>10;if(more)entries.removeLast();return new Page(entries,more);
    }
    public static Long position(Connection c,CaptureRanking definition,UUID player,br.com.shirokaidex.profiles.StatisticsPrivacy privacy,String version)throws SQLException {
        return position(c,definition,player,privacy,version,null);
    }
    public static Long position(Connection c,CaptureRanking definition,UUID player,br.com.shirokaidex.profiles.StatisticsPrivacy privacy,String version,br.com.shirokaidex.profiles.StatisticsPrivacy.Key sensitivity)throws SQLException {
        boolean collection=definition.metric()==CaptureRanking.Metric.COLLECTION;if(collection&&(version==null||!version.matches("[a-f0-9]{64}")))return null;
        String sql=collection?"SELECT position FROM(SELECT s.player_uuid,row_number() OVER(ORDER BY s.ratio COLLATE DEX_RATIO DESC,s.reached_at IS NULL,s.reached_at,s.player_uuid) position FROM profile_collection_points s LEFT JOIN player_ranking_privacy v ON v.player_uuid=s.player_uuid WHERE s.collection_id=? AND s.definition_version=? AND s.ratio IS NOT NULL AND coalesce(v.hidden,0)=0 AND "+privacy.sql(sensitivity,"s")+") WHERE player_uuid=?":"SELECT position FROM(SELECT s.player_uuid,row_number() OVER(ORDER BY s.score DESC,s.reached_at IS NULL,s.reached_at,s.player_uuid) position FROM capture_ranking_scores s LEFT JOIN player_ranking_privacy v ON v.player_uuid=s.player_uuid WHERE s.metric=? AND s.entry_key=? AND s.score>0 AND coalesce(v.hidden,0)=0 AND "+privacy.sql(definition.metric(),"s")+") WHERE player_uuid=?";
        try(var q=c.prepareStatement(sql)){q.setString(1,collection?definition.key():definition.metric().name());q.setString(2,collection?version:definition.key());q.setString(3,player.toString());try(var rows=q.executeQuery()){return rows.next()?rows.getLong(1):null;}}
    }
    public static void privacy(Connection c,UUID player,boolean hidden)throws SQLException {
        try(var q=c.prepareStatement("INSERT INTO player_ranking_privacy VALUES(?,?) ON CONFLICT(player_uuid) DO UPDATE SET hidden=excluded.hidden")){q.setString(1,player.toString());q.setBoolean(2,hidden);q.executeUpdate();}
    }
}
