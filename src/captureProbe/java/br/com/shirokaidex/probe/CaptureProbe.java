package br.com.shirokaidex.probe;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.Consumer;
import static net.minecraft.commands.Commands.literal;

/** Test-only console fixture. Never fabricates a spawn cause or a first capture event. */
public final class CaptureProbe implements ModInitializer {
    private PokemonCapturedEvent last;
    private EmptyPokeBallEntity thrown;private PokemonEntity aimed;private int hits;
    public void onInitialize(){
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe((Consumer<com.cobblemon.mod.common.api.events.pokeball.ThrownPokeballHitEvent>) event->{if(event.getPokeBall().getOwner() instanceof ServerPlayer p&&p.getGameProfile().getName().equals("DexCapture"))hits++;});
        CobblemonEvents.POKEMON_CAPTURED.subscribe((Consumer<PokemonCapturedEvent>) event->{if(event.getPlayer().getGameProfile().getName().equals("DexCapture"))last=event;});
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->{
            var root=literal("dexcaptureprobe").requires(source->source.getPlayer()==null&&source.getServer().getWorldData().getLevelName().equals("native-capture-validation"));
            for(String mode:List.of("scan","throw","unknown","replay","park","parked","status"))root.then(literal(mode).executes(context->run(context.getSource(),mode)));
            dispatcher.register(root);
        });
    }
    private int run(CommandSourceStack source,String mode){
        var server=source.getServer();var player=server.getPlayerList().getPlayerByName("DexCapture");if(player==null)throw new IllegalStateException("Reserved player required");
        if(mode.equals("status")){source.sendSuccess(()->Component.literal("Hits="+hits+"; ball="+(thrown==null?"none":thrown.getCaptureState()+"/removed="+thrown.isRemoved()+"/pos="+thrown.position())+"; target="+(aimed==null?"none":aimed.getBoundingBox()+"/wild="+aimed.getPokemon().isWild())+"; event="+(last!=null)),false);return 1;}
        if(mode.equals("replay")){if(last==null)throw new IllegalStateException("No native capture to replay");CobblemonEvents.POKEMON_CAPTURED.emit(last);source.sendSuccess(()->Component.literal("Duplicate native event replayed"),false);return 1;}
        List<PokemonEntity> candidates=new ArrayList<>();for(var entity:server.overworld().getAllEntities())if(entity instanceof PokemonEntity pokemon&&pokemon.isAlive()&&pokemon.getPokemon().isWild())candidates.add(pokemon);
        var selected=candidates.stream().filter(p->mode.equals("parked")?p.getTags().contains("dex-capture-parked"):p.getSpawnCause()!=null&&p.getSpawnCause().getSpawner().getClass()==PlayerSpawner.class&&p.getPokemon().getSpecies().getResourceIdentifier().toString().equals("cobblemon:pikachu")&&p.getPokemon().getShiny()&&!p.getTags().contains("dex-capture-parked")).findFirst();
        if(mode.equals("scan")){int count=(int)candidates.stream().filter(p->p.getSpawnCause()!=null).count();source.sendSuccess(()->Component.literal("Natural candidate: "+selected.isPresent()+"; caused wild entities: "+count),false);return selected.isPresent()?1:0;}
        PokemonEntity pokemon;
        if(mode.equals("unknown")){var properties=new PokemonProperties();properties.setSpecies("pikachu");properties.setLevel(5);pokemon=properties.createEntity(server.overworld());server.overworld().addFreshEntity(pokemon);}
        else{if(selected.isEmpty()){source.sendSuccess(()->Component.literal("No matching native candidate"),false);return 0;}pokemon=selected.get();}
        if(mode.equals("park")){pokemon.addTag("dex-capture-parked");pokemon.setPersistenceRequired();pokemon.setNoAi(true);pokemon.moveTo(player.getX()+3,player.getY(),player.getZ());source.sendSuccess(()->Component.literal("Native candidate parked: "+pokemon.getPokemon().getUuid()),false);return 1;}
        // Clear only a tiny owned test arena so terrain cannot intercept the physical projectile.
        int bx=player.blockPosition().getX(),by=player.blockPosition().getY()+1,bz=player.blockPosition().getZ();
        for(int x=bx-1;x<=bx+5;x++)for(int z=bz-1;z<=bz+1;z++){
            server.overworld().setBlock(new net.minecraft.core.BlockPos(x,by-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            for(int y=by;y<=by+3;y++)server.overworld().setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        }
        player.teleportTo(bx+0.5,by,bz+0.5);pokemon.setNoAi(true);pokemon.moveTo(bx+3.5,by,bz+0.5);
        var ball=new EmptyPokeBallEntity(PokeBalls.INSTANCE.getMASTER_BALL(),server.overworld(),player,CobblemonEntities.EMPTY_POKEBALL);
        ball.setNoGravity(true);ball.setPos(player.getX()+1,player.getY()+0.3,player.getZ());ball.setDeltaMovement(0.65,0,0);server.overworld().addFreshEntity(ball);thrown=ball;aimed=pokemon;
        var id=pokemon.getPokemon().getUuid();var cause=pokemon.getSpawnCause();source.sendSuccess(()->Component.literal("Native ball launched: "+id+"; spawn cause: "+(cause==null?"unknown":cause.getSpawner().getClass().getSimpleName())),false);return 1;
    }
}
