package br.com.shirokaidex.localization;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.minecraft.network.chat.Component;

public final class Messages {
    private final Map<String, Map<String, String>> languages;
    public Messages() { languages = Map.of("pt_br", load("pt_br"), "en_us", load("en_us")); }
    private static Map<String, String> load(String language) {
        try (var input = Messages.class.getResourceAsStream("/localization/" + language + ".json")) {
            if (input == null) throw new IOException("Missing language: " + language);
            try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Map<String, String> values = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                return Map.copyOf(values);
            }
        } catch (IOException failure) { throw new IllegalStateException("Bundled language could not be loaded", failure); }
    }
    public Component text(String language, String key, Object... arguments) {
        String value = languages.getOrDefault(language, languages.get("pt_br")).get(key);
        if (value == null) throw new IllegalArgumentException("Missing message key: " + key);
        for (int i = 0; i < arguments.length; i++) value = value.replace("{" + i + "}", String.valueOf(arguments[i]));
        return Component.literal(value);
    }
}
