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

class CaptureReceiptsTest {
    @TempDir Path directory;
    private static final Instant AT = Instant.parse("2026-11-02T01:00:00Z");
    private static <T> T await(CompletableFuture<T> f) throws Exception { return f.get(10, TimeUnit.SECONDS); }
    private static CaptureRecord capture(UUID player, boolean verified) {
        return new CaptureRecord(UUID.randomUUID(), UUID.randomUUID(), player, "cobblemon:pikachu", "normal", true,
                Set.of("legendary"), Set.of("electric"), Set.of("electric"), "a".repeat(64), AT,
                "minecraft:overworld", 1, 64, 2, verified ? CaptureRecord.Origin.NATIVE_PLAYER_SPAWN : CaptureRecord.Origin.UNKNOWN);
    }
    private void sql(Path file, String... commands) throws Exception {
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var q = c.createStatement()) {
            for (var command : commands) q.execute(command);
        }
    }
    @Test void detailRemovalAndRestartPreserveTimezonesUnknownCountsAndReplayProtection() throws Exception {
        var file = directory.resolve("retained.sqlite"); var player = UUID.randomUUID();
        var known = capture(player, true); var unknown = capture(player, false); var now = AT.plusSeconds(10800);
        try (var db = new Database(file)) { await(db.initialize()); await(db.recordCapture(known)); await(db.recordCapture(unknown)); }
        sql(file, "DELETE FROM capture_events");
        try (var db = new Database(file)) {
            await(db.initialize()); assertFalse(await(db.recordCapture(known))); assertFalse(await(db.recordCapture(unknown)));
            assertTrue(await(db.captureHistory(player, 0)).entries().isEmpty());
            var utc = await(db.captureSummary(player, now, ZoneId.of("UTC")));
            var sao = await(db.captureSummary(player, now, ZoneId.of("America/Sao_Paulo")));
            assertEquals(1, utc.total()); assertEquals(1, utc.today()); assertEquals(1, utc.week()); assertEquals(1, utc.month());
            assertEquals(0, sao.today()); assertEquals(0, sao.week()); assertEquals(1, sao.month()); assertEquals(1, sao.unverified());
            assertEquals(1, await(db.captureCount(player, "SPECIES", "cobblemon:pikachu")));
            assertEquals(1, await(db.capturedShinyForms(player)).size());
            assertEquals(0, await(db.captureSummary(UUID.randomUUID(), now, ZoneId.of("UTC"))).today());
        }
    }
    @Test void nativeReplayAfterPruningCannotConsumeAnotherProofOrUpgradeUnknownOrigin() throws Exception {
        var file = directory.resolve("native.sqlite"); var player = UUID.randomUUID(); var record = capture(player, false);
        var entity = UUID.randomUUID();
        try (var db = new Database(file)) {
            await(db.initialize()); await(db.recordObservedCapture(new CaptureObservation(record, entity, CaptureObservation.Kind.NONE)));
            await(db.recordNativeSpawnProof(new NativeSpawnProof(record.pokemon(), entity, record.dimension(), AT.minusSeconds(1), NativeSpawnProof.Source.ENTITY_LOAD)));
        }
        sql(file, "DELETE FROM capture_events");
        try (var db = new Database(file)) {
            await(db.initialize()); assertFalse(await(db.recordObservedCapture(new CaptureObservation(record, entity, CaptureObservation.Kind.LIVE_NATIVE))));
            var summary = await(db.captureSummary(player, AT, ZoneId.of("UTC"))); assertEquals(0, summary.total()); assertEquals(1, summary.unverified());
        }
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var q = c.createStatement(); var rows = q.executeQuery("SELECT consumed_event FROM native_spawn_proofs")) { assertTrue(rows.next()); assertNull(rows.getString(1)); }
    }
    @Test void migrationNineBackfillsOnlyExistingEventsAndIsIdempotent() throws Exception {
        var file = directory.resolve("nine.sqlite"); var player = UUID.randomUUID(); var known = capture(player, true); var unknown = capture(player, false);
        try (var db = new Database(file)) { await(db.initialize()); await(db.recordCapture(known)); await(db.recordCapture(unknown)); }
        // Exact schema9 fixture: remove only migration10 objects/metadata, never a user database.
        sql(file, "DROP TABLE player_medal_favorites", "DROP TABLE player_achievements", "DROP TABLE achievement_audit", "DROP TABLE achievement_definitions", "DROP TABLE profile_collection_points", "DROP TABLE player_stat_privacy", "DROP TABLE player_preferences", "DROP TABLE captured_forms", "DELETE FROM schema_migrations WHERE version=14", "DROP TABLE competition_awards", "DELETE FROM schema_migrations WHERE version=13", "DROP TABLE competition_adjustments", "DROP TABLE competition_audit", "DROP TABLE competition_results", "DROP TABLE competition_points", "DROP TABLE competition_entries", "DROP TABLE competition_editions", "DELETE FROM schema_migrations WHERE version=12", "DROP TABLE capture_ranking_scores", "DROP TABLE player_ranking_privacy", "DROP TABLE capture_record_zones", "DROP TABLE capture_record_buckets", "DROP INDEX capture_history_expiry", "DELETE FROM schema_migrations WHERE version=11", "DROP TABLE capture_receipts", "DROP INDEX capture_history_player_time", "DELETE FROM schema_migrations WHERE version=10", "PRAGMA user_version=9");
        for (int i = 0; i < 2; i++) try (var db = new Database(file)) {
            await(db.initialize()); assertFalse(await(db.recordCapture(known))); assertFalse(await(db.recordCapture(unknown)));
            var summary = await(db.captureSummary(player, AT, ZoneId.of("UTC"))); assertEquals(1, summary.today()); assertEquals(1, summary.unverified()); assertEquals(1, summary.total());
        }
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var q = c.createStatement(); var rows = q.executeQuery("SELECT count(*) FROM capture_receipts")) { assertTrue(rows.next()); assertEquals(2, rows.getInt(1)); }
    }
    @Test void receiptAndNativeProofRollbackWhenDetailInsertFails() throws Exception {
        var file = directory.resolve("rollback.sqlite"); var record = capture(UUID.randomUUID(), false); var entity = UUID.randomUUID();
        try (var db = new Database(file)) { await(db.initialize()); await(db.recordNativeSpawnProof(new NativeSpawnProof(record.pokemon(), entity, record.dimension(), AT.minusSeconds(1), NativeSpawnProof.Source.PREHIT))); }
        // SQLite trigger fault in a disposable test DB. No production fault hooks.
        sql(file, "CREATE TRIGGER reject_detail BEFORE INSERT ON capture_events BEGIN SELECT RAISE(ABORT, 'test detail failure'); END");
        try (var db = new Database(file)) { await(db.initialize()); assertThrows(ExecutionException.class, () -> await(db.recordObservedCapture(new CaptureObservation(record, entity, CaptureObservation.Kind.LIVE_NATIVE)))); }
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var q = c.createStatement()) {
            try (var rows = q.executeQuery("SELECT count(*) FROM capture_receipts")) { assertTrue(rows.next()); assertEquals(0, rows.getInt(1)); }
            try (var rows = q.executeQuery("SELECT consumed_event FROM native_spawn_proofs")) { assertTrue(rows.next()); assertNull(rows.getString(1)); }
        }
        sql(file, "DROP TRIGGER reject_detail");
        try (var db = new Database(file)) { await(db.initialize()); assertTrue(await(db.recordObservedCapture(new CaptureObservation(record, entity, CaptureObservation.Kind.LIVE_NATIVE)))); assertEquals(1, await(db.captureSummary(record.player(), AT, ZoneId.of("UTC"))).total()); }
    }
    @Test void migrationConflictRollsBackWithoutDestroyingSchemaNineData() throws Exception {
        var file = directory.resolve("conflict.sqlite"); var known = capture(UUID.randomUUID(), true);
        try (var db = new Database(file)) { await(db.initialize()); await(db.recordCapture(known)); }
        sql(file, "DROP TABLE player_medal_favorites", "DROP TABLE player_achievements", "DROP TABLE achievement_audit", "DROP TABLE achievement_definitions", "DROP TABLE profile_collection_points", "DROP TABLE player_stat_privacy", "DROP TABLE player_preferences", "DROP TABLE captured_forms", "DELETE FROM schema_migrations WHERE version=14", "DROP TABLE competition_awards", "DELETE FROM schema_migrations WHERE version=13", "DROP TABLE competition_adjustments", "DROP TABLE competition_audit", "DROP TABLE competition_results", "DROP TABLE competition_points", "DROP TABLE competition_entries", "DROP TABLE competition_editions", "DELETE FROM schema_migrations WHERE version=12", "DROP TABLE capture_ranking_scores", "DROP TABLE player_ranking_privacy", "DROP TABLE capture_record_zones", "DROP TABLE capture_record_buckets", "DROP INDEX capture_history_expiry", "DELETE FROM schema_migrations WHERE version=11", "DROP TABLE capture_receipts", "DROP INDEX capture_history_player_time", "DELETE FROM schema_migrations WHERE version=10", "PRAGMA user_version=9", "CREATE INDEX capture_history_player_time ON capture_events(species)");
        try (var db = new Database(file)) { assertThrows(ExecutionException.class, () -> await(db.initialize())); }
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var q = c.createStatement()) {
            try (var rows = q.executeQuery("PRAGMA user_version")) { assertEquals(9, rows.getInt(1)); }
            try (var rows = q.executeQuery("SELECT count(*) FROM sqlite_master WHERE name='capture_receipts'")) { assertEquals(0, rows.getInt(1)); }
            try (var rows = q.executeQuery("SELECT count(*) FROM capture_events")) { assertEquals(1, rows.getInt(1)); }
        }
    }
}
