package br.com.shirokaidex.captures;

import br.com.shirokaidex.database.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CaptureRecordsTest {
    @TempDir Path directory;
    private static <T>T await(CompletableFuture<T> f)throws Exception{return f.get(10,TimeUnit.SECONDS);}
    private CaptureRecord record(UUID player,String at,boolean verified){return new CaptureRecord(UUID.randomUUID(),UUID.randomUUID(),player,"mod:a","normal",false,Set.of(),Set.of(),Set.of(),"a".repeat(64),Instant.parse(at),"minecraft:overworld",0,64,0,verified?CaptureRecord.Origin.NATIVE_PLAYER_SPAWN:CaptureRecord.Origin.UNKNOWN);}
    @Test void initializedAndLazyZonesUseSameReceiptsWithTimezoneAndDeterministicTies()throws Exception{
        var player=UUID.randomUUID();try(var db=new Database(directory.resolve("zones.sqlite"))){await(db.initialize());assertEquals(0,await(db.captureRecords(player,ZoneId.of("UTC"))).getFirst().total());
            await(db.recordCapture(record(player,"2026-11-02T01:00:00Z",true)));await(db.recordCapture(record(player,"2026-11-02T04:00:00Z",true)));await(db.recordCapture(record(player,"2026-11-03T04:00:00Z",false)));
            var utc=await(db.captureRecords(player,ZoneId.of("UTC")));assertEquals(2,utc.getFirst().total());assertEquals("2026-11-02",utc.getFirst().bucket());
            var sao=await(db.captureRecords(player,ZoneId.of("America/Sao_Paulo")));assertEquals(1,sao.getFirst().total());assertEquals("2026-11-01",sao.getFirst().bucket());assertEquals(1,sao.get(1).total());assertEquals(2,sao.get(2).total());
            assertEquals(0,await(db.captureRecords(UUID.randomUUID(),ZoneId.of("UTC"))).getFirst().total());
        }
    }
    @Test void recordBucketsSurviveCleanupRestartAndContinueOncePerCapture()throws Exception{
        var file=directory.resolve("restart.sqlite");var player=UUID.randomUUID();var value=record(player,"2026-11-02T01:00:00Z",true);
        try(var db=new Database(file)){await(db.initialize());await(db.recordCapture(value));await(db.captureRecords(player,ZoneId.of("UTC")));}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){q.executeUpdate("DELETE FROM capture_events");}
        try(var db=new Database(file)){await(db.initialize());assertFalse(await(db.recordCapture(value)));assertEquals(1,await(db.captureRecords(player,ZoneId.of("UTC"))).getFirst().total());await(db.recordCapture(record(player,"2026-11-02T02:00:00Z",true)));assertEquals(2,await(db.captureRecords(player,ZoneId.of("UTC"))).getFirst().total());assertEquals(2,await(db.captureRecords(player,ZoneId.of("America/Sao_Paulo"))).get(2).total());}
    }
}
