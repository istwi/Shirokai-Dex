package br.com.shirokaidex.defeats;
import br.com.shirokaidex.captures.*;
import com.cobblemon.mod.common.api.events.battles.*;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import java.util.*;
import java.time.Instant;

/** Single-player native PvE only. PvP/NPC/multi-player/unknown origins never receive guessed credit. */
public final class DefeatAccess {
    private record Target(UUID player,String species) { }
    private final Map<UUID,Map<UUID,Target>> battles=new HashMap<>();
    public void started(BattleStartedEvent.Post event,CaptureAccess captures){
        var battle=event.getBattle();var players=battle.getPlayers();if(players.size()!=1)return;
        var player=players.getFirst();if(!player.server.isSameThread())return;
        var targets=new HashMap<UUID,Target>();
        for(var actor:battle.getActors())if(actor instanceof PokemonBattleActor wild){var entity=wild.getEntity();if(entity==null||!entity.getPokemon().isWild()||captures.observe(entity,NativeSpawnProof.Source.ENTITY_LOAD)==null)continue;
            boolean opposite=true;for(var teammate:actor.getSide().getActors())if(teammate.isForPlayer(player))opposite=false;
            if(opposite)targets.put(entity.getPokemon().getUuid(),new Target(player.getUUID(),entity.getPokemon().getSpecies().getResourceIdentifier().toString()));
        }
        if(!targets.isEmpty())battles.put(battle.getBattleId(),Map.copyOf(targets));
    }
    public DefeatRecord fainted(BattleFaintedEvent event){
        var targets=battles.get(event.getBattle().getBattleId());if(targets==null)return null;
        var pokemon=event.getKilled();var target=targets.get(pokemon.getUuid());if(target==null||!(pokemon.getActor() instanceof PokemonBattleActor)||pokemon.getHealth()>0)return null;
        return new DefeatRecord(event.getBattle().getBattleId(),pokemon.getUuid(),target.player(),target.species(),Instant.now());
    }
    public void ended(UUID battle){battles.remove(battle);}
    public void clear(){battles.clear();}
}
