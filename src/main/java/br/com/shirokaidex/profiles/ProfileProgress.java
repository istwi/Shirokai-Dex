package br.com.shirokaidex.profiles;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class ProfileProgress {
    private ProfileProgress() { }
    public record Proof(String collection,String version,Integer registered,int total,Instant observedAt) {
        public Proof {if(collection==null||!collection.matches("[a-z][a-z0-9_]{0,47}")||version==null||!version.matches("[a-f0-9]{64}")||total<0||registered!=null&&(registered<0||registered>total))throw new IllegalArgumentException("Invalid progress proof");Objects.requireNonNull(observedAt);}
    }
    public static void registerCollation(Connection c)throws SQLException {
        org.sqlite.Collation.create(c,"DEX_RATIO",new org.sqlite.Collation(){@Override protected int xCompare(String a,String b){return compareRatios(a,b);}});
    }
    public static int compareRatios(String a,String b){long[] x=ratio(a),y=ratio(b);return Long.compare(x[0]*y[1],y[0]*x[1]);}
    private static long[] ratio(String value){var parts=value.split("/",-1);if(parts.length!=2)throw new IllegalArgumentException("Invalid progress ratio");long n=Long.parseLong(parts[0]),d=Long.parseLong(parts[1]);if(n<0||d<1||n>d||d>Integer.MAX_VALUE)throw new IllegalArgumentException("Invalid progress ratio");return new long[]{n,d};}
    public static boolean observe(Connection c,UUID player,List<Proof> proofs)throws SQLException {
        boolean changed=false;
        for(var proof:proofs){
            try(var q=c.prepareStatement("SELECT definition_version,registered,total FROM profile_collection_points WHERE player_uuid=? AND collection_id=?")){q.setString(1,player.toString());q.setString(2,proof.collection());try(var rows=q.executeQuery()){changed|=!rows.next()||!Objects.equals(rows.getString(1),proof.version())||!Objects.equals(rows.getObject(2),proof.registered())||rows.getInt(3)!=proof.total();}}
            try(var q=c.prepareStatement("INSERT INTO profile_collection_points VALUES(?,?,?,?,?,?,NULL,?) ON CONFLICT(player_uuid,collection_id) DO UPDATE SET definition_version=excluded.definition_version,registered=excluded.registered,total=excluded.total,ratio=excluded.ratio,reached_at=CASE WHEN definition_version<>excluded.definition_version THEN NULL WHEN registered IS excluded.registered AND total=excluded.total THEN reached_at WHEN registered IS NULL THEN NULL ELSE excluded.observed_at END,observed_at=excluded.observed_at")){
                q.setString(1,player.toString());q.setString(2,proof.collection());q.setString(3,proof.version());if(proof.registered()==null)q.setNull(4,Types.INTEGER);else q.setInt(4,proof.registered());q.setInt(5,proof.total());q.setString(6,proof.registered()==null||proof.total()==0?null:proof.registered()+"/"+proof.total());q.setLong(7,proof.observedAt().toEpochMilli());q.executeUpdate();
            }
        }
        return changed;
    }
    public static List<Proof> read(Connection c,UUID player)throws SQLException {
        var result=new ArrayList<Proof>();try(var q=c.prepareStatement("SELECT collection_id,definition_version,registered,total,observed_at FROM profile_collection_points WHERE player_uuid=? ORDER BY collection_id")){q.setString(1,player.toString());try(var rows=q.executeQuery()){while(rows.next()){int value=rows.getInt(3);Integer registered=rows.wasNull()?null:value;result.add(new Proof(rows.getString(1),rows.getString(2),registered,rows.getInt(4),Instant.ofEpochMilli(rows.getLong(5))));}}}return List.copyOf(result);
    }
}
