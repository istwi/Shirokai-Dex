package br.com.shirokaidex.config;

import br.com.shirokaidex.rankings.CaptureRanking;
import com.google.gson.stream.*;
import java.io.IOException;
import java.util.*;

final class RankingsCodec {
    private RankingsCodec() { }
    static List<CaptureRanking> read(JsonReader reader)throws IOException {
        var result=new ArrayList<CaptureRanking>();reader.beginArray();
        while(reader.hasNext()){
            if(result.size()>=128)throw new IOException("Too many rankings");
            var values=new HashMap<String,String>();reader.beginObject();
            while(reader.hasNext()){
                String key=reader.nextName();if(!Set.of("id","name","metric","key","permission").contains(key)||values.containsKey(key)||reader.peek()!=JsonToken.STRING)throw new IOException("Invalid ranking field");
                values.put(key,reader.nextString());
            }
            reader.endObject();
            if(!values.keySet().containsAll(Set.of("id","name","metric")))throw new IOException("Ranking requires id, name and metric");
            result.add(new CaptureRanking(values.get("id"),values.get("name"),CaptureRanking.Metric.valueOf(values.get("metric")),values.getOrDefault("key",""),values.getOrDefault("permission","")));
        }
        reader.endArray();return CaptureRanking.validate(result);
    }
}
