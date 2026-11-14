package br.com.shirokaidex.competitions;

import br.com.shirokaidex.captures.*;
import br.com.shirokaidex.categories.*;
import br.com.shirokaidex.config.*;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.rankings.CaptureRanking;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CompetitionTest {
    @TempDir Path directory;
    private static final Instant AT=Instant.parse("2026-11-14T00:00:00Z");
    private static <T>T await(CompletableFuture<T> f)throws Exception{return f.get(10,TimeUnit.SECONDS);}
    private static CompetitionDefinition definition(String id,CompetitionDefinition.ReviewMode mode,int threshold){return new CompetitionDefinition(id,id,CompetitionDefinition.Cadence.CUSTOM,ZoneId.of("America/Sao_Paulo"),AT,AT.plusSeconds(60),CaptureRanking.Metric.TOTAL,"",CompetitionDefinition.CategoryMode.SNAPSHOT,CompetitionDefinition.Visibility.PUBLIC,1,true,mode,threshold,"");}
    private static Settings settings(CategoryCatalog categories,CompetitionDefinition... defs)throws Exception{
        var root=new JsonObject();root.addProperty("schema_version",1);root.addProperty("language","pt_br");root.addProperty("menu_title","Dex");root.add("categories",JsonParser.parseString(CategoryRules.of(categories).json()).getAsJsonObject().get("categories"));var array=new JsonArray();for(var d:defs)array.add(JsonParser.parseString(CompetitionJson.write(d)));root.add("competitions",array);return ConfigManager.parse(root.toString());
    }
    private static Settings settings(CompetitionDefinition... defs)throws Exception{return settings(new CategoryCatalog(List.of()),defs);}
    private static CaptureRecord record(UUID player,Instant at,Settings settings){
        var form=new br.com.shirokaidex.pokedex.ProgressCalculator.Form("normal",Set.of(),Set.of("electric"),false,false,false);var categories=new HashSet<String>();for(var key:settings.categories().definitions().keySet())if(settings.categories().matches(key,"cobblemon:pikachu",form))categories.add(key);
        return new CaptureRecord(UUID.randomUUID(),UUID.randomUUID(),player,"cobblemon:pikachu","normal",false,Set.of(),Set.of("electric"),categories,CategoryRules.of(settings.categories()).fingerprint(),at,"minecraft:overworld",0,64,0,CaptureRecord.Origin.UNKNOWN);
    }
    private static CaptureObservation observation(CaptureRecord record,UUID entity){return new CaptureObservation(record,entity,CaptureObservation.Kind.LIVE_NATIVE);}
    private static void capture(Database db,CaptureRecord record,Settings settings)throws Exception{var entity=UUID.randomUUID();await(db.recordNativeSpawnProof(new NativeSpawnProof(record.pokemon(),entity,record.dimension(),record.capturedAt().minusSeconds(1),NativeSpawnProof.Source.PREHIT)));assertTrue(await(db.recordObservedCapture(observation(record,entity),settings)));}
    private static String edition(Database db)throws Exception{return await(db.competitionEditions(false)).getFirst().id();}
    @Test void recurringCalendarsHonorMondayMonthlyTimezoneDstAndExclusiveCustomEnd(){
        var d=definition("weekly",CompetitionDefinition.ReviewMode.LOG_ONLY,0);var weekly=new CompetitionDefinition(d.id(),d.name(),CompetitionDefinition.Cadence.WEEKLY,ZoneId.of("America/New_York"),null,null,d.metric(),d.key(),d.categoryMode(),d.visibility(),24,true,d.reviewMode(),0,"");
        var window=weekly.window(Instant.parse("2026-11-01T12:00:00Z")).orElseThrow();assertEquals(Instant.parse("2026-10-26T04:00:00Z"),window.start());assertEquals(Duration.ofHours(169),Duration.between(window.start(),window.end()));
        var monthly=new CompetitionDefinition("month","Month",CompetitionDefinition.Cadence.MONTHLY,ZoneId.of("America/Sao_Paulo"),null,null,d.metric(),d.key(),d.categoryMode(),d.visibility(),24,true,d.reviewMode(),0,"");assertEquals(29,Duration.between(monthly.window(Instant.parse("2028-02-10T12:00:00Z")).orElseThrow().start(),monthly.window(Instant.parse("2028-02-10T12:00:00Z")).orElseThrow().end()).toDays());
        assertTrue(d.window(AT.minusMillis(1)).isEmpty());assertTrue(d.window(AT.plusSeconds(60)).isEmpty());assertTrue(d.window(AT).isPresent());
    }
    @Test void enrollmentCountsOnlyLaterDistinctVerifiedCapturesAndConfirmedWithdrawalIsPermanent()throws Exception{
        var settings=settings(definition("test",CompetitionDefinition.ReviewMode.LOG_ONLY,0));var player=UUID.randomUUID();
        try(var db=new Database(directory.resolve("join.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));var id=edition(db);
            capture(db,record(player,AT.plusSeconds(1),settings),settings);assertTrue(await(db.enrollCompetition(id,player,AT.plusSeconds(2))));
            capture(db,record(player,AT.plusSeconds(2),settings),settings);var counted=record(player,AT.plusSeconds(3),settings);capture(db,counted,settings);
            assertFalse(await(db.recordObservedCapture(observation(counted,UUID.randomUUID()),settings)));capture(db,record(player,AT.plusSeconds(4),settings),settings);
            assertEquals(2,await(db.competitionView(id,player,0)).orElseThrow().membership().score());assertFalse(await(db.cancelCompetition(id,player,false,AT.plusSeconds(5))));
            assertTrue(await(db.cancelCompetition(id,player,true,AT.plusSeconds(5))));assertFalse(await(db.enrollCompetition(id,player,AT.plusSeconds(6))));capture(db,record(player,AT.plusSeconds(7),settings),settings);
            var view=await(db.competitionView(id,player,0)).orElseThrow();assertTrue(view.membership().cancelled());assertEquals(2,view.membership().score());assertTrue(view.standings().isEmpty());assertEquals(5,await(db.captureSummary(player,AT.plusSeconds(10),ZoneId.of("UTC"))).total());
        }
    }
    @Test void heldPointsBlockFinalizationAndReviewChangesOnlyCompetitionWithoutRepeatingFinalization()throws Exception{
        var file=directory.resolve("held.sqlite");var settings=settings(definition("held",CompetitionDefinition.ReviewMode.HOLD_FOR_REVIEW,1));var player=UUID.randomUUID();String id;var second=record(player,AT.plusSeconds(3),settings);
        try(var db=new Database(file)){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));id=edition(db);await(db.enrollCompetition(id,player,AT));capture(db,record(player,AT.plusSeconds(2),settings),settings);capture(db,second,settings);await(db.synchronizeCompetitions(settings,AT.plusSeconds(3661)));assertEquals(CompetitionStore.State.REVIEW,await(db.competitionView(id,player,0)).orElseThrow().edition().state());assertEquals(1,await(db.competitionView(id,player,0)).orElseThrow().membership().score());
            assertThrows(ExecutionException.class,()->await(db.reviewCompetitionPoint(id,second.eventKey(),"COUNTED",player.toString(),"short",AT.plusSeconds(3662))));
            assertTrue(await(db.reviewCompetitionPoint(id,second.eventKey(),"REJECTED",player.toString(),"Reviewed origin evidence",AT.plusSeconds(3662))));await(db.synchronizeCompetitions(settings,AT.plusSeconds(3663)));assertEquals(CompetitionStore.State.FINAL,await(db.competitionView(id,player,0)).orElseThrow().edition().state());assertEquals(2,await(db.captureSummary(player,AT.plusSeconds(4000),ZoneId.of("UTC"))).total());assertFalse(await(db.reviewCompetitionPoint(id,second.eventKey(),"COUNTED",player.toString(),"Cannot rewrite official result",AT.plusSeconds(4000))));
        }
        try(var db=new Database(file)){await(db.initialize());await(db.synchronizeCompetitions(settings,AT.plusSeconds(5000)));assertFalse(await(db.finalizeCompetition(id,player.toString(),"Repeat finalization blocked",AT.plusSeconds(5000))));}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var rows=q.executeQuery("SELECT count(*) FROM competition_audit WHERE action='FINALIZED'")){assertEquals(1,rows.getInt(1));}
    }
    @Test void logOnlyThresholdNeverTreatsFastCapturesAsProvenAbuse()throws Exception{
        var settings=settings(definition("logs",CompetitionDefinition.ReviewMode.LOG_ONLY,1));var player=UUID.randomUUID();try(var db=new Database(directory.resolve("log.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));var id=edition(db);await(db.enrollCompetition(id,player,AT));capture(db,record(player,AT.plusSeconds(1),settings),settings);capture(db,record(player,AT.plusSeconds(2),settings),settings);assertEquals(2,await(db.competitionView(id,player,0)).orElseThrow().membership().score());}
    }
    @Test void snapshotAndLiveCategoryReloadDoNotRewritePreviouslyScoredCaptures()throws Exception{
        var criteria=new br.com.shirokaidex.pokedex.CollectionDefinition.Criteria(Set.of(),Set.of(),Set.of(),Set.of("electric"),Set.of());var electric=new CategoryCatalog(List.of(new CategoryCatalog.Definition("element",criteria,Set.of(),Set.of(),Set.of())));
        var fire=new CategoryCatalog(List.of(new CategoryCatalog.Definition("element",new br.com.shirokaidex.pokedex.CollectionDefinition.Criteria(Set.of(),Set.of(),Set.of(),Set.of("fire"),Set.of()),Set.of(),Set.of(),Set.of())));
        var base=definition("snapshot",CompetitionDefinition.ReviewMode.LOG_ONLY,0);
        var snap=new CompetitionDefinition(base.id(),base.name(),base.cadence(),base.timezone(),base.start(),base.end(),CaptureRanking.Metric.CATEGORY,"element",CompetitionDefinition.CategoryMode.SNAPSHOT,base.visibility(),1,true,base.reviewMode(),0,"");
        var live=new CompetitionDefinition("live","Live",snap.cadence(),snap.timezone(),snap.start(),snap.end(),snap.metric(),snap.key(),CompetitionDefinition.CategoryMode.LIVE,snap.visibility(),1,true,snap.reviewMode(),0,"");
        var before=settings(electric,snap,live);var after=settings(fire,snap,live);var player=UUID.randomUUID();
        try(var db=new Database(directory.resolve("categories.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(before,AT));var editions=await(db.competitionEditions(false));for(var e:editions)await(db.enrollCompetition(e.id(),player,AT));capture(db,record(player,AT.plusSeconds(1),before),before);capture(db,record(player,AT.plusSeconds(2),after),after);
            for(var e:editions)assertEquals(e.definition().categoryMode()==CompetitionDefinition.CategoryMode.SNAPSHOT?2:1,await(db.competitionView(e.id(),player,0)).orElseThrow().membership().score());
        }
    }
    @Test void privacyExcludesPublicRowsAndMakesOfficialAwardEligibilityFalse()throws Exception{
        var file=directory.resolve("privacy.sqlite");var settings=settings(definition("private",CompetitionDefinition.ReviewMode.LOG_ONLY,0));var player=UUID.randomUUID();
        try(var db=new Database(file)){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));var id=edition(db);await(db.enrollCompetition(id,player,AT));capture(db,record(player,AT.plusSeconds(1),settings),settings);await(db.rankingPrivacy(player,true));assertTrue(await(db.competitionView(id,player,0)).orElseThrow().standings().isEmpty());await(db.synchronizeCompetitions(settings,AT.plusSeconds(3661)));assertTrue(await(db.competitionView(id,player,0)).orElseThrow().standings().isEmpty());}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var rows=q.executeQuery("SELECT eligible FROM competition_results")){assertEquals(0,rows.getInt(1));}
    }
    @Test void pendingCompetitionAndAuditedCorrectionsProtectExpiredCaptureDetailsFromRetention()throws Exception{
        var settings=settings(definition("retain",CompetitionDefinition.ReviewMode.LOG_ONLY,0));var player=UUID.randomUUID();var record=record(player,AT.plusSeconds(1),settings);
        try(var db=new Database(directory.resolve("retention.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));var id=edition(db);await(db.enrollCompetition(id,player,AT));capture(db,record,settings);assertEquals(0,await(db.cleanupCaptureHistory(CaptureRetention.defaults(),AT.plus(Duration.ofDays(100)))));assertTrue(await(db.reviewCompetitionPoint(id,record.eventKey(),"REJECTED",player.toString(),"Keep audited capture evidence",AT.plusSeconds(5))));await(db.synchronizeCompetitions(settings,AT.plus(Duration.ofDays(100))));assertEquals(0,await(db.cleanupCaptureHistory(CaptureRetention.defaults(),AT.plus(Duration.ofDays(100)))));}
    }
    @Test void historyDisabledStillRetainsRequiredCompetitiveEvidenceAndCanPruneAfterFinal()throws Exception{
        var d=definition("evidence",CompetitionDefinition.ReviewMode.LOG_ONLY,0);
        var settings=ConfigManager.parse("{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"capture_history\":{\"enabled\":false},\"competitions\":["+CompetitionJson.write(d)+"]}");
        var player=UUID.randomUUID();try(var db=new Database(directory.resolve("detail.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));var id=edition(db);await(db.enrollCompetition(id,player,AT));capture(db,record(player,AT.plusSeconds(1),settings),settings);assertEquals(1,await(db.captureHistory(player,0)).entries().size());
            await(db.synchronizeCompetitions(settings,AT.plusSeconds(3661)));assertEquals(1,await(db.cleanupCaptureHistory(CaptureRetention.defaults(),AT.plus(Duration.ofDays(100)))));assertEquals(1,await(db.competitionView(id,player,0)).orElseThrow().standings().getFirst().score());assertEquals(1,await(db.captureSummary(player,AT.plus(Duration.ofDays(100)),ZoneId.of("UTC"))).total());}
    }
    @Test void cancelledPlayerCanEnrollInNextEditionAndOldDefinitionRemainsFrozen()throws Exception{
        var d=definition("repeat",CompetitionDefinition.ReviewMode.LOG_ONLY,0);var settings=settings(d);var next=new CompetitionDefinition(d.id(),"New title",d.cadence(),d.timezone(),d.end(),d.end().plusSeconds(60),d.metric(),d.key(),d.categoryMode(),d.visibility(),d.reviewHours(),true,d.reviewMode(),0,"");var nextSettings=settings(next);var player=UUID.randomUUID();
        try(var db=new Database(directory.resolve("next.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));var old=edition(db);await(db.enrollCompetition(old,player,AT));await(db.cancelCompetition(old,player,true,AT.plusSeconds(1)));await(db.synchronizeCompetitions(nextSettings,AT.plusSeconds(61)));var current=await(db.competitionEditions(false)).stream().filter(e->e.state()==CompetitionStore.State.ACTIVE).findFirst().orElseThrow();assertNotEquals(old,current.id());assertTrue(await(db.enrollCompetition(current.id(),player,AT.plusSeconds(62))));assertEquals("repeat",await(db.competitionView(old,player,0)).orElseThrow().edition().definition().name());}
    }
    @Test void failedCompetitiveWriteRollsBackCaptureReceiptStatisticsAndConsumedProof()throws Exception{
        var file=directory.resolve("fault.sqlite");var settings=settings(definition("fault",CompetitionDefinition.ReviewMode.LOG_ONLY,0));var player=UUID.randomUUID();var record=record(player,AT.plusSeconds(1),settings);var entity=UUID.randomUUID();String id;
        try(var db=new Database(file)){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));id=edition(db);await(db.enrollCompetition(id,player,AT));await(db.recordNativeSpawnProof(new NativeSpawnProof(record.pokemon(),entity,record.dimension(),AT,NativeSpawnProof.Source.PREHIT)));}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){q.execute("CREATE TRIGGER reject_point BEFORE INSERT ON competition_points BEGIN SELECT RAISE(ABORT, 'test competitive write failure'); END");}
        try(var db=new Database(file)){await(db.initialize());assertThrows(ExecutionException.class,()->await(db.recordObservedCapture(observation(record,entity),settings)));assertEquals(0,await(db.captureSummary(player,AT.plusSeconds(5),ZoneId.of("UTC"))).total());assertEquals(0,await(db.competitionView(id,player,0)).orElseThrow().membership().score());}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){try(var rows=q.executeQuery("SELECT consumed_event FROM native_spawn_proofs")){assertNull(rows.getString(1));}q.execute("DROP TRIGGER reject_point");}
        try(var db=new Database(file)){await(db.initialize());assertTrue(await(db.recordObservedCapture(observation(record,entity),settings)));assertEquals(1,await(db.captureSummary(player,AT.plusSeconds(5),ZoneId.of("UTC"))).total());assertEquals(1,await(db.competitionView(id,player,0)).orElseThrow().membership().score());}
    }

    @Test void administrativeAdjustmentsAreAuditedReversibleOnceAndCannotChangePermanentTotals()throws Exception{
        var settings=settings(definition("adjust",CompetitionDefinition.ReviewMode.LOG_ONLY,0));var player=UUID.randomUUID();
        try(var db=new Database(directory.resolve("adjust.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));var id=edition(db);await(db.enrollCompetition(id,player,AT));capture(db,record(player,AT.plusSeconds(1),settings),settings);
            assertTrue(await(db.adjustCompetition(id,player,-2,player.toString(),"Would create negative score",AT.plusSeconds(2))).isEmpty());
            var adjustment=await(db.adjustCompetition(id,player,5,player.toString(),"Audited scoring correction",AT.plusSeconds(3))).orElseThrow();assertEquals(6,await(db.competitionView(id,player,0)).orElseThrow().membership().score());
            assertTrue(await(db.revertCompetitionAdjustment(adjustment,player.toString(),"Reverse scoring correction",AT.plusSeconds(4))).isPresent());assertTrue(await(db.revertCompetitionAdjustment(adjustment,player.toString(),"Duplicate reversal blocked",AT.plusSeconds(5))).isEmpty());assertEquals(1,await(db.competitionView(id,player,0)).orElseThrow().membership().score());assertEquals(1,await(db.captureSummary(player,AT.plusSeconds(5),ZoneId.of("UTC"))).total());
            await(db.synchronizeCompetitions(settings,AT.plusSeconds(3661)));assertTrue(await(db.adjustCompetition(id,player,1,player.toString(),"Official results stay frozen",AT.plusSeconds(3662))).isEmpty());
        }
    }

    @Test void shinyCaptureCanEvaluateFrozenCategoryWithoutFabricatingOfficialKnowledge()throws Exception{
        var category=new CategoryCatalog(List.of(new CategoryCatalog.Definition("electric",new br.com.shirokaidex.pokedex.CollectionDefinition.Criteria(Set.of(),Set.of(),Set.of(),Set.of("electric"),Set.of()),Set.of(),Set.of(),Set.of())));
        var d=definition("shiny_category",CompetitionDefinition.ReviewMode.LOG_ONLY,0);var definition=new CompetitionDefinition(d.id(),d.name(),d.cadence(),d.timezone(),d.start(),d.end(),CaptureRanking.Metric.CATEGORY,"electric",d.categoryMode(),d.visibility(),1,true,d.reviewMode(),0,"");var settings=settings(category,definition);var player=UUID.randomUUID();
        var r=record(player,AT.plusSeconds(1),settings);var shiny=new CaptureRecord(r.ball(),r.pokemon(),r.player(),r.species(),r.form(),true,r.labels(),r.types(),r.categories(),r.categoryRules(),r.capturedAt(),r.dimension(),r.x(),r.y(),r.z(),r.origin());
        try(var db=new Database(directory.resolve("shiny-category.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));var id=edition(db);await(db.enrollCompetition(id,player,AT));capture(db,shiny,settings);assertEquals(1,await(db.competitionView(id,player,0)).orElseThrow().membership().score());assertEquals(1,await(db.captureSummary(player,AT.plusSeconds(2),ZoneId.of("UTC"))).shiny());}
    }

    @Test void paginatedHistoryUsesArchivedCompetitionCategoryAndOverlappingPeriodFilters()throws Exception{
        var file=directory.resolve("history.sqlite");var settings=settings(definition("history_test",CompetitionDefinition.ReviewMode.LOG_ONLY,0));
        try(var db=new Database(file)){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));await(db.synchronizeCompetitions(settings,AT.plusSeconds(3661)));var all=await(db.competitionHistory(CompetitionStore.HistoryFilter.all(),0));assertEquals(1,all.editions().size());assertFalse(all.hasMore());assertEquals(CompetitionStore.State.FINAL,all.editions().getFirst().state());
            assertTrue(await(db.competitionHistory(new CompetitionStore.HistoryFilter("missing","",null,null),0)).editions().isEmpty());assertTrue(await(db.competitionHistory(new CompetitionStore.HistoryFilter("","electric",null,null),0)).editions().isEmpty());
            assertEquals(1,await(db.competitionHistory(new CompetitionStore.HistoryFilter("history_test","",AT.plusSeconds(30),AT.plusSeconds(90)),0)).editions().size());assertTrue(await(db.competitionHistory(new CompetitionStore.HistoryFilter("","",AT.plusSeconds(60),AT.plusSeconds(90)),0)).editions().isEmpty());assertTrue(await(db.competitionHistory(CompetitionStore.HistoryFilter.all(),1)).editions().isEmpty());
            assertThrows(ExecutionException.class,()->await(db.competitionHistory(CompetitionStore.HistoryFilter.all(),-1)));assertThrows(IllegalArgumentException.class,()->new CompetitionStore.HistoryFilter("bad id","",null,null));assertThrows(IllegalArgumentException.class,()->new CompetitionStore.HistoryFilter("","",AT,AT));
        }
    }
    @Test void administratorAuditAndInspectExposePersistedPointEvidenceWithoutMutation()throws Exception{
        var settings=settings(definition("audit",CompetitionDefinition.ReviewMode.LOG_ONLY,0));var player=UUID.randomUUID();var record=record(player,AT.plusSeconds(1),settings);
        try(var db=new Database(directory.resolve("inspect.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));var id=edition(db);await(db.enrollCompetition(id,player,AT));capture(db,record,settings);
            assertEquals(1,await(db.competitionAdminRows(id,"points",null,0)).size());assertTrue(await(db.competitionAdminRows(id,"points",null,1)).isEmpty());var inspected=await(db.competitionAdminRows(id,"inspect",record.eventKey(),0));assertEquals(1,inspected.size());assertTrue(inspected.getFirst().contains("cobblemon:pikachu"));assertTrue(inspected.getFirst().contains("NATIVE_PLAYER_SPAWN"));assertTrue(await(db.competitionAdminRows(id,"audit",null,0)).stream().anyMatch(row->row.contains("JOINED")));assertThrows(ExecutionException.class,()->await(db.competitionAdminRows(id,"invalid",null,0)));assertEquals(1,await(db.competitionView(id,player,0)).orElseThrow().membership().score());
        }
    }

}
