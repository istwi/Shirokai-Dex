package br.com.shirokaidex.captures;

import br.com.shirokaidex.database.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class NativeProvenanceTest {
    @TempDir Path directory;
    private static final Instant AT=Instant.parse("2026-11-07T12:00:00Z");
    private static <T>T await(CompletableFuture<T> f)throws Exception{return f.get(8,TimeUnit.SECONDS);}
    private static NativeSpawnProof proof(UUID pokemon,UUID entity){return new NativeSpawnProof(pokemon,entity,"minecraft:overworld",AT,NativeSpawnProof.Source.ENTITY_LOAD);}
    private static CaptureRecord capture(UUID player,UUID pokemon){return new CaptureRecord(UUID.randomUUID(),pokemon,player,"cobblemon:pikachu","normal",true,Set.of(),Set.of("electric"),Set.of("electric"),"a".repeat(64),AT.plusSeconds(5),"minecraft:overworld",1,64,2,CaptureRecord.Origin.UNKNOWN);}
    @Test void persistedPhysicalAndLogicalIdentityRestoresMissingCauseAfterRestart()throws Exception{
        var file=directory.resolve("proof.sqlite");var pokemon=UUID.randomUUID();var entity=UUID.randomUUID();var player=UUID.randomUUID();
        try(var db=new Database(file)){await(db.initialize());assertTrue(await(db.recordNativeSpawnProof(proof(pokemon,entity))));}
        try(var db=new Database(file)){await(db.initialize());assertTrue(await(db.recordObservedCapture(new CaptureObservation(capture(player,pokemon),entity,CaptureObservation.Kind.MISSING_CAUSE))));assertEquals(1,await(db.captureSummary(player,AT.plusSeconds(10),ZoneId.of("UTC"))).total());}
        try(var c=java.sql.DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var r=q.executeQuery("select entity_uuid,verification from capture_events")){r.next();assertEquals(entity.toString(),r.getString(1));assertEquals("PERSISTED_NATIVE",r.getString(2));}
    }
    @Test void liveFlagCannotInventProofThatWasNeverPersisted()throws Exception{
        try(var db=new Database(directory.resolve("none.sqlite"))){await(db.initialize());var player=UUID.randomUUID();var record=capture(player,UUID.randomUUID()).withOrigin(CaptureRecord.Origin.NATIVE_PLAYER_SPAWN);await(db.recordObservedCapture(new CaptureObservation(record,UUID.randomUUID(),CaptureObservation.Kind.LIVE_NATIVE)));assertEquals(0,await(db.captureSummary(player,AT.plusSeconds(10),ZoneId.of("UTC"))).total());}
    }

    @Test void copiedPokemonIdentityOnAnotherEntityDoesNotReuseProof()throws Exception{
        try(var db=new Database(directory.resolve("identity.sqlite"))){await(db.initialize());var pokemon=UUID.randomUUID();var entity=UUID.randomUUID();var player=UUID.randomUUID();await(db.recordNativeSpawnProof(proof(pokemon,entity)));
            await(db.recordObservedCapture(new CaptureObservation(capture(player,pokemon),UUID.randomUUID(),CaptureObservation.Kind.MISSING_CAUSE)));
            await(db.recordObservedCapture(new CaptureObservation(capture(player,UUID.randomUUID()),entity,CaptureObservation.Kind.MISSING_CAUSE)));
            assertEquals(0,await(db.captureSummary(player,AT.plusSeconds(10),ZoneId.of("UTC"))).total());
        }
    }
    @Test void missingPrehitCannotUseProofAndStillRetiresItAfterCompletion()throws Exception{
        try(var db=new Database(directory.resolve("correlation.sqlite"))){await(db.initialize());var pokemon=UUID.randomUUID();var entity=UUID.randomUUID();var player=UUID.randomUUID();await(db.recordNativeSpawnProof(proof(pokemon,entity)));
            await(db.recordObservedCapture(new CaptureObservation(capture(player,pokemon),entity,CaptureObservation.Kind.NONE)));
            await(db.recordObservedCapture(new CaptureObservation(capture(player,pokemon),entity,CaptureObservation.Kind.MISSING_CAUSE)));
            assertEquals(0,await(db.captureSummary(player,AT.plusSeconds(10),ZoneId.of("UTC"))).total());
        }
    }
    @Test void consumedProofCannotBeResetByLoadOrPrehitObservation()throws Exception{
        try(var db=new Database(directory.resolve("consumed.sqlite"))){await(db.initialize());var pokemon=UUID.randomUUID();var entity=UUID.randomUUID();var player=UUID.randomUUID();await(db.recordNativeSpawnProof(proof(pokemon,entity)));
            await(db.recordObservedCapture(new CaptureObservation(capture(player,pokemon),entity,CaptureObservation.Kind.LIVE_NATIVE)));
            assertFalse(await(db.recordNativeSpawnProof(new NativeSpawnProof(pokemon,entity,"minecraft:overworld",AT.plusSeconds(4),NativeSpawnProof.Source.PREHIT))));
            await(db.recordObservedCapture(new CaptureObservation(capture(player,pokemon),entity,CaptureObservation.Kind.MISSING_CAUSE)));
            assertEquals(1,await(db.captureSummary(player,AT.plusSeconds(10),ZoneId.of("UTC"))).total());
        }
    }
    @Test void duplicateDoesNotRewriteClassificationOrConsumeAnotherProof()throws Exception{
        var file=directory.resolve("duplicate.sqlite");try(var db=new Database(file)){await(db.initialize());var pokemon=UUID.randomUUID();var entity=UUID.randomUUID();var player=UUID.randomUUID();var record=capture(player,pokemon);await(db.recordNativeSpawnProof(proof(pokemon,entity)));
            assertTrue(await(db.recordObservedCapture(new CaptureObservation(record,entity,CaptureObservation.Kind.LIVE_NATIVE))));assertFalse(await(db.recordObservedCapture(new CaptureObservation(record,entity,CaptureObservation.Kind.NONE))));assertEquals(1,await(db.captureSummary(player,AT.plusSeconds(10),ZoneId.of("UTC"))).total());
        }
        try(var c=java.sql.DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var r=q.executeQuery("select verification from capture_events")){r.next();assertEquals("LIVE_NATIVE",r.getString(1));}
    }
    @Test void unknownOldEventIsNeverUpgradedWhenEvidenceAppearsLater()throws Exception{
        try(var db=new Database(directory.resolve("history.sqlite"))){await(db.initialize());var pokemon=UUID.randomUUID();var entity=UUID.randomUUID();var player=UUID.randomUUID();var record=capture(player,pokemon);
            await(db.recordObservedCapture(new CaptureObservation(record,entity,CaptureObservation.Kind.MISSING_CAUSE)));await(db.recordNativeSpawnProof(proof(pokemon,entity)));
            assertFalse(await(db.recordObservedCapture(new CaptureObservation(record,entity,CaptureObservation.Kind.MISSING_CAUSE))));assertEquals(0,await(db.captureSummary(player,AT.plusSeconds(10),ZoneId.of("UTC"))).total());
        }
    }
    @Test void proofWithLaterObservationTimeCannotValidateAnEarlierCapture()throws Exception{
        try(var db=new Database(directory.resolve("clock.sqlite"))){await(db.initialize());var pokemon=UUID.randomUUID();var entity=UUID.randomUUID();var player=UUID.randomUUID();await(db.recordNativeSpawnProof(new NativeSpawnProof(pokemon,entity,"minecraft:overworld",AT.plusSeconds(30),NativeSpawnProof.Source.ENTITY_LOAD)));
            await(db.recordObservedCapture(new CaptureObservation(capture(player,pokemon),entity,CaptureObservation.Kind.MISSING_CAUSE)));assertEquals(0,await(db.captureSummary(player,AT.plusSeconds(60),ZoneId.of("UTC"))).total());
        }
    }
    @Test void upgradeFromSixKeepsLegacyTotalsAndUnknownClassifications()throws Exception{
        var file=directory.resolve("legacy-six.sqlite");var player=UUID.randomUUID();var pokemon=UUID.randomUUID();var entity=UUID.randomUUID();var old=capture(player,pokemon);
        try(var c=java.sql.DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){
            var resources=List.of("V001__foundation.sql","V002__category_rules.sql","V003__reward_journal.sql","V004__reward_unlocks.sql","V005__saved_items.sql","V006__captures.sql");
            for(int i=0;i<resources.size();i++){
                try(var input=getClass().getResourceAsStream("/database/"+resources.get(i))){for(String statement:new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split(";"))if(!statement.isBlank())q.execute(statement);}
                q.executeUpdate("INSERT INTO schema_migrations VALUES("+(i+1)+",'2026-01-01T00:00:00Z')");
            }
            q.execute("PRAGMA user_version=6");c.setAutoCommit(false);CaptureLedger.record(c,old);CaptureLedger.record(c,capture(player,UUID.randomUUID()).withOrigin(CaptureRecord.Origin.NATIVE_PLAYER_SPAWN));c.commit();
        }
        try(var db=new Database(file)){await(db.initialize());await(db.recordNativeSpawnProof(proof(pokemon,entity)));assertFalse(await(db.recordObservedCapture(new CaptureObservation(old,entity,CaptureObservation.Kind.MISSING_CAUSE))));var summary=await(db.captureSummary(player,AT.plusSeconds(10),ZoneId.of("UTC")));assertEquals(1,summary.total());assertEquals(1,summary.unverified());}
        try(var c=java.sql.DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var r=q.executeQuery("select verification from capture_events")){while(r.next())assertEquals("LEGACY",r.getString(1));}
    }

}
