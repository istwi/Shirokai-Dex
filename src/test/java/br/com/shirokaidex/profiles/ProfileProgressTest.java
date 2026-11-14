package br.com.shirokaidex.profiles;

import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.pokedex.*;
import br.com.shirokaidex.rankings.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfileProgressTest {
    @TempDir Path directory;
    private static final Instant AT=Instant.parse("2026-11-14T00:00:00Z");
    private static <T>T await(CompletableFuture<T> f)throws Exception{return f.get(10,TimeUnit.SECONDS);}
    @Test void rationalOrderingDistinguishesScoresThatDoublePrecisionCannotAndVersionChangesDoNotMix()throws Exception{
        var a=new UUID(0,1);var b=new UUID(0,2);var settings=Settings.defaults();String version="a".repeat(64);var definition=new CaptureRanking("dex_national","National",CaptureRanking.Metric.COLLECTION,"national","");
        assertTrue(ProfileProgress.compareRatios("2147483646/2147483647","2147483645/2147483646")>0);
        try(var db=new Database(directory.resolve("exact.sqlite"))){await(db.initialize());await(db.observeProfile(a,List.of(new ProfileProgress.Proof("national",version,2147483646,2147483647,AT)),settings,AT));await(db.observeProfile(b,List.of(new ProfileProgress.Proof("national",version,2147483645,2147483646,AT)),settings,AT));var entries=await(db.captureRanking(definition,0,settings.statisticsPrivacy(),version)).entries();assertEquals(a,entries.getFirst().player());assertNull(entries.getFirst().reachedAt());assertEquals(2147483647,entries.getFirst().total());assertTrue(await(db.captureRanking(definition,0,settings.statisticsPrivacy(),"b".repeat(64))).entries().isEmpty());
            await(db.observeProfile(b,List.of(new ProfileProgress.Proof("national",version,2147483646,2147483647,AT.plusSeconds(1))),settings,AT.plusSeconds(1)));entries=await(db.captureRanking(definition,0,settings.statisticsPrivacy(),version)).entries();assertEquals(b,entries.getFirst().player());assertEquals(AT.plusSeconds(1),entries.getFirst().reachedAt());await(db.rankingPrivacy(b,true));assertEquals(a,await(db.captureRanking(definition,0,settings.statisticsPrivacy(),version)).entries().getFirst().player());
        }
    }
    @Test void unavailableAndEmptyCollectionsDoNotBecomeFalseCompletionAndOfflineProofsPersist()throws Exception{
        var file=directory.resolve("proofs.sqlite");var player=UUID.randomUUID();var viewer=UUID.randomUUID();var settings=Settings.defaults();String version="a".repeat(64);var definition=new CaptureRanking("dex_national","National",CaptureRanking.Metric.COLLECTION,"national","");
        try(var db=new Database(file)){await(db.initialize());await(db.recordLogin(player,"DexProfile",AT));await(db.observeProfile(player,List.of(new ProfileProgress.Proof("national",version,null,100,AT)),settings,AT));assertTrue(await(db.captureRanking(definition,0,settings.statisticsPrivacy(),version)).entries().isEmpty());await(db.observeProfile(player,List.of(new ProfileProgress.Proof("national",version,0,0,AT)),settings,AT));assertTrue(await(db.captureRanking(definition,0,settings.statisticsPrivacy(),version)).entries().isEmpty());await(db.observeProfile(player,List.of(new ProfileProgress.Proof("national",version,5,100,AT.plusSeconds(1))),settings,AT.plusSeconds(1)));}
        try(var db=new Database(file)){await(db.initialize());var profile=await(db.profile(player.toString(),viewer,settings,Map.of("national",version),Set.of("national"),List.of(definition),AT.plusSeconds(2))).data().orElseThrow();assertEquals(5,profile.collections().getFirst().registered());assertEquals(AT.plusSeconds(1),profile.collections().getFirst().observedAt());assertEquals(1,profile.rankingPositions().get(definition.id()));assertTrue(await(db.profile(player.toString(),viewer,settings,Map.of("national","b".repeat(64)),Set.of("national"),List.of(),AT)).data().orElseThrow().collections().isEmpty());}
    }
    @Test void collectionVersionTracksCountingRulesAndDynamicRegistryRatherThanDisplayMetadata(){
        var form=new ProgressCalculator.Form("normal",Set.of(),Set.of("electric"),false,false,false);var catalog=List.of(new ProgressCalculator.Species("mod:a",false,false,List.of(form)));var original=CollectionDefinition.defaults().getFirst();var renamed=new CollectionDefinition(original.id(),"Renamed","Info",original.icon(),original.mode(),original.criteria(),original.permission());assertEquals(CollectionVersions.of(catalog,List.of(original)),CollectionVersions.of(catalog,List.of(renamed)));var expanded=List.of(catalog.getFirst(),new ProgressCalculator.Species("mod:b",false,false,List.of(form)));assertNotEquals(CollectionVersions.of(catalog,List.of(original)),CollectionVersions.of(expanded,List.of(original)));
    }
}
