package br.com.shirokaidex.rewards;

import br.com.shirokaidex.pokedex.CollectionDefinition;
import com.google.gson.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Requested attributes plus optional, per-operation frozen Cobblemon NBT. No game objects cross threads. */
public record PokemonReward(String species,String form,Integer level,boolean shiny,String nature,String ability,
                            String gender,String pokeball,String heldItem,List<String> moves,
                            Map<String,Integer> ivs,Map<String,Integer> evs,String snapshot) {
    public static final Set<String> STATS=Set.of("hp","attack","defence","special_attack","special_defence","speed");
    public PokemonReward {
        CollectionDefinition.identifier(species);form=optional(form);nature=optional(nature);ability=optional(ability);gender=optional(gender);pokeball=optional(pokeball);heldItem=optional(heldItem);
        if(level!=null&&(level<1||level>100))throw new IllegalArgumentException("pokemon level must be 1..100");
        for(String id:List.of(nature,pokeball,heldItem))if(!id.isEmpty())CollectionDefinition.identifier(id);
        for(String name:List.of(form,ability))if(!name.isEmpty()&&!name.matches("[a-z0-9_/-]{1,64}"))throw new IllegalArgumentException("invalid form or ability");
        if(!gender.isEmpty()&&!Set.of("MALE","FEMALE","GENDERLESS").contains(gender))throw new IllegalArgumentException("invalid gender");
        moves=List.copyOf(moves);if(moves.size()>4||new HashSet<>(moves).size()!=moves.size())throw new IllegalArgumentException("maximum four distinct moves");
        for(String move:moves)if(!move.matches("[a-z0-9_]{1,64}"))throw new IllegalArgumentException("invalid move id");
        ivs=stats(ivs,31);evs=stats(evs,252);if(evs.values().stream().mapToInt(Integer::intValue).sum()>510)throw new IllegalArgumentException("maximum 510 EVs");
        if(snapshot==null||snapshot.getBytes(StandardCharsets.UTF_8).length>65536)throw new IllegalArgumentException("pokemon snapshot maximum 64 KiB");
    }
    public PokemonReward withSnapshot(String value){return new PokemonReward(species,form,level,shiny,nature,ability,gender,pokeball,heldItem,moves,ivs,evs,value);}
    public JsonObject json(){
        var json=new JsonObject();json.addProperty("type","POKEMON");json.addProperty("species",species);
        if(!form.isEmpty())json.addProperty("form",form);if(level!=null)json.addProperty("level",level);json.addProperty("shiny",shiny);
        for(var entry:Map.of("nature",nature,"ability",ability,"gender",gender,"pokeball",pokeball,"held_item",heldItem).entrySet().stream().sorted(Map.Entry.comparingByKey()).toList())if(!entry.getValue().isEmpty())json.addProperty(entry.getKey(),entry.getValue());
        if(!moves.isEmpty()){var values=new JsonArray();moves.forEach(values::add);json.add("moves",values);}
        for(var pair:Map.of("ivs",ivs,"evs",evs).entrySet().stream().sorted(Map.Entry.comparingByKey()).toList())if(!pair.getValue().isEmpty()){
            var values=new JsonObject();pair.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->values.addProperty(e.getKey(),e.getValue()));json.add(pair.getKey(),values);
        }
        if(!snapshot.isEmpty())json.addProperty("snapshot_snbt",snapshot);return json;
    }
    private static String optional(String value){return value==null?"":value;}
    private static Map<String,Integer> stats(Map<String,Integer> values,int max){
        var copy=Map.copyOf(values);for(var entry:copy.entrySet())if(!STATS.contains(entry.getKey())||entry.getValue()<0||entry.getValue()>max)throw new IllegalArgumentException("invalid stat or range");return copy;
    }
}
