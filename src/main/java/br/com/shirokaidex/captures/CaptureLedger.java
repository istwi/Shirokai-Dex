package br.com.shirokaidex.captures;

import java.sql.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import com.google.gson.Gson;

/** Runs only in the database transaction/worker. A duplicate observation cannot increment aggregates. */
public final class CaptureLedger {
    private CaptureLedger() { }
    public record Summary(long total,long shiny,long legendary,long mythical,long distinctSpecies,long today,long week,long month,long unverified,ZoneId timezone) { }
    public static boolean record(Connection c,CaptureRecord r)throws SQLException{
        return record(c,r,true);
    }
    public static boolean record(Connection c,CaptureRecord r,boolean historyEnabled)throws SQLException{
        // This durable receipt is retained even when detailed history is pruned.
        // The caller owns the transaction: receipt, details and counters commit together.
        try(var q=c.prepareStatement("INSERT INTO capture_receipts(event_key,player_uuid,captured_at,verified) VALUES(?,?,?,?) ON CONFLICT(event_key) DO NOTHING")){
            q.setString(1,r.eventKey());q.setString(2,r.player().toString());q.setLong(3,r.capturedAt().toEpochMilli());q.setBoolean(4,r.verified());
            if(q.executeUpdate()==0)return false;
        }
        var json=new Gson();
        if(historyEnabled||!r.verified())try(var q=c.prepareStatement("INSERT INTO capture_events(event_key,ball_uuid,pokemon_uuid,player_uuid,species,form,shiny,labels_json,types_json,categories_json,category_rules,captured_at,dimension,x,y,z,origin) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(event_key) DO NOTHING")){
            int i=1;q.setString(i++,r.eventKey());q.setString(i++,r.ball().toString());q.setString(i++,r.pokemon().toString());q.setString(i++,r.player().toString());q.setString(i++,r.species());q.setString(i++,r.form());q.setBoolean(i++,r.shiny());q.setString(i++,json.toJson(new TreeSet<>(r.labels())));q.setString(i++,json.toJson(new TreeSet<>(r.types())));q.setString(i++,json.toJson(new TreeSet<>(r.categories())));q.setString(i++,r.categoryRules());q.setLong(i++,r.capturedAt().toEpochMilli());q.setString(i++,r.dimension());q.setDouble(i++,r.x());q.setDouble(i++,r.y());q.setDouble(i++,r.z());q.setString(i,r.origin().name());
            if(q.executeUpdate()!=1)throw new SQLException("Capture detail conflicts with new receipt");
        }
        if(!r.verified())return true;
        br.com.shirokaidex.rankings.CaptureRankings.record(c,r);
        CaptureRecords.record(c,r);
        CapturedForms.record(c,r);
        try(var q=c.prepareStatement("INSERT INTO capture_totals VALUES(?,1,?,?,?) ON CONFLICT(player_uuid) DO UPDATE SET total=total+1,shiny=shiny+excluded.shiny,legendary=legendary+excluded.legendary,mythical=mythical+excluded.mythical")){
            q.setString(1,r.player().toString());q.setInt(2,r.shiny()?1:0);q.setInt(3,r.labels().contains("legendary")?1:0);q.setInt(4,r.labels().contains("mythical")?1:0);q.executeUpdate();
        }
        if(r.shiny())try(var q=c.prepareStatement("INSERT INTO captured_shiny_forms VALUES(?,?,?,?) ON CONFLICT(player_uuid,species,form) DO NOTHING")){q.setString(1,r.player().toString());q.setString(2,r.species());q.setString(3,r.form().toLowerCase(Locale.ROOT));q.setLong(4,r.capturedAt().toEpochMilli());q.executeUpdate();}
        increment(c,r.player(),"SPECIES",r.species());for(String category:r.categories())increment(c,r.player(),"CATEGORY",category);return true;
    }
    private static void increment(Connection c,UUID player,String kind,String key)throws SQLException{
        try(var q=c.prepareStatement("INSERT INTO capture_counts VALUES(?,?,?,1) ON CONFLICT(player_uuid,kind,entry_key) DO UPDATE SET total=total+1")){q.setString(1,player.toString());q.setString(2,kind);q.setString(3,key);q.executeUpdate();}
    }
    public static Set<br.com.shirokaidex.pokedex.CapturedShinyForm> shinyForms(Connection c,UUID player)throws SQLException{
        var forms=new HashSet<br.com.shirokaidex.pokedex.CapturedShinyForm>();try(var q=c.prepareStatement("SELECT species,form FROM captured_shiny_forms WHERE player_uuid=?")){q.setString(1,player.toString());try(var rows=q.executeQuery()){while(rows.next())forms.add(new br.com.shirokaidex.pokedex.CapturedShinyForm(rows.getString(1),rows.getString(2)));}}return Set.copyOf(forms);
    }
    public static long count(Connection c,UUID player,String kind,String key)throws SQLException{
        if(!Set.of("SPECIES","CATEGORY").contains(kind))throw new IllegalArgumentException("Unknown capture counter");
        try(var q=c.prepareStatement("SELECT total FROM capture_counts WHERE player_uuid=? AND kind=? AND entry_key=?")){q.setString(1,player.toString());q.setString(2,kind);q.setString(3,key);try(var r=q.executeQuery()){return r.next()?r.getLong(1):0;}}
    }
    public static Summary summary(Connection c,UUID player,Instant now,ZoneId zone)throws SQLException{
        long total=0,shiny=0,legendary=0,mythical=0,distinct=0,unknown=0;long[] periods=new long[3];
        try(var q=c.prepareStatement("SELECT total,shiny,legendary,mythical FROM capture_totals WHERE player_uuid=?")){q.setString(1,player.toString());try(var r=q.executeQuery()){if(r.next()){total=r.getLong(1);shiny=r.getLong(2);legendary=r.getLong(3);mythical=r.getLong(4);}}}
        try(var q=c.prepareStatement("SELECT count(*) FROM capture_counts WHERE player_uuid=? AND kind='SPECIES'")){q.setString(1,player.toString());try(var r=q.executeQuery()){r.next();distinct=r.getLong(1);}}
        try(var q=c.prepareStatement("SELECT count(*) FROM capture_receipts WHERE player_uuid=? AND verified=0")){q.setString(1,player.toString());try(var r=q.executeQuery()){r.next();unknown=r.getLong(1);}}
        var date=now.atZone(zone).toLocalDate();var days=List.of(date,date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),date.withDayOfMonth(1));
        for(int i=0;i<3;i++)try(var q=c.prepareStatement("SELECT count(*) FROM capture_receipts WHERE player_uuid=? AND verified=1 AND captured_at>=? AND captured_at<=?")){
            q.setString(1,player.toString());q.setLong(2,days.get(i).atStartOfDay(zone).toInstant().toEpochMilli());q.setLong(3,now.toEpochMilli());try(var r=q.executeQuery()){r.next();periods[i]=r.getLong(1);}
        }
        return new Summary(total,shiny,legendary,mythical,distinct,periods[0],periods[1],periods[2],unknown,zone);
    }
}
