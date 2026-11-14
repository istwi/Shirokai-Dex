package br.com.shirokaidex.defeats;
import java.time.Instant;
import java.util.UUID;
public record DefeatRecord(UUID battle,UUID pokemon,UUID player,String species,Instant at){
    public DefeatRecord {java.util.Objects.requireNonNull(battle);java.util.Objects.requireNonNull(pokemon);java.util.Objects.requireNonNull(player);java.util.Objects.requireNonNull(at);br.com.shirokaidex.pokedex.CollectionDefinition.identifier(species);}
}
