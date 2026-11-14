package br.com.shirokaidex.rankings;

import br.com.shirokaidex.captures.*;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.config.ConfigManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CaptureRankingsTest {
    @TempDir Path directory;
    private static final Instant AT=Instant.parse("2026-11-14T00:00:00Z");
    private static final CaptureRanking TOTAL=new CaptureRanking("captures","Capturas",CaptureRanking.Metric.TOTAL,"","");
    private static <T>T await(CompletableFuture<T> f)throws Exception{return f.get(10,TimeUnit.SECONDS);}
    private static CaptureRecord capture(UUID player,Instant at,boolean verified){return new CaptureRecord(UUID.randomUUID(),UUID.randomUUID(),player,"mod:a","normal",true,Set.of("legendary","mythical"),Set.of(),Set.of("custom"),"a".repeat(64),at,"minecraft:overworld",0,64,0,verified?CaptureRecord.Origin.NATIVE_PLAYER_SPAWN:CaptureRecord.Origin.UNKNOWN);}
    private void sql(Path file,String... commands)throws Exception{try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){for(String command:commands)q.execute(command);}}
    @Test void exactScoreTimeAndUuidOrderingWithPermanentMetricsAndNoDuplicateOrUnknownCredit()throws Exception{
        var a=UUID.fromString("00000000-0000-0000-0000-000000000001");var b=UUID.fromString("00000000-0000-0000-0000-000000000002");var d=UUID.randomUUID();
        try(var db=new Database(directory.resolve("order.sqlite"))){
            await(db.initialize());await(db.recordLogin(a,"First",AT));var first=capture(a,AT,true);await(db.recordCapture(first));assertFalse(await(db.recordCapture(first)));
            await(db.recordCapture(capture(b,AT,true)));await(db.recordCapture(capture(d,AT.plusSeconds(1),true)));await(db.recordCapture(capture(d,AT.plusSeconds(2),true)));await(db.recordCapture(capture(a,AT,false)));
            var entries=await(db.captureRanking(TOTAL,0)).entries();assertEquals(List.of(d,a,b),entries.stream().map(CaptureRankings.Entry::player).toList());assertEquals("First",entries.get(1).name());assertEquals(AT.plusSeconds(2),entries.getFirst().reachedAt());
            for(var metric:CaptureRanking.Metric.values()){
                if(metric==CaptureRanking.Metric.COLLECTION)continue;
                if(metric==CaptureRanking.Metric.SPECIES){await(db.statisticsPrivacy(d,br.com.shirokaidex.profiles.StatisticsPrivacy.Key.SPECIES_CAPTURES,true));}
                String key=metric==CaptureRanking.Metric.SPECIES?"mod:a":metric==CaptureRanking.Metric.CATEGORY?"custom":"";
                var definition=new CaptureRanking("test","Test",metric,key,"");assertEquals(2,await(db.captureRanking(definition,0)).entries().getFirst().score());
            }
        }
    }
    @Test void boundedPagesPrivacyInvalidatesCacheAndPersistsWithoutErasingScore()throws Exception{
        var file=directory.resolve("privacy.sqlite");var players=new ArrayList<UUID>();
        try(var db=new Database(file)){
            await(db.initialize());for(int i=0;i<12;i++){var player=new UUID(0,i+1);players.add(player);await(db.recordCapture(capture(player,AT,true)));}
            var page=await(db.captureRanking(TOTAL,0));assertEquals(10,page.entries().size());assertTrue(page.hasMore());assertEquals(2,await(db.captureRanking(TOTAL,1)).entries().size());assertThrows(UnsupportedOperationException.class,()->page.entries().clear());
            await(db.rankingPrivacy(players.getFirst(),true));assertFalse(await(db.captureRanking(TOTAL,0)).entries().stream().anyMatch(x->x.player().equals(players.getFirst())));
            assertThrows(ExecutionException.class,()->await(db.captureRanking(TOTAL,-1)));assertThrows(ExecutionException.class,()->await(db.captureRanking(TOTAL,100001)));
        }
        try(var db=new Database(file)){await(db.initialize());assertEquals(players.get(1),await(db.captureRanking(TOTAL,0)).entries().getFirst().player());await(db.rankingPrivacy(players.getFirst(),false));assertEquals(players.getFirst(),await(db.captureRanking(TOTAL,0)).entries().getFirst().player());}
    }
    @Test void scoreMutationInvalidatesCacheAndSurvivesDetailRemovalAndRestart()throws Exception{
        var file=directory.resolve("cache.sqlite");var player=UUID.randomUUID();
        try(var db=new Database(file)){await(db.initialize());assertTrue(await(db.captureRanking(TOTAL,0)).entries().isEmpty());await(db.recordCapture(capture(player,AT,true)));assertEquals(1,await(db.captureRanking(TOTAL,0)).entries().getFirst().score());}
        sql(file,"DELETE FROM capture_events");
        try(var db=new Database(file)){await(db.initialize());assertEquals(1,await(db.captureRanking(TOTAL,0)).entries().getFirst().score());}
    }
    @Test void migrationTenBackfillsScoresWithoutInventingMissingHistoricalTimestamps()throws Exception{
        var file=directory.resolve("ten.sqlite");var player=UUID.randomUUID();
        try(var db=new Database(file)){await(db.initialize());await(db.recordCapture(capture(player,AT,true)));}
        sql(file,"DROP TABLE player_medal_favorites", "DROP TABLE player_achievements", "DROP TABLE achievement_audit", "DROP TABLE achievement_definitions", "DROP TABLE profile_collection_points", "DROP TABLE player_stat_privacy", "DROP TABLE player_preferences", "DROP TABLE captured_forms", "DELETE FROM schema_migrations WHERE version=14", "DROP TABLE competition_awards", "DELETE FROM schema_migrations WHERE version=13", "DROP TABLE competition_adjustments", "DROP TABLE competition_audit", "DROP TABLE competition_results", "DROP TABLE competition_points", "DROP TABLE competition_entries", "DROP TABLE competition_editions", "DELETE FROM schema_migrations WHERE version=12", "DROP TABLE capture_ranking_scores","DROP TABLE player_ranking_privacy","DROP TABLE capture_record_zones","DROP TABLE capture_record_buckets","DROP INDEX capture_history_expiry","DELETE FROM schema_migrations WHERE version=11","PRAGMA user_version=10","DELETE FROM capture_events");
        try(var db=new Database(file)){
            await(db.initialize());var total=await(db.captureRanking(TOTAL,0)).entries().getFirst();assertEquals(1,total.score());assertEquals(AT,total.reachedAt());
            var shiny=await(db.captureRanking(new CaptureRanking("shiny","Shiny",CaptureRanking.Metric.SHINY,"",""),0)).entries().getFirst();assertEquals(1,shiny.score());assertNull(shiny.reachedAt());
        }
    }
    @Test void configuredDefinitionsAreStrictUniqueAndRetainDefaultsForOldConfigs()throws Exception{
        String base="{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\"";
        assertEquals(CaptureRanking.defaultsFor(br.com.shirokaidex.pokedex.CollectionDefinition.defaults()),ConfigManager.parse(base+"}").rankings());
        var parsed=ConfigManager.parse(base+",\"rankings\":[{\"id\":\"electric\",\"name\":\"Electric\",\"metric\":\"CATEGORY\",\"key\":\"electric\",\"permission\":\"dex.electric\"}]}");assertEquals("electric",parsed.rankings().getFirst().key());
        for(String defs:List.of("[{\"id\":\"x\",\"name\":\"X\",\"metric\":\"SPECIES\"}]","[{\"id\":\"x\",\"name\":\"X\",\"metric\":\"TOTAL\",\"key\":\"invalid\"}]","[{\"id\":\"x\",\"name\":\"X\",\"metric\":\"FAKE\"}]","[{\"id\":\"x\",\"name\":\"X\",\"metric\":\"TOTAL\",\"extra\":\"x\"}]","[{\"id\":\"x\",\"id\":\"y\",\"name\":\"X\",\"metric\":\"TOTAL\"}]"))assertThrows(java.io.IOException.class,()->ConfigManager.parse(base+",\"rankings\":"+defs+"}"));
        assertThrows(IllegalArgumentException.class,()->CaptureRanking.validate(List.of(TOTAL,TOTAL)));
    }
}
