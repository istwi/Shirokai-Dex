package br.com.shirokaidex.notifications;

import br.com.shirokaidex.profiles.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Durable event identity; claim-before-send provides at-most-once delivery, including across restart. */
public final class NotificationOutbox {
    public static final String GLOBAL="GLOBAL";
    public record Notice(long sequence,NotificationPolicy.Event event,String value,boolean broadcastEligible) { }
    private NotificationOutbox() { }
    public static void enqueue(Connection c,String key,String target,NotificationPolicy.Event event,String value,Instant now)throws SQLException {
        if(key.length()>512||target.length()>36||value.length()>512)throw new IllegalArgumentException("Oversized notification");
        try(var q=c.prepareStatement("INSERT INTO notification_outbox(event_key,target,event,value,created_at) VALUES(?,?,?,?,?) ON CONFLICT(event_key) DO NOTHING")){q.setString(1,key);q.setString(2,target);q.setString(3,event.name());q.setString(4,value);q.setLong(5,now.toEpochMilli());q.executeUpdate();}
    }
    public static List<Notice> claim(Connection c,String target,Instant now)throws SQLException {
        boolean enabled=target.equals(GLOBAL)||ProfileStore.preferences(c,UUID.fromString(target)).notifications();
        var notices=new ArrayList<Notice>();try(var q=c.prepareStatement("SELECT sequence,event,value,event_key FROM notification_outbox WHERE target=? AND consumed_at IS NULL ORDER BY sequence LIMIT 8")){q.setString(1,target);try(var rows=q.executeQuery()){while(rows.next())notices.add(new Notice(rows.getLong(1),NotificationPolicy.Event.valueOf(rows.getString(2)),rows.getString(3),rows.getString(4).startsWith("public:")));}}
        try(var q=c.prepareStatement("UPDATE notification_outbox SET consumed_at=? WHERE sequence=? AND consumed_at IS NULL")){for(var notice:notices){q.setLong(1,now.toEpochMilli());q.setLong(2,notice.sequence());q.addBatch();}q.executeBatch();}
        return enabled?List.copyOf(notices):List.of();
    }
    public static void progress(Connection c,UUID player,List<ProfileProgress.Proof> proofs,Instant now)throws SQLException {
        for(var proof:proofs){if(proof.registered()==null||proof.total()<=0)continue;int tier=(int)((long)proof.registered()*4/proof.total());Integer before=null;String version=null;
            try(var q=c.prepareStatement("SELECT version,tier FROM notification_progress WHERE player_uuid=? AND collection_id=?")){q.setString(1,player.toString());q.setString(2,proof.collection());try(var rows=q.executeQuery()){if(rows.next()){version=rows.getString(1);before=rows.getInt(2);}}}
            if(proof.version().equals(version)&&before!=null&&tier>before)for(int step=before+1;step<=tier;step++)enqueue(c,"progress:"+player+":"+proof.collection()+":"+version+":"+step,player.toString(),NotificationPolicy.Event.PROGRESS,proof.collection()+": "+(step*25)+"%",now);
            try(var q=c.prepareStatement("INSERT INTO notification_progress VALUES(?,?,?,?) ON CONFLICT(player_uuid,collection_id) DO UPDATE SET version=excluded.version,tier=excluded.tier")){q.setString(1,player.toString());q.setString(2,proof.collection());q.setString(3,proof.version());q.setInt(4,tier);q.executeUpdate();}
        }
    }
    public static void competition(Connection c,String edition,String name,NotificationPolicy.Event event,boolean publicEdition,Instant now)throws SQLException {
        try(var q=c.prepareStatement("INSERT INTO notification_outbox(event_key,target,event,value,created_at) SELECT ?||player_uuid,player_uuid,?,?,? FROM competition_entries WHERE edition_id=? AND cancelled_at IS NULL ON CONFLICT(event_key) DO NOTHING")){q.setString(1,(publicEdition?"public:":"private:")+event.name()+":"+edition+":");q.setString(2,event.name());q.setString(3,name);q.setLong(4,now.toEpochMilli());q.setString(5,edition);q.executeUpdate();}
        if(publicEdition)enqueue(c,event.name()+":"+edition+":GLOBAL",GLOBAL,event,name,now);
    }
}
