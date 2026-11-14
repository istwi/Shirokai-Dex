package br.com.shirokaidex.config;

import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.IOException;
import java.math.BigDecimal;

/** Bounded JSON tree reader that retains duplicate-field rejection at every depth. */
public final class StrictJson {
    private StrictJson() { }
    public static JsonObject object(JsonReader reader)throws IOException{return read(reader,0,new int[]{0}).getAsJsonObject();}
    private static JsonElement read(JsonReader r,int depth,int[] nodes)throws IOException {
        if(depth>24||++nodes[0]>4096)throw new IOException("JSON definition exceeds complexity limit");
        return switch(r.peek()){
            case BEGIN_OBJECT->{var o=new JsonObject();r.beginObject();while(r.hasNext()){String key=r.nextName();if(o.has(key))throw new IOException("Duplicate JSON definition field: "+key);o.add(key,read(r,depth+1,nodes));}r.endObject();yield o;}
            case BEGIN_ARRAY->{var a=new JsonArray();r.beginArray();while(r.hasNext()){if(a.size()>=256)throw new IOException("Definition array too large");a.add(read(r,depth+1,nodes));}r.endArray();yield a;}
            case STRING->new JsonPrimitive(r.nextString());case BOOLEAN->new JsonPrimitive(r.nextBoolean());
            case NUMBER->{String value=r.nextString();if(value.length()>64)throw new IOException("Definition number too large");yield new JsonPrimitive(new BigDecimal(value));}
            default->throw new IOException("Unsupported JSON definition value");
        };
    }
}
