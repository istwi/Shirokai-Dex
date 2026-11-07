package br.com.shirokaidex.rewards;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.pokemon.*;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Typed validation never delegates invalid strings to the permissive properties parser. */
public final class PokemonPayload {
    private PokemonPayload() { }
    public static Pokemon create(MinecraftServer server,ServerPlayer owner,PokemonReward request){
        thread(server);var species=PokemonSpecies.INSTANCE.getByIdentifier(ResourceLocation.parse(request.species()));
        if(species==null||!species.getImplemented())throw new IllegalArgumentException("unknown/unimplemented pokemon species");
        List<FormData> forms=new ArrayList<>();forms.add(species.getStandardForm());forms.addAll(species.getForms());
        var form=request.form().isEmpty()?species.getStandardForm():forms.stream().filter(f->f.getName().equalsIgnoreCase(request.form())).findFirst().orElseThrow(()->new IllegalArgumentException("unknown form"));
        int max=Cobblemon.INSTANCE.getConfig().getMaxPokemonLevel();int level=request.level()==null?Math.min(5,max):request.level();
        if(level>max)throw new IllegalArgumentException("level exceeds server maximum");
        if(!request.gender().isEmpty()&&!form.getPossibleGenders().contains(Gender.valueOf(request.gender())))throw new IllegalArgumentException("gender is invalid for form");
        if(!request.ability().isEmpty()){
            if(Abilities.get(request.ability())==null)throw new IllegalArgumentException("unknown ability");
            boolean legal=false;for(var potential:form.getAbilities())if(potential.getTemplate().getName().equals(request.ability()))legal=true;
            if(!legal)throw new IllegalArgumentException("ability not in form pool");
        }
        for(String move:request.moves())if(Moves.getByName(move)==null||!form.getMoves().getAllLegalMoves().contains(Moves.getByName(move)))throw new IllegalArgumentException("unknown or illegal move for form");
        if(!request.nature().isEmpty()&&Natures.INSTANCE.getNature(ResourceLocation.parse(request.nature()))==null)throw new IllegalArgumentException("unknown nature");
        if(!request.pokeball().isEmpty()&&PokeBalls.INSTANCE.getPokeBall(ResourceLocation.parse(request.pokeball()))==null)throw new IllegalArgumentException("unknown pokeball");
        if(!request.heldItem().isEmpty()&&(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(request.heldItem()))||request.heldItem().equals("minecraft:air")))throw new IllegalArgumentException("unknown held item");
        var pokemon=new Pokemon();pokemon.setSpecies(species);pokemon.setForcedAspects(new HashSet<>(form.getAspects()));pokemon.setLevel(level);pokemon.setShiny(request.shiny());pokemon.initialize();pokemon.updateAspects();pokemon.updateForm();
        if(!pokemon.getForm().getName().equalsIgnoreCase(form.getName()))throw new IllegalArgumentException("form cannot be represented by its real aspects");
        if(!request.gender().isEmpty())pokemon.setGender(Gender.valueOf(request.gender()));
        if(!request.nature().isEmpty())pokemon.setNature(Natures.INSTANCE.getNature(ResourceLocation.parse(request.nature())));
        if(!request.ability().isEmpty()){
            var match=java.util.stream.StreamSupport.stream(pokemon.getForm().getAbilities().spliterator(),false).filter(potential->potential.getTemplate().getName().equals(request.ability())).findFirst().orElseThrow(()->new IllegalArgumentException("ability invalid for effective form"));
            pokemon.updateAbility(match.getTemplate().create(false,match.getPriority()));
        }
        if(!request.pokeball().isEmpty())pokemon.setCaughtBall(Objects.requireNonNull(PokeBalls.INSTANCE.getPokeBall(ResourceLocation.parse(request.pokeball()))));
        request.ivs().forEach((key,value)->pokemon.setIV(stat(key),value));request.evs().forEach((key,value)->pokemon.setEV(stat(key),value));
        if(!request.moves().isEmpty()){pokemon.getMoveSet().clear();for(int i=0;i<request.moves().size();i++)pokemon.getMoveSet().setMove(i,Objects.requireNonNull(Moves.getByName(request.moves().get(i))).create());}
        if(!request.heldItem().isEmpty())pokemon.swapHeldItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(request.heldItem()))),false,false);
        if(owner!=null)pokemon.setOriginalTrainer(owner.getUUID());pokemon.heal();verify(request,pokemon);
        return pokemon;
    }
    public static PokemonReward freeze(MinecraftServer server,ServerPlayer owner,PokemonReward request){
        var pokemon=create(server,owner,request);String data=pokemon.saveToNBT(server.registryAccess(),new net.minecraft.nbt.CompoundTag()).toString();
        var frozen=request.withSnapshot(data);var restored=restore(server,frozen);
        if(!signature(pokemon).equals(signature(restored)))throw new IllegalArgumentException("pokemon snapshot did not round trip");return frozen;
    }
    public static Pokemon restore(MinecraftServer server,PokemonReward request){
        thread(server);if(request.snapshot().isEmpty())throw new IllegalArgumentException("pokemon snapshot not prepared");
        try{var pokemon=Pokemon.Companion.loadFromNBT(server.registryAccess(),TagParser.parseTag(request.snapshot()));verify(request,pokemon);return pokemon;}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new IllegalArgumentException("invalid pokemon snapshot",failure);}
    }
    private static void verify(PokemonReward request,Pokemon pokemon){
        if(!pokemon.getForm().getPossibleGenders().contains(pokemon.getGender()))throw new IllegalArgumentException("gender invalid for effective form");
        boolean legalAbility=false;for(var potential:pokemon.getForm().getAbilities())if(potential.getTemplate().getName().equals(pokemon.getAbility().getName()))legalAbility=true;
        if(!legalAbility)throw new IllegalArgumentException("ability invalid for effective form");
        for(var move:pokemon.getMoveSet().getMoves())if(!pokemon.getForm().getMoves().getAllLegalMoves().contains(move.getTemplate()))throw new IllegalArgumentException("move invalid for effective form");
        if(!pokemon.getSpecies().getResourceIdentifier().toString().equals(request.species())||pokemon.getShiny()!=request.shiny())throw new IllegalArgumentException("pokemon properties changed");
        if(!request.form().isEmpty()&&!pokemon.getForm().getName().equalsIgnoreCase(request.form()))throw new IllegalArgumentException("form changed");
        if(request.level()!=null&&pokemon.getLevel()!=request.level())throw new IllegalArgumentException("level changed");
        if(!request.nature().isEmpty()&&!pokemon.getNature().getName().toString().equals(request.nature()))throw new IllegalArgumentException("nature changed");
        if(!request.ability().isEmpty()&&!pokemon.getAbility().getName().equals(request.ability()))throw new IllegalArgumentException("ability changed");
        if(!request.gender().isEmpty()&&!pokemon.getGender().name().equals(request.gender()))throw new IllegalArgumentException("gender changed");
        if(!request.pokeball().isEmpty()&&!pokemon.getCaughtBall().getName().toString().equals(request.pokeball()))throw new IllegalArgumentException("pokeball changed");
        if(!request.heldItem().isEmpty()&&!BuiltInRegistries.ITEM.getKey(pokemon.heldItem().getItem()).toString().equals(request.heldItem()))throw new IllegalArgumentException("held item changed");
        request.ivs().forEach((key,value)->{if(!Objects.equals(pokemon.getIvs().get(stat(key)),value))throw new IllegalArgumentException("IV changed");});
        request.evs().forEach((key,value)->{if(pokemon.getEvs().getOrDefault(stat(key))!=value)throw new IllegalArgumentException("EV changed");});
        if(!request.moves().isEmpty()&&!pokemon.getMoveSet().getMoves().stream().map(move->move.getTemplate().getName()).toList().equals(request.moves()))throw new IllegalArgumentException("moves changed");
    }
    public static String signature(Pokemon p){
        StringBuilder value=new StringBuilder().append(p.getUuid()).append('|').append(p.getSpecies().getResourceIdentifier()).append('|').append(p.getForm().getName()).append('|').append(p.getLevel()).append('|').append(p.getShiny()).append('|').append(p.getGender()).append('|').append(p.getNature().getName()).append('|').append(p.getAbility().getName()).append('|').append(p.getCaughtBall().getName()).append('|').append(p.getOriginalTrainer()).append('|').append(p.heldItem());
        for(String key:PokemonReward.STATS.stream().sorted().toList())value.append('|').append(p.getIvs().get(stat(key))).append(':').append(p.getEvs().getOrDefault(stat(key)));
        p.getMoveSet().getMoves().forEach(move->value.append('|').append(move.getTemplate().getName()));return value.toString();
    }
    private static Stats stat(String key){return switch(key){case "hp"->Stats.HP;case "attack"->Stats.ATTACK;case "defence"->Stats.DEFENCE;case "special_attack"->Stats.SPECIAL_ATTACK;case "special_defence"->Stats.SPECIAL_DEFENCE;case "speed"->Stats.SPEED;default->throw new IllegalArgumentException("unknown stat");};}
    private static void thread(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Pokemon API requires server thread");}
}
