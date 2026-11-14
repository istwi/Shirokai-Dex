package br.com.shirokaidex.captures;

import br.com.shirokaidex.config.ConfigManager;
import br.com.shirokaidex.database.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CaptureRetentionTest {
    @TempDir Path directory;
    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
    private static <T> T await(CompletableFuture<T> f) throws Exception { return f.get(10, TimeUnit.SECONDS); }
    private CaptureRecord capture(UUID player, Instant time, boolean verified) {
        return new CaptureRecord(UUID.randomUUID(), UUID.randomUUID(), player, "mod:a", "normal", true,
                Set.of(), Set.of(), Set.of(), "a".repeat(64), time, "minecraft:overworld", 0, 64, 0,
                verified ? CaptureRecord.Origin.NATIVE_PLAYER_SPAWN : CaptureRecord.Origin.UNKNOWN);
    }
    private String config(String fields) { return "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"capture_history\":" + fields + "}"; }
    @Test void cleanupIsBoundedStrictlyBeforeCutoffAndPreservesUnknownAuditAndReceipts() throws Exception {
        var file = directory.resolve("cleanup.sqlite"); var player = UUID.randomUUID(); var cutoff = NOW.minus(Duration.ofDays(90));
        var old = capture(player, cutoff.minusMillis(1), true);
        try (var db = new Database(file)) {
            await(db.initialize()); await(db.recordCapture(old)); await(db.recordCapture(capture(player, cutoff.minusSeconds(1), true)));
            await(db.recordCapture(capture(player, cutoff, true))); await(db.recordCapture(capture(player, cutoff.minusSeconds(10), false)));
            var policy = new CaptureRetention(true, 90, true, 1);
            assertEquals(1, await(db.cleanupCaptureHistory(policy, NOW))); assertEquals(1, await(db.cleanupCaptureHistory(policy, NOW)));
            assertEquals(0, await(db.cleanupCaptureHistory(policy, NOW))); assertEquals(2, await(db.captureHistory(player, 0)).entries().size());
            assertFalse(await(db.recordCapture(old))); assertEquals(3, await(db.captureSummary(player, NOW, ZoneId.of("UTC"))).total());
            assertEquals(1, await(db.captureSummary(player, NOW, ZoneId.of("UTC"))).unverified());
        }
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var q = c.createStatement(); var rows = q.executeQuery("SELECT count(*) FROM capture_receipts")) { assertEquals(4, rows.getInt(1)); }
    }
    @Test void disabledCleanupLeavesOldDetailsAndMismatchedReceiptsAreNeverPruned() throws Exception {
        var file = directory.resolve("disabled.sqlite"); var record = capture(UUID.randomUUID(), NOW.minus(Duration.ofDays(100)), true);
        try (var db = new Database(file)) { await(db.initialize()); await(db.recordCapture(record)); assertEquals(0, await(db.cleanupCaptureHistory(new CaptureRetention(true, 90, false, 1000), NOW))); }
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var q = c.createStatement()) { q.executeUpdate("UPDATE capture_receipts SET captured_at=0"); }
        try (var db = new Database(file)) { await(db.initialize()); assertEquals(0, await(db.cleanupCaptureHistory(CaptureRetention.defaults(), NOW))); assertEquals(1, await(db.captureHistory(record.player(), 0)).entries().size()); }
    }
    @Test void disablingHistoryKeepsDedupStatisticsAndUnknownAudit() throws Exception {
        var file = directory.resolve("off.sqlite"); var player = UUID.randomUUID(); var known = capture(player, NOW, false); var entity = UUID.randomUUID();
        try (var db = new Database(file)) {
            await(db.initialize()); await(db.recordNativeSpawnProof(new NativeSpawnProof(known.pokemon(), entity, known.dimension(), NOW.minusSeconds(1), NativeSpawnProof.Source.PREHIT)));
            var observation = new CaptureObservation(known, entity, CaptureObservation.Kind.LIVE_NATIVE);
            assertTrue(await(db.recordObservedCapture(observation, false))); assertFalse(await(db.recordObservedCapture(observation, false)));
            assertTrue(await(db.captureHistory(player, 0)).entries().isEmpty()); assertEquals(1, await(db.captureSummary(player, NOW, ZoneId.of("UTC"))).today());
            assertEquals(1, await(db.capturedShinyForms(player)).size());
            var unknown = capture(player, NOW, false);
            await(db.recordObservedCapture(new CaptureObservation(unknown, null, CaptureObservation.Kind.NONE), false));
            assertEquals("UNKNOWN", await(db.captureHistory(player, 0)).entries().getFirst().origin());
        }
    }
    @Test void configurationUsesDefaultsAndStrictTypesLimitsAndDuplicateChecks() throws Exception {
        assertEquals(CaptureRetention.defaults(), ConfigManager.parse(config("{}")).captureHistory());
        assertEquals(new CaptureRetention(false, 30, false, 20), ConfigManager.parse(config("{\"enabled\":false,\"retention_days\":30,\"cleanup\":{\"enabled\":false,\"batch_size\":20}}")).captureHistory());
        for (var invalid : List.of("{\"enabled\":\"false\"}", "{\"retention_days\":0}", "{\"retention_days\":1.5}", "{\"retention_days\":36501}", "{\"cleanup\":{\"batch_size\":10001}}", "{\"cleanup\":{\"batch_size\":0}}", "{\"extra\":true}", "{\"enabled\":true,\"enabled\":false}", "{\"cleanup\":{\"enabled\":true,\"enabled\":false}}"))
            assertThrows(java.io.IOException.class, () -> ConfigManager.parse(config(invalid)), invalid);
    }
}
