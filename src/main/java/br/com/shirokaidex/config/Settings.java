package br.com.shirokaidex.config;

import java.util.List;
import br.com.shirokaidex.pokedex.CollectionDefinition;
import br.com.shirokaidex.categories.CategoryCatalog;

public record Settings(int schemaVersion, String language, String menuTitle, List<CollectionDefinition> collections, CategoryCatalog categories) {
    public Settings { collections = collections.stream().map(value -> value.bind(categories)).toList(); }
    public Settings(int schemaVersion, String language, String menuTitle, List<CollectionDefinition> collections) {
        this(schemaVersion, language, menuTitle, collections, new CategoryCatalog(List.of()));
    }
    public Settings(int schemaVersion, String language, String menuTitle) { this(schemaVersion, language, menuTitle, CollectionDefinition.defaults()); }
    public static Settings defaults() {
        return new Settings(1, "pt_br", "✦ Shirokai Dex");
    }
}
