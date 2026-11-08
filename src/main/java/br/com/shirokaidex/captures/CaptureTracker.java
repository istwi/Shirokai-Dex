package br.com.shirokaidex.captures;

import java.time.*;
import java.util.*;

/** Bounded immutable pre-hit evidence, confined to the server thread. Missing evidence never implies legitimacy. */
public final class CaptureTracker {
    private record Attempt(UUID pokemon,UUID player,Instant at,boolean nativeWild) { }
    private final Map<UUID,Attempt> attempts=new LinkedHashMap<>();
    public void hit(UUID ball,UUID pokemon,UUID player,Instant at,boolean nativeWild){
        attempts.put(ball,new Attempt(pokemon,player,at,nativeWild));while(attempts.size()>512)attempts.remove(attempts.keySet().iterator().next());
    }
    public CaptureRecord.Origin captured(UUID ball,UUID pokemon,UUID player,Instant at){
        var evidence=attempts.remove(ball);
        return evidence!=null&&evidence.nativeWild()&&evidence.pokemon().equals(pokemon)&&evidence.player().equals(player)
                &&!at.isBefore(evidence.at())&&!at.isAfter(evidence.at().plusSeconds(600))?CaptureRecord.Origin.NATIVE_PLAYER_SPAWN:CaptureRecord.Origin.UNKNOWN;
    }
    public void clear(){attempts.clear();}
}
