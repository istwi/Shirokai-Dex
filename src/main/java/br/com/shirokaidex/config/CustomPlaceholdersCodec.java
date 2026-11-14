package br.com.shirokaidex.config;

import br.com.shirokaidex.placeholders.CustomPlaceholder;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.util.*;

final class CustomPlaceholdersCodec {
    private CustomPlaceholdersCodec() { }
    static List<CustomPlaceholder> read(JsonReader reader)throws IOException {
        var result=new ArrayList<CustomPlaceholder>();reader.beginArray();while(reader.hasNext()){
            if(result.size()>=64)throw new IOException("Too many custom placeholders");var object=StrictJson.object(reader);
            if(!Set.of("id","metric","argument","template").containsAll(object.keySet()))throw new IOException("Unknown custom placeholder field");
            for(var key:object.keySet())if(!object.get(key).isJsonPrimitive()||!object.get(key).getAsJsonPrimitive().isString())throw new IOException("Custom placeholder fields must be strings");
            if(!object.has("id")||!object.has("metric"))throw new IOException("Missing custom placeholder id/metric");
            result.add(new CustomPlaceholder(object.get("id").getAsString(),object.get("metric").getAsString(),object.has("argument")?object.get("argument").getAsString():"",object.has("template")?object.get("template").getAsString():"{value}"));
        }reader.endArray();return CustomPlaceholder.validate(result);
    }
}
