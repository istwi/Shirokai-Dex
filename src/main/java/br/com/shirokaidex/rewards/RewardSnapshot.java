package br.com.shirokaidex.rewards;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;

/** Versioned internal snapshot of a validated definition, independent from later config edits. */
public final class RewardSnapshot {
    private RewardSnapshot() { }
    public static String encode(RewardPlan plan){
        var root=new JsonObject();root.addProperty("format_version",1);root.add("plan",new Gson().toJsonTree(plan));String json=root.toString();
        if(json.getBytes(StandardCharsets.UTF_8).length>262144)throw new IllegalArgumentException("Reward snapshot too large");return json;
    }
    public static RewardPlan decode(String json){
        if(json==null||json.getBytes(StandardCharsets.UTF_8).length>262144)throw new IllegalArgumentException("Invalid reward snapshot size");
        var root=JsonParser.parseString(json).getAsJsonObject();if(root.size()!=2||!root.has("format_version")||!root.get("format_version").getAsString().equals("1")||!root.has("plan"))throw new IllegalArgumentException("Unknown reward snapshot version");
        var gson=new Gson();var result=gson.fromJson(root.get("plan"),RewardPlan.class);
        if(result==null||!gson.toJsonTree(result).equals(root.get("plan")))throw new IllegalArgumentException("Reward snapshot differs from validated definition");return result;
    }
    public static RewardPlan awardPlan(RewardPlan template,String id){
        return new RewardPlan(id,template.collection(),1,null,template.items(),false,template.pokemon(),template.currencies(),template.commands());
    }
}
