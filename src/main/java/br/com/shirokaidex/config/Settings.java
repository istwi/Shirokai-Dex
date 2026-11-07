package br.com.shirokaidex.config;

import java.util.List;
import br.com.shirokaidex.pokedex.CollectionDefinition;

public record Settings(int schemaVersion, String language, String menuTitle, List<CollectionDefinition> collections) {
    public Settings { collections = List.copyOf(collections); }
    public Settings(int schemaVersion, String language, String menuTitle) { this(schemaVersion, language, menuTitle, CollectionDefinition.defaults()); }
    public static Settings defaults() {
        return new Settings(1, "pt_br", "✦ Shirokai Dex");
    }
}
