package br.com.shirokaidex.profiles;

import br.com.shirokaidex.achievements.AchievementStore;
import br.com.shirokaidex.captures.*;
import br.com.shirokaidex.rankings.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Profiles expose only authorized immutable fields. Official Pokedex writes are never performed. */
public final class ProfileStore {
    private ProfileStore() { }
    public record Identity(UUID player,String name) { }
    public record Data(Identity identity,Map<StatisticsPrivacy.Key,Long> statistics,Set<StatisticsPrivacy.Key> visibleStatistics,Long today,Long week,Long month,
            List<CaptureRecords.Best> records,Map<String,Long> species,Map<String,Long> categories,
            List<ProfileProgress.Proof> collections,Map<String,Long> rankingPositions,List<AchievementStore.Medal> medals,
            long victories,long claimedRewards,ZoneId timezone) {
        public Data {statistics=Map.copyOf(statistics);visibleStatistics=Set.copyOf(visibleStatistics);records=List.copyOf(records);species=Map.copyOf(species);categories=Map.copyOf(categories);collections=List.copyOf(collections);rankingPositions=Map.copyOf(rankingPositions);medals=List.copyOf(medals);}
    }
    public record Preferences(String language,boolean notifications) { }
    public static Optional<Identity> resolve(Connection c,String input)throws SQLException {
        if(input==null)return Optional.empty();boolean uuid=input.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");if(!uuid&&!input.matches("[A-Za-z0-9_]{1,16}"))return Optional.empty();
        var found=new ArrayList<Identity>();try(var q=c.prepareStatement(uuid?"SELECT uuid,last_known_name FROM players WHERE uuid=?":"SELECT uuid,last_known_name FROM players WHERE last_known_name=? COLLATE NOCASE LIMIT 2")){q.setString(1,uuid?UUID.fromString(input).toString():input);try(var rows=q.executeQuery()){while(rows.next())found.add(new Identity(UUID.fromString(rows.getString(1)),rows.getString(2)));}}return found.size()==1?Optional.of(found.getFirst()):Optional.empty();
    }
    public static Optional<Data> view(Connection c,String input,UUID viewer,StatisticsPrivacy policy,Map<String,String> versions,Set<String> visibleCollections,Map<String,br.com.shirokaidex.pokedex.CollectionDefinition.Mode> modes,List<br.com.shirokaidex.achievements.AchievementDefinition> achievementDefinitions,List<CaptureRanking> visibleRankings,Instant now,ZoneId zone)throws SQLException {
        var identity=resolve(c,input);if(identity.isEmpty())return Optional.empty();UUID player=identity.get().player();boolean own=player.equals(viewer);var preferences=StatisticsPrivacy.preferences(c,player);var visibility=EnumSet.noneOf(StatisticsPrivacy.Key.class);for(var key:StatisticsPrivacy.Key.values())if(own||policy.visible(key,preferences))visibility.add(key);var summary=CaptureLedger.summary(c,player,now,zone);var statistics=new EnumMap<StatisticsPrivacy.Key,Long>(StatisticsPrivacy.Key.class);
        boolean total=own||policy.visible(StatisticsPrivacy.Key.TOTAL_CAPTURES,preferences),shiny=own||policy.visible(StatisticsPrivacy.Key.SHINY_CAPTURES,preferences),legendary=own||policy.visible(StatisticsPrivacy.Key.LEGENDARY_CAPTURES,preferences),records=own||policy.visible(StatisticsPrivacy.Key.PERSONAL_RECORDS,preferences);
        if(total)statistics.put(StatisticsPrivacy.Key.TOTAL_CAPTURES,summary.total());if(shiny)statistics.put(StatisticsPrivacy.Key.SHINY_CAPTURES,summary.shiny());if(legendary)statistics.put(StatisticsPrivacy.Key.LEGENDARY_CAPTURES,summary.legendary());
        var counts=new HashMap<String,Map<String,Long>>();counts.put("SPECIES",new TreeMap<>());counts.put("CATEGORY",new TreeMap<>());
        boolean species=own||policy.visible(StatisticsPrivacy.Key.SPECIES_CAPTURES,preferences),categories=own||policy.visible(StatisticsPrivacy.Key.CATEGORY_CAPTURES,preferences);
        if(species||categories)try(var q=c.prepareStatement("SELECT kind,entry_key,total FROM capture_counts WHERE player_uuid=?")){q.setString(1,player.toString());try(var rows=q.executeQuery()){while(rows.next()){String kind=rows.getString(1);if(kind.equals("SPECIES")&&species||kind.equals("CATEGORY")&&categories)counts.get(kind).put(rows.getString(2),rows.getLong(3));}}}
        var progress=ProfileProgress.read(c,player).stream().filter(proof->visibleCollections.contains(proof.collection())&&proof.version().equals(versions.get(proof.collection()))&&(own||modes.get(proof.collection())!=br.com.shirokaidex.pokedex.CollectionDefinition.Mode.SHINY_CAPTURED||policy.visible(StatisticsPrivacy.Key.SHINY_CAPTURES,preferences))).toList();
        var currentMedals=achievementDefinitions.stream().collect(java.util.stream.Collectors.toMap(br.com.shirokaidex.achievements.AchievementDefinition::id,d->d));
        var medalVisibility=new HashMap<String,Boolean>();for(var definition:achievementDefinitions)medalVisibility.put(definition.id(),definition.publicMedal()&&definition.dependencies().stream().allMatch(id->medalVisibility.getOrDefault(id,false))&&br.com.shirokaidex.achievements.Requirement.publiclyVisible(definition.requirement(),policy,preferences,modes,medalVisibility));
        var medals=AchievementStore.medals(c,player).stream().map(medal->new AchievementStore.Medal(currentMedals.getOrDefault(medal.definition().id(),medal.definition()),medal.active(),medal.unlockedAt(),medal.favoriteSlot())).filter(medal->medal.active()&&(own||medal.definition().publicMedal()&&br.com.shirokaidex.achievements.Requirement.publiclyVisible(medal.definition().requirement(),policy,preferences,modes,medalVisibility)&&medal.definition().dependencies().stream().allMatch(id->medalVisibility.getOrDefault(id,false)))).toList();var positions=new LinkedHashMap<String,Long>();
        for(var ranking:visibleRankings){Long position=CaptureRankings.position(c,ranking,player,policy,versions.get(ranking.key()),modes.get(ranking.key())==br.com.shirokaidex.pokedex.CollectionDefinition.Mode.SHINY_CAPTURED?StatisticsPrivacy.Key.SHINY_CAPTURES:null);if(position!=null)positions.put(ranking.id(),position);}
        long victories=number(c,"SELECT count(*) FROM competition_results WHERE player_uuid=? AND position=1 AND edition_id IN (SELECT edition_id FROM competition_editions WHERE state='FINAL')",player);
        long claimed=number(c,"SELECT count(*) FROM reward_operations WHERE player_uuid=? AND state='COMPLETED' AND coalesce(json_extract(plan_json,'$.source'),'')<>'ADMIN_ITEM'",player);
        return Optional.of(new Data(identity.get(),statistics,visibility,total?summary.today():null,total?summary.week():null,total?summary.month():null,records?CaptureRecords.best(c,player,zone):List.of(),counts.get("SPECIES"),counts.get("CATEGORY"),progress,positions,medals,victories,claimed,zone));
    }
    private static long number(Connection c,String sql,UUID player)throws SQLException {try(var q=c.prepareStatement(sql)){q.setString(1,player.toString());try(var rows=q.executeQuery()){rows.next();return rows.getLong(1);}}}
    public static Preferences preferences(Connection c,UUID player)throws SQLException {
        try(var q=c.prepareStatement("SELECT language,notifications FROM player_preferences WHERE player_uuid=?")){q.setString(1,player.toString());try(var rows=q.executeQuery()){return rows.next()?new Preferences(rows.getString(1),rows.getBoolean(2)):new Preferences(null,true);}}
    }
    public static void notifications(Connection c,UUID player,boolean enabled)throws SQLException {
        try(var q=c.prepareStatement("INSERT INTO player_preferences(player_uuid,notifications) VALUES(?,?) ON CONFLICT(player_uuid) DO UPDATE SET notifications=excluded.notifications")){q.setString(1,player.toString());q.setBoolean(2,enabled);q.executeUpdate();}
    }
}
