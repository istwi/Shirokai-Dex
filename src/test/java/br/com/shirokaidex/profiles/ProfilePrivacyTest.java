package br.com.shirokaidex.profiles;

import br.com.shirokaidex.config.*;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.captures.*;
import br.com.shirokaidex.rankings.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfilePrivacyTest {
    @TempDir Path directory;
    private static final Instant NOW=Instant.parse("2026-11-14T00:00:00Z");
    private static <T>T await(CompletableFuture<T> f)throws Exception{return f.get(10,TimeUnit.SECONDS);}
    private static CaptureRecord capture(UUID player){return new CaptureRecord(UUID.randomUUID(),UUID.randomUUID(),player,"mod:a","normal",true,Set.of("legendary"),Set.of(),Set.of("custom"),"a".repeat(64),NOW,"minecraft:overworld",0,64,0,CaptureRecord.Origin.NATIVE_PLAYER_SPAWN);}
    @Test void defaultsHideSpeciesPubliclyOwnViewRemainsCompleteAndOverridesInvalidateRankCache()throws Exception{
        var player=UUID.randomUUID();var viewer=UUID.randomUUID();var settings=Settings.defaults();
        try(var db=new Database(directory.resolve("privacy.sqlite"))){await(db.initialize());await(db.recordLogin(player,"PrivateTrainer",NOW));await(db.recordCapture(capture(player)));
            var publicView=await(db.profile("PrivateTrainer",viewer,settings,Map.of(),Set.of(),CaptureRanking.defaults(),NOW)).data().orElseThrow();assertEquals(1,publicView.statistics().get(StatisticsPrivacy.Key.TOTAL_CAPTURES));assertTrue(publicView.species().isEmpty());assertFalse(publicView.visibleStatistics().contains(StatisticsPrivacy.Key.SPECIES_CAPTURES));assertEquals(1,publicView.categories().get("custom"));
            var own=await(db.profile(player.toString(),player,settings,Map.of(),Set.of(),CaptureRanking.defaults(),NOW)).data().orElseThrow();assertEquals(1,own.species().get("mod:a"));
            var ranking=new CaptureRanking("totals","Totals",CaptureRanking.Metric.TOTAL,"","");assertEquals(1,await(db.captureRanking(ranking,0)).entries().size());long revision=db.privacyRevision();await(db.statisticsPrivacy(player,StatisticsPrivacy.Key.TOTAL_CAPTURES,false));assertTrue(db.privacyRevision()>revision);assertTrue(await(db.captureRanking(ranking,0)).entries().isEmpty());
            publicView=await(db.profile(player.toString(),viewer,settings,Map.of(),Set.of(),CaptureRanking.defaults(),NOW)).data().orElseThrow();assertFalse(publicView.statistics().containsKey(StatisticsPrivacy.Key.TOTAL_CAPTURES));assertNull(publicView.today());assertFalse(publicView.rankingPositions().containsKey("captures"));
            await(db.statisticsPrivacy(player,StatisticsPrivacy.Key.SPECIES_CAPTURES,true));publicView=await(db.profile(player.toString(),viewer,settings,Map.of(),Set.of(),List.of(),NOW)).data().orElseThrow();assertEquals(1,publicView.species().get("mod:a"));
        }
    }
    @Test void offlineIdentityAmbiguityPreferencesAndPublicPolicySurviveRestart()throws Exception{
        var file=directory.resolve("offline.sqlite");var a=UUID.randomUUID();var b=UUID.randomUUID();var settings=Settings.defaults();
        try(var db=new Database(file)){await(db.initialize());await(db.recordLogin(a,"Trainer",NOW));await(db.recordCapture(capture(a)));await(db.statisticsPrivacy(a,StatisticsPrivacy.Key.SHINY_CAPTURES,false));await(db.notificationPreference(a,false));}
        try(var db=new Database(file)){await(db.initialize());assertFalse(await(db.preferences(a)).notifications());var data=await(db.profile("trainer",b,settings,Map.of(),Set.of(),List.of(),NOW)).data().orElseThrow();assertEquals(a,data.identity().player());assertFalse(data.statistics().containsKey(StatisticsPrivacy.Key.SHINY_CAPTURES));await(db.recordLogin(b,"Trainer",NOW));assertTrue(await(db.profile("Trainer",a,settings,Map.of(),Set.of(),List.of(),NOW)).data().isEmpty());assertTrue(await(db.profile(a.toString(),b,settings,Map.of(),Set.of(),List.of(),NOW)).data().isPresent());assertTrue(await(db.profile("bad player name",a,settings,Map.of(),Set.of(),List.of(),NOW)).data().isEmpty());}
    }
    @Test void disabledOverridesHonorConfiguredDefaultsAndMalformedPoliciesAreRejected()throws Exception{
        String base="{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\"";
        var settings=ConfigManager.parse(base+",\"statistics_privacy\":{\"enabled\":true,\"allow_player_override\":false,\"defaults\":{\"total_captures\":false}}}");assertFalse(settings.statisticsPrivacy().visible(StatisticsPrivacy.Key.TOTAL_CAPTURES,Map.of(StatisticsPrivacy.Key.TOTAL_CAPTURES,true)));assertFalse(settings.statisticsPrivacy().visible(StatisticsPrivacy.Key.SPECIES_CAPTURES,Map.of()));
        for(String policy:List.of("{\"enabled\":\"true\"}","{\"defaults\":{\"total_captures\":1}}","{\"unknown\":true}","{\"defaults\":{\"fake\":true}}","{\"defaults\":{\"total_captures\":true,\"total_captures\":false}}"))assertThrows(java.io.IOException.class,()->ConfigManager.parse(base+",\"statistics_privacy\":"+policy+"}"));
    }
    @Test void verifiedShinyCollectionRespectsShinyPrivacyInPublicProfilesAndRankings()throws Exception{
        var player=UUID.randomUUID();var viewer=UUID.randomUUID();var settings=Settings.defaults();String version="a".repeat(64);var ranking=new CaptureRanking("dex_shiny","Shiny",CaptureRanking.Metric.COLLECTION,"shiny","");
        try(var db=new Database(directory.resolve("shiny-private.sqlite"))){await(db.initialize());await(db.recordLogin(player,"ShinyTrainer",NOW));await(db.observeProfile(player,List.of(new ProfileProgress.Proof("shiny",version,1,100,NOW)),settings,NOW));await(db.statisticsPrivacy(player,StatisticsPrivacy.Key.SHINY_CAPTURES,false));assertTrue(await(db.captureRanking(ranking,0,settings.statisticsPrivacy(),version,StatisticsPrivacy.Key.SHINY_CAPTURES)).entries().isEmpty());assertTrue(await(db.profile(player.toString(),viewer,settings,Map.of("shiny",version),Set.of("shiny"),List.of(ranking),NOW)).data().orElseThrow().collections().isEmpty());assertEquals(1,await(db.profile(player.toString(),player,settings,Map.of("shiny",version),Set.of("shiny"),List.of(ranking),NOW)).data().orElseThrow().collections().size());}
    }

}
