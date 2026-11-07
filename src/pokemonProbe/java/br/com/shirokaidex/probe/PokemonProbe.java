package br.com.shirokaidex.probe;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import com.mojang.brigadier.arguments.StringArgumentType;
import static net.minecraft.commands.Commands.*;

/** Disposable-world fixtures only. This source set is never included in the mod JAR. */
public final class PokemonProbe implements ModInitializer {
    public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->{
        var player=argument("player",StringArgumentType.word());
        for(String action:java.util.List.of("stats","partyfill","fillall","freepc","touch"))player.then(literal(action).executes(ctx->run(ctx.getSource(),StringArgumentType.getString(ctx,"player"),action)));
        dispatcher.register(literal("dexpokemonprobe").requires(source->source.hasPermission(4)).then(player));
    });}
    private int run(CommandSourceStack source,String name,String action){
        var server=source.getServer();if(!server.getWorldPath(LevelResource.ROOT).toString().contains("pokemon-reward-validation")||!name.startsWith("DexPokemon"))throw new IllegalStateException("Probe requires disposable world/profile");
        var player=server.getPlayerList().getPlayerByName(name);if(player==null)throw new IllegalArgumentException("Player not online");
        var party=Cobblemon.INSTANCE.getStorage().getParty(player);var pc=Cobblemon.INSTANCE.getStorage().getPC(player);
        if(action.equals("partyfill")||action.equals("fillall")){
            if(pc.getBoxes().size()!=1){if(pc.getBoxes().stream().anyMatch(box->box.iterator().hasNext()))throw new IllegalStateException("Resize fixture requires empty PC");pc.resize(1,false,pokemon->kotlin.Unit.INSTANCE);}
            while(party.getFirstAvailablePosition()!=null)if(!party.add(filler(player)))throw new IllegalStateException("Party fixture failed");
        }
        if(action.equals("fillall"))while(pc.getFirstAvailablePosition()!=null)if(!pc.add(filler(player)))throw new IllegalStateException("PC fixture failed");
        if(action.equals("freepc")){if(!pc.iterator().hasNext())throw new IllegalStateException("PC empty");for(var candidate:pc)if(candidate.getSpecies().getResourceIdentifier().toString().equals("cobblemon:magikarp")){pc.remove(candidate);break;}}
        if(action.equals("touch")){for(var pokemon:party)party.onPokemonChanged(pokemon);for(var pokemon:pc)pc.onPokemonChanged(pokemon);}
        var result=new JsonObject();var a=new JsonArray();for(var pokemon:party)if(!pokemon.getSpecies().getResourceIdentifier().toString().equals("cobblemon:magikarp"))a.add(json(pokemon));var b=new JsonArray();int pcCount=0;for(var pokemon:pc){pcCount++;if(!pokemon.getSpecies().getResourceIdentifier().toString().equals("cobblemon:magikarp"))b.add(json(pokemon));}result.addProperty("party_count",party.occupied());result.addProperty("pc_count",pcCount);result.add("party",a);result.add("pc",b);result.addProperty("free",party.size()-party.occupied()+pc.getBoxes().stream().mapToInt(box->box.getUnoccupiedSlots()).sum());
        source.sendSuccess(()->Component.literal(result.toString()),false);return 1;
    }
    private Pokemon filler(net.minecraft.server.level.ServerPlayer player){var pokemon=new Pokemon();pokemon.setSpecies(PokemonSpecies.INSTANCE.getByIdentifier(ResourceLocation.parse("cobblemon:magikarp")));pokemon.initialize();pokemon.setOriginalTrainer(player.getUUID());return pokemon;}
    private JsonObject json(Pokemon p){
        var j=new JsonObject();j.addProperty("uuid",p.getUuid().toString());j.addProperty("species",p.getSpecies().getResourceIdentifier().toString());j.addProperty("form",p.getForm().getName());j.addProperty("level",p.getLevel());j.addProperty("shiny",p.getShiny());j.addProperty("nature",p.getNature().getName().toString());j.addProperty("ability",p.getAbility().getName());j.addProperty("gender",p.getGender().name());j.addProperty("ball",p.getCaughtBall().getName().toString());j.addProperty("held",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(p.heldItem().getItem()).toString());
        var moves=new JsonArray();p.getMoveSet().getMoves().forEach(move->moves.add(move.getTemplate().getName()));j.add("moves",moves);var ivs=new JsonObject();var evs=new JsonObject();for(var stat:Stats.Companion.getPERMANENT()){ivs.addProperty(stat.getShowdownId(),p.getIvs().get(stat));evs.addProperty(stat.getShowdownId(),p.getEvs().getOrDefault(stat));}j.add("ivs",ivs);j.add("evs",evs);return j;
    }
}
