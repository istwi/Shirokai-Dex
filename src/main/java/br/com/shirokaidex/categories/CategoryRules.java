package br.com.shirokaidex.categories;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Canonical rule content, independent of JSON ordering, timestamps and player knowledge. */
public record CategoryRules(int formatVersion, String json, String fingerprint) {
    public CategoryRules {
        if (formatVersion != 1 || json == null || !hash(json).equals(fingerprint))
            throw new IllegalArgumentException("Invalid category rule snapshot");
    }
    public static CategoryRules of(CategoryCatalog catalog) {
        JsonObject root = new JsonObject(); root.addProperty("format_version", 1);
        JsonArray categories = new JsonArray();
        catalog.definitions().values().stream().sorted(Comparator.comparing(CategoryCatalog.Definition::id)).forEach(d -> {
            JsonObject entry = new JsonObject(); entry.addProperty("id", d.id());
            JsonObject criteria = new JsonObject(); var c = d.criteria();
            criteria.add("include_species", array(c.includeSpecies())); criteria.add("exclude_species", array(c.excludeSpecies()));
            criteria.add("labels", array(c.labels())); criteria.add("types", array(c.types())); criteria.add("forms", array(c.forms()));
            entry.add("criteria", criteria); entry.add("all_of", array(d.allOf()));
            entry.add("any_of", array(d.anyOf())); entry.add("none_of", array(d.noneOf())); categories.add(entry);
        });
        root.add("categories", categories); String json = root.toString();
        return new CategoryRules(1, json, hash(json));
    }
    private static JsonArray array(Set<String> values) {
        JsonArray array = new JsonArray(); values.stream().sorted().forEach(array::add); return array;
    }
    private static String hash(String json) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
