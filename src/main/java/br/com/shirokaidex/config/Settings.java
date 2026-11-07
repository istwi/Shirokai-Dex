package br.com.shirokaidex.config;

import java.util.List;
import br.com.shirokaidex.pokedex.CollectionDefinition;
import br.com.shirokaidex.categories.CategoryCatalog;

public record Settings(int schemaVersion, String language, String menuTitle, List<CollectionDefinition> collections, CategoryCatalog categories, List<br.com.shirokaidex.rewards.RewardPlan> rewards, br.com.shirokaidex.menus.layout.HomeLayout home) {
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards){this(schemaVersion,language,menuTitle,collections,categories,rewards,br.com.shirokaidex.menus.layout.HomeLayout.defaults());}
    public Settings {
        collections = collections.stream().map(value -> value.bind(categories)).toList();
        rewards = List.copyOf(rewards);java.util.Objects.requireNonNull(home);
        var ids = collections.stream().map(CollectionDefinition::id).collect(java.util.stream.Collectors.toSet());
        for (var reward : rewards) if (!ids.contains(reward.collection())) throw new IllegalArgumentException("Unknown collection for reward " + reward.id());
    }
    public Settings(int schemaVersion, String language, String menuTitle, List<CollectionDefinition> collections, CategoryCatalog categories) {
        this(schemaVersion, language, menuTitle, collections, categories, List.of());
    }
    public Settings(int schemaVersion, String language, String menuTitle, List<CollectionDefinition> collections) {
        this(schemaVersion, language, menuTitle, collections, new CategoryCatalog(List.of()));
    }
    public Settings(int schemaVersion, String language, String menuTitle) { this(schemaVersion, language, menuTitle, CollectionDefinition.defaults()); }
    public static Settings defaults() {
        return new Settings(1, "pt_br", "✦ Shirokai Dex");
    }
}
