package br.com.shirokaidex.config;

import br.com.shirokaidex.achievements.*;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.util.*;

final class AchievementsCodec {
    private AchievementsCodec() { }
    static List<AchievementDefinition> read(JsonReader reader)throws IOException {
        var definitions=new ArrayList<AchievementDefinition>();reader.beginArray();while(reader.hasNext()){if(definitions.size()>=128)throw new IOException("Too many achievements");definitions.add(AchievementJson.read(StrictJson.object(reader)));}reader.endArray();return AchievementDefinition.validate(definitions);
    }
}
