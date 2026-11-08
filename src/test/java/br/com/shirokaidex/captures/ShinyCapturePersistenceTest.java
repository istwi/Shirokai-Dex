package br.com.shirokaidex.captures;

import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.pokedex.CapturedShinyForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ShinyCapturePersistenceTest {
    @TempDir Path directory;
    private static <T>T await(CompletableFuture<T> f)throws Exception{return f.get(8,TimeUnit.SECONDS);}
    private static CaptureRecord capture(UUID player,String form,boolean shiny,boolean verified){return new CaptureRecord(UUID.randomUUID(),UUID.randomUUID(),player,"mod:a",form,shiny,Set.of(),Set.of(),Set.of(),"a".repeat(64),Instant.parse("2026-11-08T00:00:00Z"),"minecraft:overworld",1,64,2,verified?CaptureRecord.Origin.NATIVE_PLAYER_SPAWN:CaptureRecord.Origin.UNKNOWN);}
    @Test void onlyVerifiedShinyFormsEnterPermanentProjectionAndDuplicatesDoNotChangeIt()throws Exception{
        var player=UUID.randomUUID();try(var db=new Database(directory.resolve("forms.sqlite"))){await(db.initialize());var record=capture(player,"Normal",true,true);assertTrue(await(db.recordCapture(record)));assertFalse(await(db.recordCapture(record)));await(db.recordCapture(capture(player,"alola",false,true)));await(db.recordCapture(capture(player,"galar",true,false)));assertEquals(Set.of(new CapturedShinyForm("mod:a","normal")),await(db.capturedShinyForms(player)));assertTrue(await(db.capturedShinyForms(UUID.randomUUID())).isEmpty());}
    }
    @Test void distinctFormsPersistAfterRestartAndDetailedHistoryRemoval()throws Exception{
        var file=directory.resolve("permanent.sqlite");var player=UUID.randomUUID();try(var db=new Database(file)){await(db.initialize());await(db.recordCapture(capture(player,"normal",true,true)));await(db.recordCapture(capture(player,"alola",true,true)));}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){q.executeUpdate("DELETE FROM capture_events");}
        try(var db=new Database(file)){await(db.initialize());assertEquals(Set.of(new CapturedShinyForm("mod:a","normal"),new CapturedShinyForm("mod:a","alola")),await(db.capturedShinyForms(player)));}
    }
    @Test void migrationEightBackfillsOnlyRecordedVerifiedShinyEvents()throws Exception{
        var file=directory.resolve("eight.sqlite");var player=UUID.randomUUID();try(var db=new Database(file)){await(db.initialize());await(db.recordCapture(capture(player,"normal",true,true)));await(db.recordCapture(capture(player,"alola",false,true)));await(db.recordCapture(capture(player,"galar",true,false)));}
        // Restore the exact schema8 fixture (V009 only adds this projection); never downgrade a user database.
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){q.execute("DROP TABLE captured_shiny_forms");q.execute("DELETE FROM schema_migrations WHERE version=9");q.execute("PRAGMA user_version=8");}
        try(var db=new Database(file)){await(db.initialize());assertEquals(Set.of(new CapturedShinyForm("mod:a","normal")),await(db.capturedShinyForms(player)));}
    }
}
