package br.com.shirokaidex.captures;

import java.util.*;

/** A correlated pre/post-capture observation. Missing cause is eligible for proof lookup only after matching wild evidence. */
public record CaptureObservation(CaptureRecord capture,UUID entity,Kind kind) {
    public enum Kind { NONE, LIVE_NATIVE, MISSING_CAUSE }
    public CaptureObservation {Objects.requireNonNull(capture);Objects.requireNonNull(kind);if(kind!=Kind.NONE)Objects.requireNonNull(entity);}
}
