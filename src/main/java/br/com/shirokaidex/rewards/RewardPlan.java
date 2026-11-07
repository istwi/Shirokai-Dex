package br.com.shirokaidex.rewards;

import br.com.shirokaidex.pokedex.CollectionDefinition;
import br.com.shirokaidex.pokedex.DexProgress;
import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;

/** First delivery scope: immutable plain item plans; registry/capacity validation belongs to server adapter. */
public record RewardPlan(String id, String collection, int minimumRegistered, BigDecimal minimumPercent, List<Item> items, boolean manual,List<PokemonReward> pokemon) {
    public RewardPlan(String id,String collection,int minimumRegistered,BigDecimal minimumPercent,List<Item> items,boolean manual){this(id,collection,minimumRegistered,minimumPercent,items,manual,List.of());}
    public RewardPlan(String id,String collection,int minimumRegistered,BigDecimal minimumPercent,List<Item> items){this(id,collection,minimumRegistered,minimumPercent,items,false);}
    public static RewardPlan manual(String id,List<Item> items){return new RewardPlan(id,"admin",0,null,items,true);}
    public record Item(String id, int count, String data) {
        public Item(String id,int count){this(id,count,"");}
        public Item {
            CollectionDefinition.identifier(id);
            if(count<1||count>2304)throw new IllegalArgumentException("item count must be 1..2304");
            if(data==null||data.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>16384)throw new IllegalArgumentException("item payload maximum 16 KiB");
        }
    }
    public RewardPlan {
        identifier(id); identifier(collection); items = List.copyOf(items); pokemon=List.copyOf(pokemon);
        if (items.size()+pokemon.size()<1 || items.size()+pokemon.size()>32 || pokemon.size()>6) throw new IllegalArgumentException("reward requires 1..32 actions, maximum six pokemon");
        if(manual&&!pokemon.isEmpty())throw new IllegalArgumentException("manual item plans cannot contain pokemon");
        if(items.stream().mapToInt(item -> item.data().getBytes(java.nio.charset.StandardCharsets.UTF_8).length).sum()>65536)throw new IllegalArgumentException("maximum 64 KiB payload per reward");
        if(manual && (!collection.equals("admin")||minimumRegistered!=0||minimumPercent!=null))throw new IllegalArgumentException("invalid manual plan");
        if (!manual && ((minimumRegistered > 0) == (minimumPercent != null) || minimumRegistered < 0)) throw new IllegalArgumentException("exactly one positive unlock threshold required");
        if (minimumPercent != null) {
            minimumPercent = minimumPercent.stripTrailingZeros();
            if (minimumPercent.signum() <= 0 || minimumPercent.compareTo(new BigDecimal("100")) > 0 || minimumPercent.scale() > 2)
                throw new IllegalArgumentException("percent must be >0 and <=100 with at most two decimals");
        }
    }
    public boolean unlocked(DexProgress progress) {
        if (manual || progress.total() == 0 || progress.registered().isEmpty()) return false;
        int registered = progress.registered().getAsInt();
        return minimumPercent == null ? registered >= minimumRegistered
                : BigDecimal.valueOf(registered).multiply(new BigDecimal("100")).compareTo(minimumPercent.multiply(BigDecimal.valueOf(progress.total()))) >= 0;
    }
    public String json() {
        JsonObject root = new JsonObject(); root.addProperty("format_version", 1); root.addProperty("id", id); root.addProperty("collection", collection);
        if(manual)root.addProperty("source","ADMIN_ITEM");
        else if (minimumPercent == null) root.addProperty("minimum_registered", minimumRegistered); else root.addProperty("minimum_percent", minimumPercent);
        JsonArray actions = new JsonArray();
        for (Item item : items) { JsonObject value = new JsonObject(); value.addProperty("type", item.data().isEmpty()?"ITEM":"CUSTOM_ITEM"); value.addProperty("id", item.id()); value.addProperty("count", item.count()); if(!item.data().isEmpty())value.addProperty("payload_snbt",item.data()); actions.add(value); }
        root.add("items", actions);
        if(!pokemon.isEmpty()){var entries=new JsonArray();pokemon.forEach(value->entries.add(value.json()));root.add("pokemon",entries);}
        String json=root.toString();
        if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>262144)throw new IllegalArgumentException("operation plan exceeds 256 KiB");
        return json;
    }
    public int steps(){return items.size()+pokemon.size();}
    public RewardPlan withPokemon(List<PokemonReward> values){return new RewardPlan(id,collection,minimumRegistered,minimumPercent,items,manual,values);}
    public RewardPlan definition(){return withPokemon(pokemon.stream().map(value->value.withSnapshot("")).toList());}
    public RewardPlan reuse(String reserved){
        var json=JsonParser.parseString(reserved).getAsJsonObject();
        if(pokemon.isEmpty()){if(!json().equals(reserved))throw new IllegalArgumentException("reserved plan differs");return this;}
        var entries=json.getAsJsonArray("pokemon");if(entries==null||entries.size()!=pokemon.size())throw new IllegalArgumentException("reserved pokemon count differs");
        var restored=new ArrayList<PokemonReward>();for(int i=0;i<pokemon.size();i++)restored.add(pokemon.get(i).withSnapshot(entries.get(i).getAsJsonObject().get("snapshot_snbt").getAsString()));
        var plan=withPokemon(restored);if(!plan.json().equals(reserved))throw new IllegalArgumentException("reserved plan differs");return plan;
    }
    private static void identifier(String id) {
        if (id == null || !id.matches("[a-z][a-z0-9_]{0,47}")) throw new IllegalArgumentException("invalid reward or collection id");
    }
}
