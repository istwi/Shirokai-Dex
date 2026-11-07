package br.com.shirokaidex.config;

import br.com.shirokaidex.rewards.PokemonReward;
import com.google.gson.stream.*;
import java.io.IOException;
import java.util.*;

final class PokemonRewardsCodec {
    private PokemonRewardsCodec() { }
    static List<PokemonReward> read(JsonReader r)throws IOException {
        List<PokemonReward> result=new ArrayList<>();r.beginArray();
        while(r.hasNext()){
            if(result.size()>=6)throw error(r,"maximum six pokemon per reward");
            Set<String> keys=new HashSet<>();Map<String,String> text=new HashMap<>();Integer level=null;boolean shiny=false;List<String> moves=new ArrayList<>();Map<String,Integer> ivs=Map.of(),evs=Map.of();
            r.beginObject();while(r.hasNext()){
                String key=r.nextName();unique(r,keys,key);
                switch(key){
                    case "species","form","nature","ability","gender","pokeball","held_item"->text.put(key,string(r));
                    case "type"->{if(!string(r).equals("POKEMON"))throw error(r,"pokemon type must be POKEMON");}
                    case "level"->level=integer(r);
                    case "shiny"->{if(r.peek()!=JsonToken.BOOLEAN)throw error(r,"shiny must be boolean");shiny=r.nextBoolean();}
                    case "moves"->{r.beginArray();while(r.hasNext()){if(moves.size()>=4)throw error(r,"maximum four moves");moves.add(string(r));}r.endArray();}
                    case "ivs"->ivs=stats(r);
                    case "evs"->evs=stats(r);
                    default->throw error(r,"unknown pokemon field "+key);
                }
            }r.endObject();
            result.add(new PokemonReward(text.get("species"),text.get("form"),level,shiny,text.get("nature"),text.get("ability"),text.get("gender"),text.get("pokeball"),text.get("held_item"),moves,ivs,evs,""));
        }r.endArray();return List.copyOf(result);
    }
    private static Map<String,Integer> stats(JsonReader r)throws IOException{
        Map<String,Integer> values=new HashMap<>();r.beginObject();while(r.hasNext()){String key=r.nextName();if(values.containsKey(key)||!PokemonReward.STATS.contains(key))throw error(r,"unknown or duplicate stat "+key);values.put(key,integer(r));}r.endObject();return Map.copyOf(values);
    }
    private static int integer(JsonReader r)throws IOException{if(r.peek()!=JsonToken.NUMBER)throw error(r,"expected integer");String value=r.nextString();if(!value.matches("0|[1-9][0-9]{0,8}"))throw error(r,"expected nonnegative integer");return Integer.parseInt(value);}
    private static String string(JsonReader r)throws IOException{if(r.peek()!=JsonToken.STRING)throw error(r,"expected string");return r.nextString();}
    private static void unique(JsonReader r,Set<String> keys,String key)throws IOException{if(!keys.add(key))throw error(r,"duplicate field "+key);}
    private static IOException error(JsonReader r,String message){return new IOException("config.json "+r.getPath()+": "+message);}
}
