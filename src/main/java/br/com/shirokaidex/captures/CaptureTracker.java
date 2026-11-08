package br.com.shirokaidex.captures;

import java.time.*;
import java.util.*;

/** Bounded immutable pre-hit evidence, confined to the server thread. Missing evidence never implies legitimacy. */
public final class CaptureTracker {
    private record Attempt(UUID pokemon,UUID player,Instant at,boolean nativeWild,UUID entity,boolean wildMissingCause) { }
    private final Map<UUID,Attempt> attempts=new LinkedHashMap<>();
    public void hit(UUID ball,UUID pokemon,UUID player,Instant at,boolean nativeWild){
        hit(ball,pokemon,player,at,nativeWild,null,false);
    }
    public void hit(UUID ball,UUID pokemon,UUID player,Instant at,boolean nativeWild,UUID entity,boolean wildMissingCause){
        attempts.put(ball,new Attempt(pokemon,player,at,nativeWild,entity,wildMissingCause));while(attempts.size()>512)attempts.remove(attempts.keySet().iterator().next());
    }
    public record Evidence(CaptureRecord.Origin origin,UUID entity,CaptureObservation.Kind kind) { }
    public Evidence take(UUID ball,UUID pokemon,UUID player,Instant at){
        var evidence=attempts.remove(ball);
        boolean matches=evidence!=null&&evidence.pokemon().equals(pokemon)&&evidence.player().equals(player)&&!at.isBefore(evidence.at())&&!at.isAfter(evidence.at().plusSeconds(600));
        if(!matches)return new Evidence(CaptureRecord.Origin.UNKNOWN,null,CaptureObservation.Kind.NONE);
        return new Evidence(evidence.nativeWild()?CaptureRecord.Origin.NATIVE_PLAYER_SPAWN:CaptureRecord.Origin.UNKNOWN,evidence.entity(),evidence.nativeWild()?CaptureObservation.Kind.LIVE_NATIVE:evidence.wildMissingCause()?CaptureObservation.Kind.MISSING_CAUSE:CaptureObservation.Kind.NONE);
    }
    public CaptureRecord.Origin captured(UUID ball,UUID pokemon,UUID player,Instant at){return take(ball,pokemon,player,at).origin();}
    public void clear(){attempts.clear();}
}
