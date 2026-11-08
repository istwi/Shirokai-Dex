package br.com.shirokaidex.config;

import java.util.List;
import br.com.shirokaidex.pokedex.CollectionDefinition;
import br.com.shirokaidex.categories.CategoryCatalog;

public record Settings(int schemaVersion, String language, String menuTitle, List<CollectionDefinition> collections, CategoryCatalog categories, List<br.com.shirokaidex.rewards.RewardPlan> rewards, br.com.shirokaidex.menus.layout.HomeLayout home, br.com.shirokaidex.menus.layout.MenuNavigation menus,br.com.shirokaidex.rewards.CommandPolicy commandPolicy,java.time.ZoneId statisticsTimezone) {
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards,br.com.shirokaidex.menus.layout.HomeLayout home,br.com.shirokaidex.menus.layout.MenuNavigation menus,br.com.shirokaidex.rewards.CommandPolicy commandPolicy){this(schemaVersion,language,menuTitle,collections,categories,rewards,home,menus,commandPolicy,java.time.ZoneId.of("UTC"));}
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards,br.com.shirokaidex.menus.layout.HomeLayout home,br.com.shirokaidex.menus.layout.MenuNavigation menus){this(schemaVersion,language,menuTitle,collections,categories,rewards,home,menus,br.com.shirokaidex.rewards.CommandPolicy.disabled());}
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards,br.com.shirokaidex.menus.layout.HomeLayout home){this(schemaVersion,language,menuTitle,collections,categories,rewards,home,br.com.shirokaidex.menus.layout.MenuNavigation.defaults());}
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards){this(schemaVersion,language,menuTitle,collections,categories,rewards,br.com.shirokaidex.menus.layout.HomeLayout.defaults());}
    public Settings {
        java.util.Objects.requireNonNull(statisticsTimezone);
        collections = collections.stream().map(value -> value.bind(categories)).toList();
        rewards = List.copyOf(rewards);java.util.Objects.requireNonNull(commandPolicy);
        for(var reward:rewards)for(var command:reward.commands())if(commandPolicy.enabled()&&!commandPolicy.authorizes(command))throw new IllegalArgumentException("Command reward is not explicitly authorized");java.util.Objects.requireNonNull(home);java.util.Objects.requireNonNull(menus);
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
