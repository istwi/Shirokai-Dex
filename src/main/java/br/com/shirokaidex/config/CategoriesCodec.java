package br.com.shirokaidex.config;

import br.com.shirokaidex.categories.CategoryCatalog;
import br.com.shirokaidex.pokedex.CollectionDefinition.Criteria;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.*;
import java.util.*;

/** Strict candidate parser shared by standalone validation and transactional configuration. */
public final class CategoriesCodec {
    private CategoriesCodec() { }
    public static CategoryCatalog parse(String json) throws IOException {
        if (json.length() > 65536) throw new IOException("categories: maximum 65536 characters");
        try (JsonReader r = new JsonReader(new StringReader(json))) {
            r.setLenient(false);
            CategoryCatalog result = read(r);
            if (r.peek() != JsonToken.END_DOCUMENT) throw new IOException("trailing category data");
            return result;
        }
    }
    static CategoryCatalog read(JsonReader r) throws IOException {
        try {
            List<CategoryCatalog.Definition> entries = new ArrayList<>();
            r.beginArray();
            while (r.hasNext()) {
                if (entries.size() >= 64) throw new IOException("categories: maximum 64 entries");
                Set<String> fields = new HashSet<>(); String id = null; Criteria criteria = Criteria.all();
                Map<String, Set<String>> references = new HashMap<>();
                r.beginObject();
                while (r.hasNext()) {
                    String key = r.nextName();
                    if (!fields.add(key)) throw new IOException("categories " + r.getPath() + ": duplicate field " + key);
                    switch (key) {
                        case "id" -> id = string(r);
                        case "criteria" -> criteria = CollectionsCodec.criteria(r);
                        case "all_of", "any_of", "none_of" -> {
                            Set<String> refs = new HashSet<>(); r.beginArray();
                            while (r.hasNext()) {
                                if (refs.size() >= 64) throw new IOException("maximum 64 references");
                                if (!refs.add(string(r))) throw new IOException("duplicate category reference");
                            }
                            r.endArray(); references.put(key, refs);
                        }
                        default -> throw new IOException("unknown category field " + key);
                    }
                }
                r.endObject();
                entries.add(new CategoryCatalog.Definition(id, criteria, references.getOrDefault("all_of", Set.of()),
                        references.getOrDefault("any_of", Set.of()), references.getOrDefault("none_of", Set.of())));
            }
            r.endArray();
            return new CategoryCatalog(entries);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            throw new IOException("categories: " + failure.getMessage(), failure);
        }
    }
    private static String string(JsonReader r) throws IOException {
        if (r.peek() != JsonToken.STRING) throw new IOException("categories " + r.getPath() + ": expected string");
        return r.nextString();
    }
}
