package br.com.shirokaidex.config;

import br.com.shirokaidex.competitions.*;
import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.IOException;
import java.util.*;

final class CompetitionsCodec {
    private CompetitionsCodec() { }
    static List<CompetitionDefinition> read(JsonReader r)throws IOException {
        var definitions=new ArrayList<CompetitionDefinition>();r.beginArray();
        while(r.hasNext()){if(definitions.size()>=32)throw new IOException("Too many competitions");definitions.add(CompetitionJson.read(object(r,0)));}
        r.endArray();return CompetitionDefinition.validate(definitions);
    }
    private static JsonObject object(JsonReader r,int depth)throws IOException {
        if(depth>2)throw new IOException("Nested competition object not allowed");var result=new JsonObject();r.beginObject();
        while(r.hasNext()){
            String key=r.nextName();if(result.has(key))throw new IOException("Duplicate competition field");
            switch(r.peek()){
                case BEGIN_OBJECT->result.add(key,object(r,depth+1));
                case BEGIN_ARRAY->{if(!key.equals("bands")||depth!=1)throw new IOException("Unexpected competition array");var values=new JsonArray();r.beginArray();while(r.hasNext()){if(values.size()>=3)throw new IOException("Too many prize bands");values.add(object(r,depth+1));}r.endArray();result.add(key,values);}
                case STRING->result.addProperty(key,r.nextString());case BOOLEAN->result.addProperty(key,r.nextBoolean());
                case NUMBER->{String value=r.nextString();if(!value.matches("[0-9]{1,10}"))throw new IOException("Competition integer required");result.addProperty(key,Long.parseLong(value));}
                default->throw new IOException("Invalid competition field type");
            }
        }
        r.endObject();return result;
    }
}
