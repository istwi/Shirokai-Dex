package br.com.shirokaidex.config;

import br.com.shirokaidex.notifications.NotificationPolicy;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.util.*;

final class NotificationsCodec {
    private NotificationsCodec() { }
    static NotificationPolicy read(JsonReader reader)throws IOException {
        var root=StrictJson.object(reader);var rules=new EnumMap<NotificationPolicy.Event,NotificationPolicy.Rule>(NotificationPolicy.Event.class);rules.putAll(NotificationPolicy.defaults().rules());
        for(var field:root.entrySet()){
            var event=NotificationPolicy.Event.valueOf(field.getKey().toUpperCase(Locale.ROOT));var object=field.getValue().getAsJsonObject();
            if(!Set.of("enabled","channels","global","sound").containsAll(object.keySet()))throw new IOException("Unknown notification field");
            var old=rules.get(event);boolean enabled=bool(object,"enabled",old.enabled()),global=bool(object,"global",old.global());String sound=old.sound();
            if(object.has("sound")){var value=object.get("sound");if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw new IOException("Sound must be an identifier");sound=value.getAsString();}
            Set<NotificationPolicy.Channel> channels=old.channels();
            if(object.has("channels")){var parsed=EnumSet.noneOf(NotificationPolicy.Channel.class);for(var value:object.getAsJsonArray("channels")){if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()||!parsed.add(NotificationPolicy.Channel.valueOf(value.getAsString())))throw new IOException("Invalid/duplicate channel");}channels=parsed;}
            rules.put(event,new NotificationPolicy.Rule(enabled,channels,global,sound));
        }return new NotificationPolicy(rules);
    }
    private static boolean bool(JsonObject object,String key,boolean fallback)throws IOException {if(!object.has(key))return fallback;var value=object.get(key);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw new IOException("Notification toggle must be boolean");return value.getAsBoolean();}
}
