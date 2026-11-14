package br.com.shirokaidex.captures;

import br.com.shirokaidex.database.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CaptureHistoryTest {
    @TempDir Path directory;
    private static <T> T await(CompletableFuture<T> value) throws Exception { return value.get(10, TimeUnit.SECONDS); }
    private CaptureRecord record(UUID player, Instant time, boolean verified) {
        return new CaptureRecord(UUID.randomUUID(), UUID.randomUUID(), player, "mod:species", "normal", true,
                Set.of(), Set.of(), Set.of(), "a".repeat(64), time, "minecraft:overworld", 1, 64, 2,
                verified ? CaptureRecord.Origin.NATIVE_PLAYER_SPAWN : CaptureRecord.Origin.UNKNOWN);
    }
    @Test void boundedStablePagesIncludeUnknownAndIsolatePlayersWithoutChangingProgress() throws Exception {
        var player = UUID.randomUUID(); var other = UUID.randomUUID(); var time = Instant.parse("2026-11-14T00:00:00Z");
        try (var db = new Database(directory.resolve("history.sqlite"))) {
            await(db.initialize()); var keys = new ArrayList<String>();
            for (int i = 0; i < 21; i++) { var capture = record(player, time, i % 2 == 0); keys.add(capture.eventKey()); await(db.recordCapture(capture)); }
            await(db.recordCapture(record(other, time.plusSeconds(1), true))); keys.sort(String::compareTo);
            var actual = new ArrayList<String>();
            for (int page = 0; page < 3; page++) {
                var result = await(db.captureHistory(player, page));
                assertEquals(page < 2, result.hasMore()); assertEquals(page < 2 ? 10 : 1, result.entries().size());
                actual.addAll(result.entries().stream().map(CaptureHistory.Entry::eventKey).toList());
                assertThrows(UnsupportedOperationException.class, () -> result.entries().clear());
            }
            assertEquals(keys, actual); assertTrue(await(db.captureHistory(player, 3)).entries().isEmpty());
            assertEquals(1, await(db.captureHistory(other, 0)).entries().size());
            var summary = await(db.captureSummary(player, time, java.time.ZoneOffset.UTC));
            assertEquals(11, summary.total()); assertEquals(10, summary.unverified());
            assertThrows(ExecutionException.class, () -> await(db.captureHistory(player, -1)));
            assertThrows(ExecutionException.class, () -> await(db.captureHistory(player, 100001)));
        }
    }
    @Test void newestFirstAndHistorySurvivesRestart() throws Exception {
        var player = UUID.randomUUID(); var time = Instant.parse("2026-11-14T00:00:00Z"); var file = directory.resolve("restart.sqlite");
        try (var db = new Database(file)) { await(db.initialize()); await(db.recordCapture(record(player, time, true))); await(db.recordCapture(record(player, time.plusMillis(1), false))); }
        try (var db = new Database(file)) {
            await(db.initialize()); var rows = await(db.captureHistory(player, 0)).entries();
            assertEquals(time.plusMillis(1), rows.get(0).capturedAt()); assertEquals("UNKNOWN", rows.get(0).origin());
            assertEquals(time, rows.get(1).capturedAt()); assertEquals("LEGACY", rows.get(1).source());
        }
    }
}
