package br.com.shirokaidex.captures;

import java.time.Instant;
import java.util.*;

/** First observed native wild provenance, bound to both logical Pokemon and physical entity identities. */
public record NativeSpawnProof(UUID pokemon,UUID entity,String dimension,Instant observedAt,Source source,Kind kind) {
    public enum Source { ENTITY_LOAD, PREHIT }
    public enum Kind { WORLD_PLAYER, FISHING }
    public NativeSpawnProof(UUID pokemon,UUID entity,String dimension,Instant observedAt,Source source){this(pokemon,entity,dimension,observedAt,source,Kind.WORLD_PLAYER);}
    public NativeSpawnProof {Objects.requireNonNull(pokemon);Objects.requireNonNull(entity);Objects.requireNonNull(observedAt);Objects.requireNonNull(source);Objects.requireNonNull(kind);br.com.shirokaidex.pokedex.CollectionDefinition.identifier(dimension);}
}
