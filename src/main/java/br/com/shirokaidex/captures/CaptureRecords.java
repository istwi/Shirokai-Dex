package br.com.shirokaidex.captures;

import java.sql.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Permanent period buckets, initialized once per requested player/timezone from retained minimal receipts. */
public final class CaptureRecords {
    private CaptureRecords() { }
    private record Bucket(String period,String date) { }
    public record Best(String period,String bucket,long total) { }
    public static List<Best> best(Connection c,UUID player,ZoneId zone)throws SQLException {
        boolean initialized;
        try(var q=c.prepareStatement("SELECT 1 FROM capture_record_zones WHERE player_uuid=? AND timezone=?")){q.setString(1,player.toString());q.setString(2,zone.getId());try(var rows=q.executeQuery()){initialized=rows.next();}}
        if(!initialized){
            var counts=new HashMap<Bucket,Long>();
            try(var q=c.prepareStatement("SELECT captured_at FROM capture_receipts WHERE player_uuid=? AND verified=1")){q.setString(1,player.toString());try(var rows=q.executeQuery()){while(rows.next())for(var bucket:buckets(Instant.ofEpochMilli(rows.getLong(1)).atZone(zone).toLocalDate()))counts.merge(bucket,1L,Long::sum);}}
            persist(c,player,zone,counts);
            try(var q=c.prepareStatement("INSERT INTO capture_record_zones VALUES(?,?)")){q.setString(1,player.toString());q.setString(2,zone.getId());q.executeUpdate();}
        }
        var result=new ArrayList<Best>();
        for(String period:List.of("DAY","WEEK","MONTH"))try(var q=c.prepareStatement("SELECT bucket,total FROM capture_record_buckets WHERE player_uuid=? AND timezone=? AND period=? ORDER BY total DESC,bucket ASC LIMIT 1")){
            q.setString(1,player.toString());q.setString(2,zone.getId());q.setString(3,period);try(var rows=q.executeQuery()){if(rows.next())result.add(new Best(period,rows.getString(1),rows.getLong(2)));else result.add(new Best(period,"",0));}
        }
        return List.copyOf(result);
    }
    public static void record(Connection c,CaptureRecord capture)throws SQLException {
        if(!capture.verified())return;
        var zones=new ArrayList<ZoneId>();
        try(var q=c.prepareStatement("SELECT timezone FROM capture_record_zones WHERE player_uuid=?")){q.setString(1,capture.player().toString());try(var rows=q.executeQuery()){while(rows.next())zones.add(ZoneId.of(rows.getString(1)));}}
        for(var zone:zones)increment(c,capture.player(),zone,capture.capturedAt());
    }
    private static List<Bucket> buckets(LocalDate date) {return List.of(new Bucket("DAY",date.toString()),new Bucket("WEEK",date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()),new Bucket("MONTH",date.withDayOfMonth(1).toString()));}
    private static void increment(Connection c,UUID player,ZoneId zone,Instant at)throws SQLException {
        var counts=new HashMap<Bucket,Long>();for(var bucket:buckets(at.atZone(zone).toLocalDate()))counts.put(bucket,1L);persist(c,player,zone,counts);
    }
    private static void persist(Connection c,UUID player,ZoneId zone,Map<Bucket,Long> counts)throws SQLException {
        try(var q=c.prepareStatement("INSERT INTO capture_record_buckets VALUES(?,?,?,?,?) ON CONFLICT(player_uuid,timezone,period,bucket) DO UPDATE SET total=total+excluded.total")){
            for(var entry:counts.entrySet()){q.setString(1,player.toString());q.setString(2,zone.getId());q.setString(3,entry.getKey().period());q.setString(4,entry.getKey().date());q.setLong(5,entry.getValue());q.addBatch();}q.executeBatch();
        }
    }
}
