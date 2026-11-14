package br.com.shirokaidex.config;

import br.com.shirokaidex.profiles.StatisticsPrivacy;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.util.*;

final class StatisticsPrivacyCodec {
    private StatisticsPrivacyCodec() { }
    static StatisticsPrivacy read(JsonReader reader)throws IOException {
        var o=StrictJson.object(reader);for(String key:o.keySet())if(!Set.of("enabled","defaults","allow_player_override").contains(key))throw new IOException("Unknown statistics privacy field");
        var defaults=new EnumMap<StatisticsPrivacy.Key,Boolean>(StatisticsPrivacy.Key.class);defaults.putAll(StatisticsPrivacy.defaultsPolicy().defaults());
        if(o.has("defaults")){var values=o.getAsJsonObject("defaults");for(String key:values.keySet()){StatisticsPrivacy.Key type;try{type=StatisticsPrivacy.Key.parse(key);}catch(IllegalArgumentException invalid){throw new IOException("Unknown statistic privacy key",invalid);}if(!key.equals(type.id()))throw new IOException("Privacy keys must be lowercase");defaults.put(type,bool(values,key,true));}}
        return new StatisticsPrivacy(bool(o,"enabled",true),defaults,bool(o,"allow_player_override",true));
    }
    private static boolean bool(JsonObject o,String key,boolean fallback)throws IOException {if(!o.has(key))return fallback;var value=o.get(key);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw new IOException("Privacy boolean required");return value.getAsBoolean();}
}
