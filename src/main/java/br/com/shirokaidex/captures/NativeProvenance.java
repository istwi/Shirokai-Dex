package br.com.shirokaidex.captures;

import java.sql.*;

/** Called inside the single database transaction; never upgrades past events or resets consumed proof. */
public final class NativeProvenance {
    private NativeProvenance() { }
    public static boolean observe(Connection c,NativeSpawnProof proof)throws SQLException {
        try(var q=c.prepareStatement("INSERT INTO native_spawn_proofs VALUES(?,?,?,?,?,NULL) ON CONFLICT(pokemon_uuid,entity_uuid) DO NOTHING")){
            q.setString(1,proof.pokemon().toString());q.setString(2,proof.entity().toString());q.setString(3,proof.dimension());q.setLong(4,proof.observedAt().toEpochMilli());q.setString(5,proof.source().name());return q.executeUpdate()==1;
        }
    }
    public static boolean capture(Connection c,CaptureObservation observation)throws SQLException{
        var original=observation.capture();
        try(var q=c.prepareStatement("SELECT 1 FROM capture_events WHERE event_key=?")){q.setString(1,original.eventKey());try(var r=q.executeQuery()){if(r.next())return false;}}
        boolean verified=false;
        if(observation.entity()!=null){
            Long observedAt=null;
            try(var q=c.prepareStatement("SELECT observed_at FROM native_spawn_proofs WHERE pokemon_uuid=? AND entity_uuid=? AND consumed_event IS NULL")){
                q.setString(1,original.pokemon().toString());q.setString(2,observation.entity().toString());try(var r=q.executeQuery()){if(r.next())observedAt=r.getLong(1);}
            }
            if(observedAt!=null)try(var q=c.prepareStatement("UPDATE native_spawn_proofs SET consumed_event=? WHERE pokemon_uuid=? AND entity_uuid=? AND consumed_event IS NULL")){
                q.setString(1,original.eventKey());q.setString(2,original.pokemon().toString());q.setString(3,observation.entity().toString());
                // Every completed observation retires matching proof, even when correlation was insufficient to score it.
                boolean retired=q.executeUpdate()==1;verified=retired&&observation.kind()!=CaptureObservation.Kind.NONE&&observedAt<=original.capturedAt().toEpochMilli();
            }
        }
        var result=original.withOrigin(verified?CaptureRecord.Origin.NATIVE_PLAYER_SPAWN:CaptureRecord.Origin.UNKNOWN);
        if(!CaptureLedger.record(c,result))throw new SQLException("Capture observation changed during transaction");
        try(var q=c.prepareStatement("UPDATE capture_events SET entity_uuid=?,verification=? WHERE event_key=?")){
            q.setString(1,observation.entity()==null?null:observation.entity().toString());q.setString(2,!verified?"UNVERIFIED":observation.kind()==CaptureObservation.Kind.LIVE_NATIVE?"LIVE_NATIVE":"PERSISTED_NATIVE");q.setString(3,result.eventKey());q.executeUpdate();
        }
        return true;
    }
}
