package br.com.shirokaidex.defeats;
import java.sql.*;
import java.util.*;
public final class DefeatLedger {
    private DefeatLedger() { }
    public record Entry(UUID player,String name,long total) { }
    public static boolean record(Connection c,DefeatRecord r)throws SQLException {
        try(var q=c.prepareStatement("INSERT INTO defeat_receipts VALUES(?,?,?,?,?) ON CONFLICT(battle_uuid,pokemon_uuid) DO NOTHING")){q.setString(1,r.battle().toString());q.setString(2,r.pokemon().toString());q.setString(3,r.player().toString());q.setString(4,r.species());q.setLong(5,r.at().toEpochMilli());if(q.executeUpdate()!=1)return false;}
        try(var q=c.prepareStatement("INSERT INTO defeat_totals VALUES(?,1,?) ON CONFLICT(player_uuid) DO UPDATE SET total=total+1,reached_at=excluded.reached_at")){q.setString(1,r.player().toString());q.setLong(2,r.at().toEpochMilli());q.executeUpdate();}return true;
    }
    public static List<Entry> top(Connection c)throws SQLException {
        var result=new ArrayList<Entry>();try(var q=c.prepareStatement("SELECT s.player_uuid,coalesce(p.last_known_name,s.player_uuid),s.total FROM defeat_totals s LEFT JOIN players p ON p.uuid=s.player_uuid WHERE NOT EXISTS(SELECT 1 FROM player_ranking_privacy v WHERE v.player_uuid=s.player_uuid AND v.hidden=1) ORDER BY s.total DESC,s.reached_at,s.player_uuid LIMIT 10");var rows=q.executeQuery()){while(rows.next())result.add(new Entry(UUID.fromString(rows.getString(1)),rows.getString(2),rows.getLong(3)));}return List.copyOf(result);
    }
}
