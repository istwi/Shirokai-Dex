package br.com.shirokaidex.config;

import java.util.List;
import br.com.shirokaidex.pokedex.CollectionDefinition;
import br.com.shirokaidex.categories.CategoryCatalog;

public record Settings(int schemaVersion, String language, String menuTitle, List<CollectionDefinition> collections, CategoryCatalog categories, List<br.com.shirokaidex.rewards.RewardPlan> rewards, br.com.shirokaidex.menus.layout.HomeLayout home, br.com.shirokaidex.menus.layout.MenuNavigation menus,br.com.shirokaidex.rewards.CommandPolicy commandPolicy,java.time.ZoneId statisticsTimezone,br.com.shirokaidex.captures.CaptureRetention captureHistory,List<br.com.shirokaidex.rankings.CaptureRanking> rankings,List<br.com.shirokaidex.competitions.CompetitionDefinition> competitions,List<br.com.shirokaidex.achievements.AchievementDefinition> achievements,br.com.shirokaidex.profiles.StatisticsPrivacy statisticsPrivacy) {
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards,br.com.shirokaidex.menus.layout.HomeLayout home,br.com.shirokaidex.menus.layout.MenuNavigation menus,br.com.shirokaidex.rewards.CommandPolicy commandPolicy,java.time.ZoneId statisticsTimezone,br.com.shirokaidex.captures.CaptureRetention captureHistory,List<br.com.shirokaidex.rankings.CaptureRanking> rankings,List<br.com.shirokaidex.competitions.CompetitionDefinition> competitions){this(schemaVersion,language,menuTitle,collections,categories,rewards,home,menus,commandPolicy,statisticsTimezone,captureHistory,rankings,competitions,List.of(),br.com.shirokaidex.profiles.StatisticsPrivacy.defaultsPolicy());}
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards,br.com.shirokaidex.menus.layout.HomeLayout home,br.com.shirokaidex.menus.layout.MenuNavigation menus,br.com.shirokaidex.rewards.CommandPolicy commandPolicy,java.time.ZoneId statisticsTimezone,br.com.shirokaidex.captures.CaptureRetention captureHistory,List<br.com.shirokaidex.rankings.CaptureRanking> rankings){this(schemaVersion,language,menuTitle,collections,categories,rewards,home,menus,commandPolicy,statisticsTimezone,captureHistory,rankings,List.of());}
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards,br.com.shirokaidex.menus.layout.HomeLayout home,br.com.shirokaidex.menus.layout.MenuNavigation menus,br.com.shirokaidex.rewards.CommandPolicy commandPolicy,java.time.ZoneId statisticsTimezone,br.com.shirokaidex.captures.CaptureRetention captureHistory){this(schemaVersion,language,menuTitle,collections,categories,rewards,home,menus,commandPolicy,statisticsTimezone,captureHistory,br.com.shirokaidex.rankings.CaptureRanking.defaultsFor(collections));}
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards,br.com.shirokaidex.menus.layout.HomeLayout home,br.com.shirokaidex.menus.layout.MenuNavigation menus,br.com.shirokaidex.rewards.CommandPolicy commandPolicy,java.time.ZoneId statisticsTimezone){this(schemaVersion,language,menuTitle,collections,categories,rewards,home,menus,commandPolicy,statisticsTimezone,br.com.shirokaidex.captures.CaptureRetention.defaults());}
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards,br.com.shirokaidex.menus.layout.HomeLayout home,br.com.shirokaidex.menus.layout.MenuNavigation menus,br.com.shirokaidex.rewards.CommandPolicy commandPolicy){this(schemaVersion,language,menuTitle,collections,categories,rewards,home,menus,commandPolicy,java.time.ZoneId.of("UTC"));}
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards,br.com.shirokaidex.menus.layout.HomeLayout home,br.com.shirokaidex.menus.layout.MenuNavigation menus){this(schemaVersion,language,menuTitle,collections,categories,rewards,home,menus,br.com.shirokaidex.rewards.CommandPolicy.disabled());}
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards,br.com.shirokaidex.menus.layout.HomeLayout home){this(schemaVersion,language,menuTitle,collections,categories,rewards,home,br.com.shirokaidex.menus.layout.MenuNavigation.defaults());}
    public Settings(int schemaVersion,String language,String menuTitle,List<CollectionDefinition> collections,CategoryCatalog categories,List<br.com.shirokaidex.rewards.RewardPlan> rewards){this(schemaVersion,language,menuTitle,collections,categories,rewards,br.com.shirokaidex.menus.layout.HomeLayout.defaults());}
    public Settings {
        java.util.Objects.requireNonNull(statisticsTimezone);java.util.Objects.requireNonNull(captureHistory);
        achievements=br.com.shirokaidex.achievements.AchievementDefinition.validate(achievements);java.util.Objects.requireNonNull(statisticsPrivacy);
        competitions=br.com.shirokaidex.competitions.CompetitionDefinition.validate(competitions);
        for(var reward:rewards)if(reward.id().startsWith("cp_"))throw new IllegalArgumentException("Reward id prefix cp_ is reserved for competition awards");
        for(var competition:competitions)br.com.shirokaidex.competitions.CompetitionAwards.templates(competition.prizes(),rewards);
        for(var competition:competitions)if(competition.metric()==br.com.shirokaidex.rankings.CaptureRanking.Metric.CATEGORY&&!categories.definitions().containsKey(competition.key()))throw new IllegalArgumentException("Unknown competition category");
        rankings=br.com.shirokaidex.rankings.CaptureRanking.validate(rankings);
        collections = collections.stream().map(value -> value.bind(categories)).toList();
        rewards = List.copyOf(rewards);java.util.Objects.requireNonNull(commandPolicy);
        for(var reward:rewards)for(var command:reward.commands())if(commandPolicy.enabled()&&!commandPolicy.authorizes(command))throw new IllegalArgumentException("Command reward is not explicitly authorized");java.util.Objects.requireNonNull(home);java.util.Objects.requireNonNull(menus);
        var ids = collections.stream().map(CollectionDefinition::id).collect(java.util.stream.Collectors.toSet());
        for(var achievement:achievements)for(String collection:br.com.shirokaidex.achievements.Requirement.collections(achievement.requirement()))if(!ids.contains(collection))throw new IllegalArgumentException("Unknown achievement collection");
        for(var ranking:rankings)if(ranking.metric()==br.com.shirokaidex.rankings.CaptureRanking.Metric.COLLECTION&&!ids.contains(ranking.key()))throw new IllegalArgumentException("Unknown ranking collection");
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
