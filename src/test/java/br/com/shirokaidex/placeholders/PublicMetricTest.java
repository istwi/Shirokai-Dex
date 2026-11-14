package br.com.shirokaidex.placeholders;

import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.profiles.StatisticsPrivacy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class PublicMetricTest {
    @TempDir Path directory;
    @Test void privacyRevisionNeverServesOldPublicValuesAndLateCallbacksCannotRepopulateAfterLogout(){
        var cache=new PublicMetricCache();var key=new PublicMetricCache.Key(UUID.randomUUID(),"stats","total_captures");var settings=new Object();
        assertEquals("N/D",cache.read(key,settings,0,0));var first=cache.poll();assertNotNull(first);assertNull(cache.poll());cache.complete(first,1,"42");assertEquals("42",cache.read(key,settings,0,2));
        assertEquals("N/D",cache.read(key,settings,1,3));var changed=cache.poll();cache.remove(key.player());cache.complete(changed,4,"42");assertEquals("N/D",cache.read(key,settings,1,5));var next=cache.poll();cache.complete(next,6,"N/D");assertEquals("N/D",cache.read(key,settings,1,7));assertNull(cache.poll());
        cache.read(key,settings,1,TimeUnit.SECONDS.toNanos(6));var refreshing=cache.poll();cache.clear();cache.complete(refreshing,9,"42");assertEquals("N/D",cache.read(key,settings,1,10));
    }
    @Test void floodsAreBoundedPerPlayerAndOnlyFourQueriesCanRunConcurrently(){
        var cache=new PublicMetricCache();var player=UUID.randomUUID();var settings=new Object();for(int i=0;i<1000;i++)cache.read(new PublicMetricCache.Key(player,"rank","rank_"+i),settings,0,0);
        var jobs=new ArrayList<PublicMetricCache.Request>();for(int i=0;i<4;i++)jobs.add(cache.poll());assertTrue(jobs.stream().allMatch(Objects::nonNull));assertNull(cache.poll());int processed=0;while(!jobs.isEmpty()){var request=jobs.removeFirst();cache.complete(request,1,"1");processed++;var next=cache.poll();if(next!=null)jobs.add(next);}assertEquals(64,processed);
    }
    @Test void permissionFingerprintCannotReusePreviouslyAuthorizedMetric(){
        var cache=new PublicMetricCache();var player=UUID.randomUUID();var settings=new Object();var allowed=new PublicMetricCache.Key(player,"medals","count","national,restricted");var revoked=new PublicMetricCache.Key(player,"medals","count","national");
        cache.read(allowed,settings,0,0);cache.complete(cache.poll(),1,"5");assertEquals("5",cache.read(allowed,settings,0,2));assertEquals("N/D",cache.read(revoked,settings,0,3));
    }
    @Test void reloadDoesNotReusePriorConfigurationAndRefreshKeepsSameRevisionValue(){
        var cache=new PublicMetricCache();var key=new PublicMetricCache.Key(UUID.randomUUID(),"victories","");var a=new Object();var b=new Object();cache.read(key,a,0,0);cache.complete(cache.poll(),0,"3");assertEquals("3",cache.read(key,a,0,TimeUnit.SECONDS.toNanos(6)));assertNotNull(cache.poll());assertEquals("N/D",cache.read(key,b,0,TimeUnit.SECONDS.toNanos(6)));
    }
    @Test void nullViewerIsPublicNotSelfAndHiddenTotalsDoNotBecomeZero()throws Exception{
        var player=UUID.randomUUID();var now=Instant.parse("2026-11-14T00:00:00Z");
        try(var db=new Database(directory.resolve("public.sqlite"))){db.initialize().get(10,TimeUnit.SECONDS);db.recordLogin(player,"Public",now).get(10,TimeUnit.SECONDS);db.statisticsPrivacy(player,StatisticsPrivacy.Key.TOTAL_CAPTURES,false).get(10,TimeUnit.SECONDS);
            var publicData=db.profile(player.toString(),null,Settings.defaults(),Map.of(),Set.of(),List.of(),now).get(10,TimeUnit.SECONDS).data().orElseThrow();
            assertEquals("N/D",PublicMetricValues.format(publicData,"stats","total_captures"));assertEquals("N/D",PublicMetricValues.format(publicData,"stats","week"));assertEquals("0",PublicMetricValues.format(publicData,"claimed_rewards",""));assertEquals("0",PublicMetricValues.format(publicData,"medals","count"));assertEquals("N/D",PublicMetricValues.format(publicData,"rank","missing"));
        }
    }
    @Test void availableRewardsFollowUnlockAndJournalStateWithoutGrantingAnything()throws Exception {
        var player=UUID.randomUUID();var now=Instant.now();var plan=new br.com.shirokaidex.rewards.RewardPlan("first","national",1,null,List.of(new br.com.shirokaidex.rewards.RewardPlan.Item("minecraft:diamond",1)));
        try(var db=new Database(directory.resolve("available.sqlite"))){
            db.initialize().get(10,TimeUnit.SECONDS);assertEquals(0,db.publicRewardAvailability(player,List.of(plan),now).get(10,TimeUnit.SECONDS).count());
            db.rewardUnlocks(player,List.of(new br.com.shirokaidex.rewards.RewardUnlock(plan,1,100))).get(10,TimeUnit.SECONDS);assertEquals(1,db.publicRewardAvailability(player,List.of(plan),now).get(10,TimeUnit.SECONDS).count());
            var operation=db.reserveReward(player,plan).get(10,TimeUnit.SECONDS);assertEquals(1,db.publicRewardAvailability(player,List.of(plan),now).get(10,TimeUnit.SECONDS).count());
            db.beginReward(operation.id()).get(10,TimeUnit.SECONDS);assertEquals(0,db.publicRewardAvailability(player,List.of(plan),now).get(10,TimeUnit.SECONDS).count());assertEquals(0,db.publicRewardAvailability(player,List.of(),now).get(10,TimeUnit.SECONDS).count());
        }
    }
    @Test void favoritePlaceholderUsesHumanSlotsOneThroughThree(){
        var definition=new br.com.shirokaidex.achievements.AchievementDefinition("badge","First","","minecraft:paper",false,"",br.com.shirokaidex.achievements.AchievementDefinition.Policy.PERMANENT,true,true,true,Set.of(),new br.com.shirokaidex.achievements.Requirement.Count(br.com.shirokaidex.rankings.CaptureRanking.Metric.TOTAL,"",1));
        var medal=new br.com.shirokaidex.achievements.AchievementStore.Medal(definition,true,Instant.EPOCH,0);
        var data=new br.com.shirokaidex.profiles.ProfileStore.Data(new br.com.shirokaidex.profiles.ProfileStore.Identity(UUID.randomUUID(),"Player"),Map.of(),Set.of(),null,null,null,List.of(),Map.of(),Map.of(),List.of(),Map.of(),List.of(medal),0,0,java.time.ZoneId.of("UTC"));
        assertEquals("First",PublicMetricValues.format(data,"medals","1"));assertEquals("",PublicMetricValues.format(data,"medals","2"));assertEquals("",PublicMetricValues.format(data,"medals","3"));
    }
    @Test void customAliasesRejectRecursiveMetricsExecutableFieldsAndUnknownSources()throws Exception {
        String prefix="{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"custom_placeholders\":";
        var parsed=br.com.shirokaidex.config.ConfigManager.parse(prefix+"""
          [{"id":"captures_label","metric":"stats","argument":"total_captures","template":"Capturas: {value}"},
           {"id":"national_label","metric":"percent","template":"Dex: {value}"}]}
          """);
        assertEquals("Capturas: 2",parsed.customPlaceholders().getFirst().render("2"));assertEquals("N/D",parsed.customPlaceholders().getFirst().render("N/D"));
        for(String invalid:List.of(
            "[{\"id\":\"a\",\"metric\":\"custom\",\"argument\":\"a\"}]",
            "[{\"id\":\"a\",\"metric\":\"percent\",\"argument\":\"missing\"}]",
            "[{\"id\":\"a\",\"metric\":\"rank\",\"argument\":\"missing\"}]",
            "[{\"id\":\"a\",\"metric\":\"victories\",\"command\":\"op user\"}]",
            "[{\"id\":\"a\",\"metric\":\"victories\",\"template\":\"%external:secret% {value}\"}]",
            "[{\"id\":\"a\",\"metric\":\"victories\",\"template\":\"{unknown} {value}\"}]",
            "[{\"id\":\"a\",\"metric\":\"victories\",\"metric\":\"stats\"}]"))assertThrows(java.io.IOException.class,()->br.com.shirokaidex.config.ConfigManager.parse(prefix+invalid+"}"));
    }
    @Test void argumentsAreStrictAndDoNotInterpretTemplatesOrCommands(){
        assertNull(PublicMetricValues.argument("rank","foo bar"));assertNull(PublicMetricValues.argument("stats","%other:secret%"));assertNull(PublicMetricValues.argument("medals","4"));assertNull(PublicMetricValues.argument("victories","target"));assertEquals("",PublicMetricValues.argument("victories",null));assertEquals("shiny_captures",PublicMetricValues.argument("stats"," shiny_captures "));
    }
}
