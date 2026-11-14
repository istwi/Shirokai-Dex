package br.com.shirokaidex.captures;

import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.config.ConfigManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CaptureTest {
    @TempDir Path directory;
    private static final Instant NOW=Instant.parse("2026-11-02T03:30:00Z");
    private static <T>T await(CompletableFuture<T> f)throws Exception{return f.get(8,TimeUnit.SECONDS);}
    private static CaptureRecord record(UUID player,UUID ball,UUID pokemon,Instant at,boolean shiny,Set<String> labels,CaptureRecord.Origin origin){return new CaptureRecord(ball,pokemon,player,"cobblemon:pikachu","normal",shiny,labels,Set.of("electric"),Set.of("electric_captures"),"a".repeat(64),at,"minecraft:overworld",1,64,2,origin);}
    private static CaptureRecord verified(UUID player){return record(player,UUID.randomUUID(),UUID.randomUUID(),NOW,false,Set.of(),CaptureRecord.Origin.NATIVE_PLAYER_SPAWN);}
    @Test void hitEvidenceMustMatchRecipientTargetAndNativeWildSource(){
        var tracker=new CaptureTracker();var ball=UUID.randomUUID();var pokemon=UUID.randomUUID();var player=UUID.randomUUID();
        assertEquals(CaptureRecord.Origin.UNKNOWN,tracker.captured(ball,pokemon,player,NOW));
        tracker.hit(ball,pokemon,player,NOW,true);assertEquals(CaptureRecord.Origin.UNKNOWN,tracker.captured(ball,pokemon,UUID.randomUUID(),NOW));
        tracker.hit(ball,pokemon,player,NOW,false);assertEquals(CaptureRecord.Origin.UNKNOWN,tracker.captured(ball,pokemon,player,NOW));
        tracker.hit(ball,pokemon,player,NOW,true);assertEquals(CaptureRecord.Origin.NATIVE_PLAYER_SPAWN,tracker.captured(ball,pokemon,player,NOW.plusSeconds(10)));assertEquals(CaptureRecord.Origin.UNKNOWN,tracker.captured(ball,pokemon,player,NOW.plusSeconds(10)));
    }
    @Test void expiredClockReversedOrClearedEvidenceCannotProveACapture(){
        var tracker=new CaptureTracker();var ball=UUID.randomUUID();var pokemon=UUID.randomUUID();var player=UUID.randomUUID();
        for(var at:List.of(NOW.minusSeconds(1),NOW.plusSeconds(601))){tracker.hit(ball,pokemon,player,NOW,true);assertEquals(CaptureRecord.Origin.UNKNOWN,tracker.captured(ball,pokemon,player,at));}
        tracker.hit(ball,pokemon,player,NOW,true);tracker.clear();assertEquals(CaptureRecord.Origin.UNKNOWN,tracker.captured(ball,pokemon,player,NOW));
    }
    @Test void evidenceBufferIsBoundedWithoutKeepingMinecraftObjects(){
        var tracker=new CaptureTracker();var ball=UUID.randomUUID();var pokemon=UUID.randomUUID();var player=UUID.randomUUID();tracker.hit(ball,pokemon,player,NOW,true);
        for(int i=0;i<512;i++)tracker.hit(UUID.randomUUID(),pokemon,player,NOW,true);
        assertEquals(CaptureRecord.Origin.UNKNOWN,tracker.captured(ball,pokemon,player,NOW));
    }
    @Test void snapshotClassificationsAreImmutableAndIdsAreNotSpeciesOrTimeHeuristics(){
        var player=UUID.randomUUID();var labels=new HashSet<>(Set.of("legendary"));var value=record(player,UUID.randomUUID(),UUID.randomUUID(),NOW,true,labels,CaptureRecord.Origin.NATIVE_PLAYER_SPAWN);labels.clear();assertTrue(value.labels().contains("legendary"));assertThrows(UnsupportedOperationException.class,()->value.categories().clear());assertNotEquals(value.eventKey(),verified(player).eventKey());
    }
    @Test void duplicateBallAndPokemonNeverIncrementPermanentCountersTwice()throws Exception{
        try(var db=new Database(directory.resolve("capture.sqlite"))){await(db.initialize());var player=UUID.randomUUID();var value=verified(player);assertTrue(await(db.recordCapture(value)));assertFalse(await(db.recordCapture(value)));var other=record(UUID.randomUUID(),value.ball(),value.pokemon(),NOW,true,Set.of("legendary"),CaptureRecord.Origin.NATIVE_PLAYER_SPAWN);assertFalse(await(db.recordCapture(other)));assertEquals(1,await(db.captureSummary(player,NOW,ZoneId.of("UTC"))).total());assertEquals(0,await(db.captureSummary(other.player(),NOW,ZoneId.of("UTC"))).total());}
    }
    @Test void distinctCapturesOfSameSpeciesAtSameTimeCountIndividually()throws Exception{
        try(var db=new Database(directory.resolve("many.sqlite"))){await(db.initialize());var player=UUID.randomUUID();for(int i=0;i<10;i++)await(db.recordCapture(verified(player)));var summary=await(db.captureSummary(player,NOW,ZoneId.of("UTC")));assertEquals(10,summary.total());assertEquals(1,summary.distinctSpecies());assertEquals(10,await(db.captureCount(player,"SPECIES","cobblemon:pikachu")));assertEquals(10,await(db.captureCount(player,"CATEGORY","electric_captures")));}
    }
    @Test void unverifiedOriginIsAuditedWithoutBecomingLegitimateOrShinyStatistics()throws Exception{
        try(var db=new Database(directory.resolve("unknown.sqlite"))){await(db.initialize());var player=UUID.randomUUID();await(db.recordCapture(record(player,UUID.randomUUID(),UUID.randomUUID(),NOW,true,Set.of("legendary","mythical"),CaptureRecord.Origin.UNKNOWN)));var summary=await(db.captureSummary(player,NOW,ZoneId.of("UTC")));assertEquals(0,summary.total());assertEquals(0,summary.shiny());assertEquals(0,summary.legendary());assertEquals(0,summary.mythical());assertEquals(1,summary.unverified());assertEquals(0,await(db.captureCount(player,"CATEGORY","electric_captures")));}
    }
    @Test void shinyLegendaryMythicalAndCategoriesUseObservedSnapshotOnly()throws Exception{
        try(var db=new Database(directory.resolve("labels.sqlite"))){await(db.initialize());var player=UUID.randomUUID();await(db.recordCapture(record(player,UUID.randomUUID(),UUID.randomUUID(),NOW,true,Set.of("legendary","mythical"),CaptureRecord.Origin.NATIVE_PLAYER_SPAWN)));var summary=await(db.captureSummary(player,NOW,ZoneId.of("UTC")));assertEquals(1,summary.shiny());assertEquals(1,summary.legendary());assertEquals(1,summary.mythical());assertEquals(1,await(db.captureCount(player,"CATEGORY","electric_captures")));}
    }
    @Test void localDayIsoWeekAndMonthAreDerivedFromConfiguredZone()throws Exception{
        try(var db=new Database(directory.resolve("period.sqlite"))){await(db.initialize());var player=UUID.randomUUID();await(db.recordCapture(record(player,UUID.randomUUID(),UUID.randomUUID(),Instant.parse("2026-11-02T01:00:00Z"),false,Set.of(),CaptureRecord.Origin.NATIVE_PLAYER_SPAWN)));var utc=await(db.captureSummary(player,NOW,ZoneId.of("UTC")));var sao=await(db.captureSummary(player,NOW,ZoneId.of("America/Sao_Paulo")));assertEquals(1,utc.today());assertEquals(1,utc.week());assertEquals(0,sao.today());assertEquals(0,sao.week());assertEquals(1,sao.month());assertEquals(utc.total(),sao.total());}
    }
    @Test void dstBoundaryAndFutureTimestampsAreNotMistakenForTodaysCaptures()throws Exception{
        try(var db=new Database(directory.resolve("dst.sqlite"))){await(db.initialize());var player=UUID.randomUUID();var now=Instant.parse("2026-11-01T07:30:00Z");await(db.recordCapture(record(player,UUID.randomUUID(),UUID.randomUUID(),Instant.parse("2026-11-01T04:30:00Z"),false,Set.of(),CaptureRecord.Origin.NATIVE_PLAYER_SPAWN)));await(db.recordCapture(record(player,UUID.randomUUID(),UUID.randomUUID(),now.plusSeconds(3600),false,Set.of(),CaptureRecord.Origin.NATIVE_PLAYER_SPAWN)));assertEquals(1,await(db.captureSummary(player,now,ZoneId.of("America/New_York"))).today());}
    }
    @Test void restartPreservesDeduplicationAndPermanentCounters()throws Exception{
        var file=directory.resolve("restart.sqlite");var player=UUID.randomUUID();var value=verified(player);try(var db=new Database(file)){await(db.initialize());await(db.recordCapture(value));}
        try(var db=new Database(file)){await(db.initialize());assertFalse(await(db.recordCapture(value)));assertEquals(1,await(db.captureSummary(player,NOW,ZoneId.of("UTC"))).total());}
    }
    @Test void totalsSpeciesAndCategoryCountersAreIndependentOfDetailedRows()throws Exception{
        var file=directory.resolve("aggregate.sqlite");var player=UUID.randomUUID();try(var db=new Database(file)){await(db.initialize());await(db.recordCapture(verified(player)));}
        // Retention is not exposed by the product; this verifies future pruning cannot silently erase permanent aggregates.
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){q.executeUpdate("DELETE FROM capture_events");}
        try(var db=new Database(file)){await(db.initialize());assertEquals(1,await(db.captureSummary(player,NOW,ZoneId.of("UTC"))).total());assertEquals(1,await(db.captureCount(player,"SPECIES","cobblemon:pikachu")));assertEquals(1,await(db.captureCount(player,"CATEGORY","electric_captures")));}
    }
    @Test void timezoneParsingIsStrictAndDefaultsToUtc()throws Exception{
        String json="{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\"}";assertEquals(ZoneId.of("UTC"),ConfigManager.parse(json).statisticsTimezone());assertEquals(ZoneId.of("America/Sao_Paulo"),ConfigManager.parse(json.replace("\"menu_title\"","\"statistics_timezone\":\"America/Sao_Paulo\",\"menu_title\"")).statisticsTimezone());assertThrows(java.io.IOException.class,()->ConfigManager.parse(json.replace("\"menu_title\"","\"statistics_timezone\":\"not/a-zone\",\"menu_title\"")));
    }
    @Test void upgradeFromSchemaFivePreservesRewardPlansAndStartsWithNoInventedCaptures()throws Exception{
        var file=directory.resolve("legacy.sqlite");var player=UUID.randomUUID();var operation=UUID.randomUUID();String plan="{\"format_version\":1,\"id\":\"legacy\",\"collection\":\"national\",\"minimum_registered\":1,\"items\":[{\"type\":\"ITEM\",\"id\":\"minecraft:paper\",\"count\":1}]}";
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){
            var resources=List.of("V001__foundation.sql","V002__category_rules.sql","V003__reward_journal.sql","V004__reward_unlocks.sql","V005__saved_items.sql");
            for(int i=0;i<resources.size();i++){
                try(var input=getClass().getResourceAsStream("/database/"+resources.get(i))){for(String statement:new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split(";"))if(!statement.isBlank())q.execute(statement);}
                q.executeUpdate("INSERT INTO schema_migrations VALUES("+(i+1)+",'2026-01-01T00:00:00Z')");
            }
            q.execute("PRAGMA user_version=5");try(var insert=c.prepareStatement("INSERT INTO reward_operations VALUES(?,?,?,?,'PENDING','2026-01-01T00:00:00Z','2026-01-01T00:00:00Z')")){insert.setString(1,operation.toString());insert.setString(2,player.toString());insert.setString(3,"legacy");insert.setString(4,plan);insert.executeUpdate();}
        }
        try(var db=new Database(file)){await(db.initialize());assertEquals(plan,await(db.rewardOperation(operation)).orElseThrow().plan());assertEquals(0,await(db.captureSummary(player,NOW,ZoneId.of("UTC"))).total());}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var r=q.executeQuery("PRAGMA user_version")){r.next();assertEquals(Database.SCHEMA_VERSION,r.getInt(1));}
    }

}
