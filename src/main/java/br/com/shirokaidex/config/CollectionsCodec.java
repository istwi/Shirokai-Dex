package br.com.shirokaidex.config;

import br.com.shirokaidex.pokedex.CollectionDefinition;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.util.*;

final class CollectionsCodec {
    private CollectionsCodec() { }
    static List<CollectionDefinition> read(JsonReader reader) throws IOException {
        List<CollectionDefinition> values = new ArrayList<>(); Set<String> ids = new HashSet<>();
        reader.beginArray();
        while (reader.hasNext()) {
            if (values.size() >= 64) throw error(reader, "maximum 64 collections");
            CollectionDefinition value = collection(reader);
            if (!ids.add(value.id())) throw error(reader, "duplicate collection id " + value.id());
            values.add(value);
        }
        reader.endArray();
        if (values.isEmpty()) throw error(reader, "at least one collection is required");
        return List.copyOf(values);
    }
    private static CollectionDefinition collection(JsonReader r) throws IOException {
        Set<String> keys = new HashSet<>(); String id = null, name = null, description = "", icon = null, permission = "", category = null;
        CollectionDefinition.Mode mode = null; CollectionDefinition.Criteria criteria = null;
        String path = r.getPath();
        r.beginObject();
        while (r.hasNext()) {
            String field = r.nextName(); unique(r, keys, field);
            switch (field) {
                case "id" -> id = string(r);
                case "name" -> name = string(r);
                case "description" -> description = string(r);
                case "icon" -> icon = string(r);
                case "permission" -> permission = string(r);
                case "category" -> category = string(r);
                case "progress_mode" -> {
                    String value = string(r);
                    try { mode = CollectionDefinition.Mode.valueOf(value); }
                    catch (IllegalArgumentException failure) { throw error(r, "unsupported progress_mode " + value); }
                }
                case "criteria" -> criteria = criteria(r);
                default -> throw error(r, "unknown collection field " + field);
            }
        }
        r.endObject();
        String identity = id;
        var builtin = CollectionDefinition.defaults().stream().filter(value -> value.id().equals(identity)).findFirst();
        if (builtin.isPresent()) {
            var defaults = builtin.get();
            if (name == null) name = defaults.name(); if (icon == null) icon = defaults.icon();
            if (mode == null) mode = defaults.mode(); if (criteria == null) criteria = defaults.criteria();
        } else {
            if (icon == null) icon = "minecraft:book";
            if (mode == null) mode = CollectionDefinition.Mode.OWNED;
            if (criteria == null) criteria = CollectionDefinition.Criteria.all();
        }
        try { return new CollectionDefinition(id, name, description, icon, mode, criteria, permission,
                category == null ? null : new CollectionDefinition.CategorySelection(category, null)); }
        catch (IllegalArgumentException | NullPointerException failure) { throw new IOException("config.json " + path + ": " + failure.getMessage(), failure); }
    }
    static CollectionDefinition.Criteria criteria(JsonReader r) throws IOException {
        Set<String> fields = new HashSet<>(); Map<String, Set<String>> values = new HashMap<>();
        String path = r.getPath(); r.beginObject();
        while (r.hasNext()) {
            String key = r.nextName(); unique(r, fields, key);
            if (!Set.of("include_species", "exclude_species", "labels", "types", "forms").contains(key)) throw error(r, "unknown criterion " + key);
            values.put(key, strings(r));
        }
        r.endObject();
        try {
            return new CollectionDefinition.Criteria(values.getOrDefault("include_species", Set.of()), values.getOrDefault("exclude_species", Set.of()),
                    values.getOrDefault("labels", Set.of()), values.getOrDefault("types", Set.of()), values.getOrDefault("forms", Set.of()));
        } catch (IllegalArgumentException failure) { throw new IOException("config.json " + path + ": " + failure.getMessage(), failure); }
    }
    private static Set<String> strings(JsonReader r) throws IOException {
        Set<String> values = new HashSet<>(); r.beginArray();
        while (r.hasNext()) {
            if (values.size() >= 2048) throw error(r, "maximum 2048 values per criterion");
            String value = string(r);
            if (!values.add(value)) throw error(r, "duplicate criterion value " + value);
        }
        r.endArray(); return Set.copyOf(values);
    }
    private static String string(JsonReader r) throws IOException {
        if (r.peek() != JsonToken.STRING) throw error(r, "expected string");
        return r.nextString();
    }
    private static void unique(JsonReader r, Set<String> keys, String field) throws IOException {
        if (!keys.add(field)) throw error(r, "duplicate field " + field);
    }
    private static IOException error(JsonReader r, String message) { return new IOException("config.json " + r.getPath() + ": " + message); }
}
