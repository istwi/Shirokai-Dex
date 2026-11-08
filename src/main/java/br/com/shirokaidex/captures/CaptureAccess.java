package br.com.shirokaidex.captures;

import br.com.shirokaidex.categories.*;
import br.com.shirokaidex.pokedex.ProgressCalculator;
import com.cobblemon.mod.common.api.events.pokeball.ThrownPokeballHitEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner;
import net.minecraft.server.level.ServerPlayer;
import java.time.Instant;
import java.util.*;

/** Snapshots native capture evidence only; does not create Pokemon, assign ownership or alter the official Pokedex. */
public final class CaptureAccess {
    private final CaptureTracker tracker=new CaptureTracker();
    public void hit(ThrownPokeballHitEvent event){
        var ball=event.getPokeBall();if(!(ball.getOwner() instanceof ServerPlayer player)||!player.server.isSameThread())return;
        var entity=event.getPokemon();var cause=entity.getSpawnCause();
        boolean natural=entity.getPokemon().isWild()&&cause!=null&&cause.getSpawner().getClass()==PlayerSpawner.class
                &&((PlayerSpawner)cause.getSpawner()).getSpawnPool()==CobblemonSpawnPools.INSTANCE.getWORLD_SPAWN_POOL();
        tracker.hit(ball.getUUID(),entity.getPokemon().getUuid(),player.getUUID(),Instant.now(),natural);
    }
    public CaptureRecord captured(PokemonCapturedEvent event,CategoryCatalog catalog){
        var player=event.getPlayer();if(!player.server.isSameThread())throw new IllegalStateException("Capture snapshot must run on server thread");
        var pokemon=event.getPokemon();var ball=event.getPokeBallEntity();var at=Instant.now();
        var origin=tracker.captured(ball.getUUID(),pokemon.getUuid(),player.getUUID(),at);
        if(!(ball.getOwner() instanceof ServerPlayer owner)||!owner.getUUID().equals(player.getUUID())||ball.getCapturingPokemon()==null||!ball.getCapturingPokemon().getPokemon().getUuid().equals(pokemon.getUuid()))origin=CaptureRecord.Origin.UNKNOWN;
        var form=pokemon.getForm();var species=pokemon.getSpecies().getResourceIdentifier().toString();Set<String> types=new HashSet<>();form.getTypes().forEach(t->types.add(t.getName()));
        var snapshot=new ProgressCalculator.Form(form.getName(),form.getLabels(),types,false,false,false);Set<String> categories=new HashSet<>();for(String id:catalog.definitions().keySet())if(catalog.matches(id,species,snapshot))categories.add(id);
        return new CaptureRecord(ball.getUUID(),pokemon.getUuid(),player.getUUID(),species,form.getName(),pokemon.getShiny(),snapshot.labels(),snapshot.types(),categories,CategoryRules.of(catalog).fingerprint(),at,ball.level().dimension().location().toString(),ball.getX(),ball.getY(),ball.getZ(),origin);
    }
    public void clear(){tracker.clear();}
}
