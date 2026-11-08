package br.com.shirokaidex.database;

import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {
    @TempDir Path directory;
    @Test void migrationReopeningAndConcurrentLoginsPreserveIdentityAndFirstJoin() throws Exception {
        Path file = directory.resolve("dex.sqlite"); UUID uuid = UUID.randomUUID();
        try (var database = new Database(file)) {
            database.initialize().get(10, TimeUnit.SECONDS);
            database.recordLogin(uuid, "First", Instant.parse("2026-01-01T00:00:00Z")).get(10, TimeUnit.SECONDS);
            List<CompletableFuture<Void>> writes = new ArrayList<>();
            for (int i = 0; i < 100; i++) writes.add(database.recordLogin(uuid, "Updated", Instant.parse("2026-01-02T00:00:00Z")));
            CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
        }
        try (var reopened = new Database(file)) { reopened.initialize().get(10, TimeUnit.SECONDS); }
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file); Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT * FROM players")) {
                assertTrue(rs.next()); assertEquals(uuid.toString(), rs.getString("uuid"));
                assertEquals("Updated", rs.getString("last_known_name"));
                assertEquals("2026-01-01T00:00:00Z", rs.getString("first_join_at"));
                assertEquals("2026-01-02T00:00:00Z", rs.getString("last_seen_at")); assertFalse(rs.next());
            }
            try (ResultSet rs = s.executeQuery("SELECT count(*) FROM schema_migrations")) { assertEquals(6, rs.getInt(1)); }
        }
    }
    @Test void futureSchemaIsRefusedWithoutDowngrade() throws Exception {
        Path file = directory.resolve("newer.sqlite");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file); Statement s = c.createStatement()) { s.execute("PRAGMA user_version=99"); }
        try (var db = new Database(file)) { assertThrows(ExecutionException.class, () -> db.initialize().get(10, TimeUnit.SECONDS)); }
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("PRAGMA user_version")) { assertEquals(99, rs.getInt(1)); }
    }
    @Test void migrationFailureRollsBackCreatedTables() throws Exception {
        Path file = directory.resolve("conflict.sqlite");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file); Statement s = c.createStatement()) { s.execute("CREATE TABLE players(existing_data TEXT)"); s.execute("INSERT INTO players VALUES ('preserved')"); }
        try (var db = new Database(file)) { assertThrows(ExecutionException.class, () -> db.initialize().get(10, TimeUnit.SECONDS)); }
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file); Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT existing_data FROM players")) { assertEquals("preserved", rs.getString(1)); }
            try (ResultSet rs = s.executeQuery("SELECT count(*) FROM sqlite_master WHERE name='schema_migrations'")) { assertEquals(0, rs.getInt(1)); }
            try (ResultSet rs = s.executeQuery("PRAGMA user_version")) { assertEquals(0, rs.getInt(1)); }
        }
    }
    @Test void closedDatabaseRejectsNewOperations() {
        var db = new Database(directory.resolve("closed.sqlite")); db.close();
        assertThrows(ExecutionException.class, () -> db.recordLogin(UUID.randomUUID(), "Player", Instant.now()).get(10, TimeUnit.SECONDS));
    }
}
