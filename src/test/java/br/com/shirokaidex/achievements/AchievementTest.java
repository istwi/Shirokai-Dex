package br.com.shirokaidex.achievements;

import br.com.shirokaidex.captures.*;
import br.com.shirokaidex.config.*;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.profiles.ProfileProgress;
import br.com.shirokaidex.rankings.CaptureRanking;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AchievementTest {
    @TempDir Path directory;
    private static final Instant AT=Instant.parse("2026-11-14T00:00:00Z");
    private static <T>T await(CompletableFuture<T> f)throws Exception{return f.get(10,TimeUnit.SECONDS);}
    private static AchievementDefinition definition(String id,Requirement requirement,AchievementDefinition.Policy policy,boolean retroactive){return new AchievementDefinition(id,id,"Visual medal","minecraft:nether_star",false,"",policy,retroactive,true,true,Set.of(),requirement);}
    private static Settings settings(AchievementDefinition... definitions)throws Exception{var values=new com.google.gson.JsonArray();for(var d:definitions)values.add(com.google.gson.JsonParser.parseString(AchievementJson.write(d)));return ConfigManager.parse("{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"achievements\":"+values+"}");}
    private static CaptureRecord capture(UUID player,boolean verified){return new CaptureRecord(UUID.randomUUID(),UUID.randomUUID(),player,"mod:a","normal",true,Set.of(),Set.of(),Set.of(),"a".repeat(64),AT,"minecraft:overworld",0,64,0,verified?CaptureRecord.Origin.NATIVE_PLAYER_SPAWN:CaptureRecord.Origin.UNKNOWN);}
    private static ProfileProgress.Proof proof(Integer value,String version){return new ProfileProgress.Proof("national",version,value,10,AT);}
    @Test void realStoredEvidenceUnlocksVisualAndOrDependencyGraphOnceWithoutMaterialOperations()throws Exception{
        var first=definition("first",new Requirement.Count(CaptureRanking.Metric.TOTAL,"",1),AchievementDefinition.Policy.PERMANENT,true);
        var combined=definition("combined",new Requirement.All(List.of(new Requirement.Achieved("first"),new Requirement.Any(List.of(new Requirement.Form("mod:a","normal"),new Requirement.Progress("national",2,null))))),AchievementDefinition.Policy.PERMANENT,true);
        var settings=settings(combined,first);assertEquals("first",settings.achievements().getFirst().id());var player=UUID.randomUUID();var file=directory.resolve("visual.sqlite");
        try(var db=new Database(file)){await(db.initialize());await(db.recordCapture(capture(player,false)));assertTrue(await(db.observeProfile(player,List.of(proof(null,"a".repeat(64))),settings,AT)).isEmpty());await(db.recordCapture(capture(player,true)));assertEquals(Set.of("first","combined"),Set.copyOf(await(db.observeProfile(player,List.of(proof(null,"a".repeat(64))),settings,AT.plusSeconds(1)))));assertTrue(await(db.observeProfile(player,List.of(),settings,AT.plusSeconds(2))).isEmpty());assertEquals(2,await(db.medals(player)).stream().filter(AchievementStore.Medal::active).count());}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var rows=q.executeQuery("SELECT count(*) FROM reward_operations")){assertEquals(0,rows.getInt(1));}
    }
    @Test void permanentAndRecalculatedMedalsTreatUnknownAsUnavailableRatherThanFalse()throws Exception{
        var required=new Requirement.Progress("national",2,null);var settings=settings(definition("permanent",required,AchievementDefinition.Policy.PERMANENT,true),definition("live",required,AchievementDefinition.Policy.RECALCULATE,true));var player=UUID.randomUUID();
        try(var db=new Database(directory.resolve("policies.sqlite"))){await(db.initialize());assertEquals(2,await(db.observeProfile(player,List.of(proof(3,"a".repeat(64))),settings,AT)).size());await(db.observeProfile(player,List.of(proof(null,"a".repeat(64))),settings,AT.plusSeconds(1)));assertEquals(2,await(db.medals(player)).stream().filter(AchievementStore.Medal::active).count());await(db.observeProfile(player,List.of(proof(1,"a".repeat(64))),settings,AT.plusSeconds(2)));var medals=await(db.medals(player));assertTrue(medals.stream().filter(m->m.definition().id().equals("permanent")).findFirst().orElseThrow().active());assertFalse(medals.stream().filter(m->m.definition().id().equals("live")).findFirst().orElseThrow().active());assertTrue(await(db.observeProfile(player,List.of(proof(3,"a".repeat(64))),settings,AT.plusSeconds(3))).isEmpty());}
    }
    @Test void nonRetroactiveCountersPersistBaselineAcrossRestartAndNeedNewCaptures()throws Exception{
        var settings=settings(definition("fresh",new Requirement.Count(CaptureRanking.Metric.TOTAL,"",2),AchievementDefinition.Policy.PERMANENT,false));var player=UUID.randomUUID();var file=directory.resolve("baseline.sqlite");
        try(var db=new Database(file)){await(db.initialize());for(int i=0;i<3;i++)await(db.recordCapture(capture(player,true)));assertTrue(await(db.observeProfile(player,List.of(),settings,AT)).isEmpty());await(db.recordCapture(capture(player,true)));assertTrue(await(db.observeProfile(player,List.of(),settings,AT.plusSeconds(1))).isEmpty());}
        try(var db=new Database(file)){await(db.initialize());await(db.recordCapture(capture(player,true)));assertEquals(List.of("fresh"),await(db.observeProfile(player,List.of(),settings,AT.plusSeconds(2))));}
    }
    @Test void nonRetroactivePokedexDefersBaselineUntilReliableAndCannotCompareDifferentRules()throws Exception{
        var settings=settings(definition("fresh_dex",new Requirement.Progress("national",1,null),AchievementDefinition.Policy.RECALCULATE,false));var player=UUID.randomUUID();
        try(var db=new Database(directory.resolve("dex-baseline.sqlite"))){await(db.initialize());assertTrue(await(db.observeProfile(player,List.of(proof(null,"a".repeat(64))),settings,AT)).isEmpty());assertTrue(await(db.observeProfile(player,List.of(proof(5,"a".repeat(64))),settings,AT.plusSeconds(1))).isEmpty());assertEquals(List.of("fresh_dex"),await(db.observeProfile(player,List.of(proof(6,"a".repeat(64))),settings,AT.plusSeconds(2))));await(db.observeProfile(player,List.of(proof(0,"b".repeat(64))),settings,AT.plusSeconds(3)));assertTrue(await(db.medals(player)).getFirst().active());}
    }
    @Test void onlyUnlockedMedalsCanOccupyThreeUniqueFavoriteSlotsAndSelectionsPersist()throws Exception{
        var requirement=new Requirement.Count(CaptureRanking.Metric.TOTAL,"",1);var settings=settings(definition("a",requirement,AchievementDefinition.Policy.PERMANENT,true),definition("b",requirement,AchievementDefinition.Policy.PERMANENT,true),definition("c",requirement,AchievementDefinition.Policy.PERMANENT,true),definition("d",requirement,AchievementDefinition.Policy.PERMANENT,true));var player=UUID.randomUUID();var file=directory.resolve("favorites.sqlite");
        try(var db=new Database(file)){await(db.initialize());await(db.observeProfile(player,List.of(),settings,AT));assertFalse(await(db.favoriteMedal(player,"a",0)));await(db.recordCapture(capture(player,true)));await(db.observeProfile(player,List.of(),settings,AT.plusSeconds(1)));for(int i=0;i<3;i++)assertTrue(await(db.favoriteMedal(player,""+(char)('a'+i),i)));assertThrows(ExecutionException.class,()->await(db.favoriteMedal(player,"d",3)));assertTrue(await(db.favoriteMedal(player,"a",2)));assertEquals(2,await(db.medals(player)).stream().filter(m->m.favoriteSlot()!=null).count());assertTrue(await(db.favoriteMedal(player,"d",0)));}
        try(var db=new Database(file)){await(db.initialize());assertEquals(3,await(db.medals(player)).stream().filter(m->m.favoriteSlot()!=null).count());assertTrue(await(db.favoriteMedal(player,"a",-1)));assertEquals(2,await(db.medals(player)).stream().filter(m->m.favoriteSlot()!=null).count());}
    }
    @Test void failedEvaluationDoesNotPoisonDefinitionCacheAndRetryCreatesConsistentEvidence()throws Exception{
        var file=directory.resolve("rollback.sqlite");var player=UUID.randomUUID();var settings=settings(definition("retry",new Requirement.Count(CaptureRanking.Metric.TOTAL,"",1),AchievementDefinition.Policy.PERMANENT,true));
        try(var db=new Database(file)){await(db.initialize());await(db.recordCapture(capture(player,true)));}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){q.execute("CREATE TRIGGER reject_medal BEFORE INSERT ON player_achievements BEGIN SELECT RAISE(ABORT, 'test medal failure'); END");}
        try(var db=new Database(file)){await(db.initialize());assertThrows(ExecutionException.class,()->await(db.observeProfile(player,List.of(),settings,AT)));try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){q.execute("DROP TRIGGER reject_medal");}assertEquals(List.of("retry"),await(db.observeProfile(player,List.of(),settings,AT.plusSeconds(1))));}
    }
    @Test void configurationRejectsCyclesUnknownReferencesExecutableFieldsDuplicatesAndFractionalCounts()throws Exception{
        String base="{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"achievements\":[";
        for(String bad:List.of("{\"id\":\"a\",\"name\":\"A\",\"requirement\":{\"type\":\"CAPTURES\",\"metric\":\"TOTAL\",\"minimum\":1.5}}","{\"id\":\"a\",\"name\":\"A\",\"rewards\":[],\"requirement\":{\"type\":\"CAPTURES\",\"metric\":\"TOTAL\",\"minimum\":1}}","{\"id\":\"a\",\"name\":\"A\",\"requirement\":{\"type\":\"ACHIEVEMENT\",\"id\":\"a\"}}","{\"id\":\"a\",\"name\":\"A\",\"requirement\":{\"type\":\"ACHIEVEMENT\",\"id\":\"missing\"}}","{\"id\":\"a\",\"name\":\"A\",\"requirement\":{\"type\":\"CAPTURES\",\"metric\":\"TOTAL\",\"minimum\":1,\"minimum\":2}}"))assertThrows(java.io.IOException.class,()->ConfigManager.parse(base+bad+"]}"));
    }
    @Test void publicProfileHonorsCurrentMedalVisibilityBeforeBackgroundReevaluation()throws Exception{
        var d=definition("visibility",new Requirement.Count(CaptureRanking.Metric.TOTAL,"",1),AchievementDefinition.Policy.PERMANENT,true);var before=settings(d);var privateDefinition=new AchievementDefinition(d.id(),d.name(),d.description(),d.icon(),d.secret(),d.hint(),d.policy(),d.retroactive(),false,d.notifyPlayer(),d.dependencies(),d.requirement());var after=settings(privateDefinition);var player=UUID.randomUUID();var viewer=UUID.randomUUID();
        try(var db=new Database(directory.resolve("current-visibility.sqlite"))){await(db.initialize());await(db.recordLogin(player,"MedalPlayer",AT));await(db.recordCapture(capture(player,true)));await(db.observeProfile(player,List.of(),before,AT));assertEquals(1,await(db.profile(player.toString(),viewer,before,Map.of(),Set.of(),List.of(),AT)).data().orElseThrow().medals().size());assertTrue(await(db.profile(player.toString(),viewer,after,Map.of(),Set.of(),List.of(),AT)).data().orElseThrow().medals().isEmpty());assertEquals(1,await(db.profile(player.toString(),player,after,Map.of(),Set.of(),List.of(),AT)).data().orElseThrow().medals().size());await(db.statisticsPrivacy(player,br.com.shirokaidex.profiles.StatisticsPrivacy.Key.TOTAL_CAPTURES,false));assertTrue(await(db.profile(player.toString(),viewer,before,Map.of(),Set.of(),List.of(),AT)).data().orElseThrow().medals().isEmpty());}
    }

}
