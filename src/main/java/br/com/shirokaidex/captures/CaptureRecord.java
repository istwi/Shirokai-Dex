package br.com.shirokaidex.captures;

import java.time.*;
import java.util.*;

/** A server observation, not reconstructed Pokedex history. IDs bind one physical ball to one Pokemon. */
public record CaptureRecord(UUID ball,UUID pokemon,UUID player,String species,String form,boolean shiny,
        Set<String> labels,Set<String> types,Set<String> categories,String categoryRules,
        Instant capturedAt,String dimension,double x,double y,double z,Origin origin) {
    public enum Origin { NATIVE_PLAYER_SPAWN, UNKNOWN }
    public CaptureRecord {
        Objects.requireNonNull(ball);Objects.requireNonNull(pokemon);Objects.requireNonNull(player);Objects.requireNonNull(capturedAt);Objects.requireNonNull(origin);
        br.com.shirokaidex.pokedex.CollectionDefinition.identifier(species);br.com.shirokaidex.pokedex.CollectionDefinition.identifier(dimension);
        if(form==null||form.length()>128||!categoryRules.matches("[a-f0-9]{64}")||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Invalid capture snapshot");
        labels=Set.copyOf(labels);types=Set.copyOf(types);categories=Set.copyOf(categories);
        if(labels.size()>128||types.size()>32||categories.size()>64)throw new IllegalArgumentException("Capture classification too large");
        for(var set:List.of(labels,types,categories))if(set.stream().anyMatch(v->v==null||v.isBlank()||v.length()>128||v.codePoints().anyMatch(Character::isISOControl)))throw new IllegalArgumentException("Invalid capture classification");
    }
    public String eventKey(){return ball+":"+pokemon;}
    public boolean verified(){return origin==Origin.NATIVE_PLAYER_SPAWN;}
}
