package br.com.shirokaidex.competitions;

import br.com.shirokaidex.rankings.CaptureRanking;
import com.google.gson.*;
import java.time.*;
import java.util.*;

public final class CompetitionJson {
    private CompetitionJson() { }
    public static String write(CompetitionDefinition d){
        var o=new JsonObject();o.addProperty("id",d.id());o.addProperty("name",d.name());o.addProperty("cadence",d.cadence().name());o.addProperty("timezone",d.timezone().getId());
        if(d.start()!=null)o.addProperty("start",d.start().toString());if(d.end()!=null)o.addProperty("end",d.end().toString());
        o.addProperty("metric",d.metric().name());o.addProperty("key",d.key());o.addProperty("category_update_mode",d.categoryMode().name());o.addProperty("visibility",d.visibility().name());o.addProperty("permission",d.permission());
        var review=new JsonObject();review.addProperty("enabled",d.reviewHours()>0);review.addProperty("duration_hours",d.reviewHours());review.addProperty("auto_finalize",d.autoFinalize());o.add("result_review",review);
        var anti=new JsonObject();anti.addProperty("mode",d.reviewMode().name());anti.addProperty("hourly_threshold",d.hourlyReviewThreshold());o.add("antiexploit",anti);o.add("prizes",writePrizes(d.prizes()));return o.toString();
    }
    public static CompetitionDefinition read(JsonObject o){
        keys(o,Set.of("id","name","cadence","timezone","start","end","metric","key","category_update_mode","visibility","permission","result_review","antiexploit","prizes"));
        var review=o.has("result_review")?o.getAsJsonObject("result_review"):new JsonObject();keys(review,Set.of("enabled","duration_hours","auto_finalize","hold_affected_rewards","announce_only_final_results"));
        // Unsafe bypass of pending reviews is not supported. Announcements are disabled until the notification layer.
        if(!bool(review,"hold_affected_rewards",true))throw new IllegalArgumentException("Affected pending results must remain held");
        if(!bool(review,"announce_only_final_results",true))throw new IllegalArgumentException("Only final result announcements are supported");
        var anti=o.has("antiexploit")?o.getAsJsonObject("antiexploit"):new JsonObject();keys(anti,Set.of("mode","hourly_threshold"));
        return new CompetitionDefinition(string(o,"id",null),string(o,"name",null),CompetitionDefinition.Cadence.valueOf(string(o,"cadence",null)),ZoneId.of(string(o,"timezone","America/Sao_Paulo")),o.has("start")?Instant.parse(string(o,"start",null)):null,o.has("end")?Instant.parse(string(o,"end",null)):null,CaptureRanking.Metric.valueOf(string(o,"metric",null)),string(o,"key",""),CompetitionDefinition.CategoryMode.valueOf(string(o,"category_update_mode","SNAPSHOT")),CompetitionDefinition.Visibility.valueOf(string(o,"visibility","PUBLIC")),bool(review,"enabled",true)?integer(review,"duration_hours",24):0,bool(review,"auto_finalize",true),CompetitionDefinition.ReviewMode.valueOf(string(anti,"mode","LOG_ONLY")),integer(anti,"hourly_threshold",0),string(o,"permission",""),o.has("prizes")?readPrizes(o.getAsJsonObject("prizes")):PrizePolicy.disabled());
    }
    private static JsonObject writePrizes(PrizePolicy policy){
        var o=new JsonObject();o.addProperty("enabled",policy.enabled());o.addProperty("minimum_participants",policy.minimumParticipants());o.addProperty("minimum_captures",policy.minimumCaptures());o.addProperty("redistribute",policy.redistribute());o.addProperty("expiry_days",policy.expiryDays());var bands=new JsonArray();for(var band:policy.bands()){var entry=new JsonObject();entry.addProperty("maximum_position",band.maximumPosition());entry.addProperty("reward",band.reward());entry.addProperty("minimum_score",band.minimumScore());bands.add(entry);}o.add("bands",bands);return o;
    }
    private static PrizePolicy readPrizes(JsonObject o){
        keys(o,Set.of("enabled","minimum_participants","minimum_captures","redistribute","expiry_days","bands"));var bands=new ArrayList<PrizePolicy.Band>();
        if(o.has("bands")){var values=o.getAsJsonArray("bands");if(values.size()>3)throw new IllegalArgumentException("Too many prize bands");for(var value:values){var band=value.getAsJsonObject();keys(band,Set.of("maximum_position","reward","minimum_score"));bands.add(new PrizePolicy.Band(integer(band,"maximum_position",0),string(band,"reward",null),integer(band,"minimum_score",0)));}}
        return new PrizePolicy(bool(o,"enabled",false),bands,integer(o,"minimum_participants",2),integer(o,"minimum_captures",1),bool(o,"redistribute",false),integer(o,"expiry_days",0));
    }
    private static void keys(JsonObject o,Set<String> allowed){for(String key:o.keySet())if(!allowed.contains(key))throw new IllegalArgumentException("Unknown competition field: "+key);}
    private static String string(JsonObject o,String key,String fallback){if(!o.has(key)){if(fallback==null)throw new IllegalArgumentException("Missing competition field: "+key);return fallback;}var value=o.get(key);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Competition string required");return value.getAsString();}
    private static boolean bool(JsonObject o,String key,boolean fallback){if(!o.has(key))return fallback;var value=o.get(key);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Competition boolean required");return value.getAsBoolean();}
    private static int integer(JsonObject o,String key,int fallback){if(!o.has(key))return fallback;var value=o.get(key);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber()||!value.getAsString().matches("[0-9]{1,10}"))throw new IllegalArgumentException("Competition integer required");return Integer.parseInt(value.getAsString());}
}
