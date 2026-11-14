package br.com.shirokaidex.achievements;

import br.com.shirokaidex.rankings.CaptureRanking;
import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;

public final class AchievementJson {
    private AchievementJson() { }
    public static String write(AchievementDefinition d){
        var o=new JsonObject();o.addProperty("id",d.id());o.addProperty("name",d.name());o.addProperty("description",d.description());o.addProperty("icon",d.icon());o.addProperty("secret",d.secret());o.addProperty("hint",d.hint());o.addProperty("policy",d.policy().name());o.addProperty("retroactive",d.retroactive());o.addProperty("visibility",d.publicMedal()?"PUBLIC":"PRIVATE");o.addProperty("notify",d.notifyPlayer());var deps=new JsonArray();d.dependencies().stream().sorted().forEach(deps::add);o.add("dependencies",deps);o.add("requirement",requirement(d.requirement()));return o.toString();
    }
    public static AchievementDefinition read(JsonObject o){
        keys(o,Set.of("id","name","description","icon","secret","hint","policy","retroactive","visibility","notify","dependencies","requirement"));var deps=new HashSet<String>();if(o.has("dependencies")){var a=o.getAsJsonArray("dependencies");if(a.size()>128)throw new IllegalArgumentException("Too many dependencies");for(var value:a){if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()||!deps.add(value.getAsString()))throw new IllegalArgumentException("Invalid dependency");}}
        String visibility=string(o,"visibility","PUBLIC");if(!Set.of("PUBLIC","PRIVATE").contains(visibility))throw new IllegalArgumentException("Invalid medal visibility");
        return new AchievementDefinition(string(o,"id",null),string(o,"name",null),string(o,"description",""),string(o,"icon","minecraft:nether_star"),bool(o,"secret",false),string(o,"hint",""),AchievementDefinition.Policy.valueOf(string(o,"policy","PERMANENT")),bool(o,"retroactive",true),visibility.equals("PUBLIC"),bool(o,"notify",true),deps,readRequirement(o.getAsJsonObject("requirement"),0));
    }
    private static JsonObject requirement(Requirement r){
        var o=new JsonObject();
        if(r instanceof Requirement.All all){var a=new JsonArray();all.conditions().forEach(c->a.add(requirement(c)));o.add("all",a);}
        else if(r instanceof Requirement.Any any){var a=new JsonArray();any.conditions().forEach(c->a.add(requirement(c)));o.add("any",a);}
        else if(r instanceof Requirement.Count count){o.addProperty("type","CAPTURES");o.addProperty("metric",count.metric().name());o.addProperty("key",count.key());o.addProperty("minimum",count.minimum());}
        else if(r instanceof Requirement.Progress progress){o.addProperty("type","POKEDEX");o.addProperty("collection",progress.collection());if(progress.minimumRegistered()!=null)o.addProperty("minimum_registered",progress.minimumRegistered());else o.addProperty("minimum_percent",progress.minimumPercent());}
        else if(r instanceof Requirement.Form form){o.addProperty("type","FORM");o.addProperty("species",form.species());o.addProperty("form",form.form());}
        else if(r instanceof Requirement.Achieved achieved){o.addProperty("type","ACHIEVEMENT");o.addProperty("id",achieved.achievement());}
        return o;
    }
    private static Requirement readRequirement(JsonObject o,int depth){
        if(o==null||depth>8)throw new IllegalArgumentException("Invalid achievement requirement");
        if(o.has("all")||o.has("any")){
            boolean all=o.has("all");keys(o,Set.of(all?"all":"any"));var a=o.getAsJsonArray(all?"all":"any");if(a.isEmpty()||a.size()>32)throw new IllegalArgumentException("Invalid condition array");var children=new ArrayList<Requirement>();for(var value:a)children.add(readRequirement(value.getAsJsonObject(),depth+1));return all?new Requirement.All(children):new Requirement.Any(children);
        }
        String type=string(o,"type",null);return switch(type){
            case "CAPTURES"->{keys(o,Set.of("type","metric","key","minimum"));yield new Requirement.Count(CaptureRanking.Metric.valueOf(string(o,"metric",null)),string(o,"key",""),number(o,"minimum").longValueExact());}
            case "POKEDEX"->{keys(o,Set.of("type","collection","minimum_registered","minimum_percent"));yield new Requirement.Progress(string(o,"collection",null),o.has("minimum_registered")?number(o,"minimum_registered").intValueExact():null,o.has("minimum_percent")?number(o,"minimum_percent"):null);}
            case "FORM"->{keys(o,Set.of("type","species","form"));yield new Requirement.Form(string(o,"species",null),string(o,"form",null));}
            case "ACHIEVEMENT"->{keys(o,Set.of("type","id"));yield new Requirement.Achieved(string(o,"id",null));}
            default->throw new IllegalArgumentException("Unknown achievement requirement type");
        };
    }
    private static void keys(JsonObject o,Set<String> keys){for(String key:o.keySet())if(!keys.contains(key))throw new IllegalArgumentException("Unknown achievement field: "+key);}
    private static String string(JsonObject o,String key,String fallback){if(!o.has(key)){if(fallback==null)throw new IllegalArgumentException("Missing achievement field: "+key);return fallback;}var value=o.get(key);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Achievement string required");return value.getAsString();}
    private static boolean bool(JsonObject o,String key,boolean fallback){if(!o.has(key))return fallback;var value=o.get(key);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Achievement boolean required");return value.getAsBoolean();}
    private static BigDecimal number(JsonObject o,String key){if(!o.has(key)||!o.get(key).isJsonPrimitive()||!o.get(key).getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Achievement number required");return o.get(key).getAsBigDecimal();}
}
