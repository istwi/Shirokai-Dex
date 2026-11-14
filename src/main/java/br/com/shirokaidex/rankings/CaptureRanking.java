package br.com.shirokaidex.rankings;

import java.util.*;

public record CaptureRanking(String id, String name, Metric metric, String key, String permission) {
    public enum Metric { TOTAL, SHINY, LEGENDARY, MYTHICAL, SPECIES, CATEGORY, COLLECTION }
    public CaptureRanking {
        if (id == null || !id.matches("[a-z][a-z0-9_]{0,47}") || name == null || name.isBlank() || name.length()>96 || name.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid ranking identity");
        Objects.requireNonNull(metric); Objects.requireNonNull(key); Objects.requireNonNull(permission);
        if (metric == Metric.SPECIES) br.com.shirokaidex.pokedex.CollectionDefinition.identifier(key);
        else if (metric == Metric.CATEGORY || metric == Metric.COLLECTION) { if (!key.matches("[a-z][a-z0-9_]{0,47}")) throw new IllegalArgumentException("Invalid ranking category"); }
        else if (!key.isEmpty()) throw new IllegalArgumentException("Ranking metric does not accept key");
        if (!permission.isEmpty() && !permission.matches("[a-z0-9_.-]{1,128}")) throw new IllegalArgumentException("Invalid ranking permission");
    }
    public static List<CaptureRanking> defaults() { return List.of(new CaptureRanking("captures","Capturas",Metric.TOTAL,"",""),new CaptureRanking("shiny","Shiny",Metric.SHINY,"",""),new CaptureRanking("legendary","Lendários",Metric.LEGENDARY,"","")); }
    public static List<CaptureRanking> defaultsFor(List<br.com.shirokaidex.pokedex.CollectionDefinition> collections){var result=new ArrayList<>(defaults());for(var collection:collections){String id="dex_"+collection.id();if(id.length()>48){try{id="dex_"+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(collection.id().getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0,40);}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}String name=collection.name().isBlank()?"Pokédex "+collection.id():collection.name();result.add(new CaptureRanking(id,name,Metric.COLLECTION,collection.id(),collection.permission()));}return validate(result);}
    public static List<CaptureRanking> validate(List<CaptureRanking> definitions) {
        if(definitions.size()>128) throw new IllegalArgumentException("Too many rankings");
        var ids=new HashSet<String>(); for(var definition:definitions) if(!ids.add(definition.id()))throw new IllegalArgumentException("Duplicate ranking id");
        return List.copyOf(definitions);
    }
}
