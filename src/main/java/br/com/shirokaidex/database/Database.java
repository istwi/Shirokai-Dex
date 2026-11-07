package br.com.shirokaidex.database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

/** The connection and all SQL are confined to one worker. No Minecraft objects cross this boundary. */
public final class Database implements AutoCloseable {
    private final Path file;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "shirokaidex-sqlite"));
    private Connection connection;
    private boolean closed;
    private CompletableFuture<Void> initialization;

    public Database(Path file) { this.file = file; }
    public synchronized CompletableFuture<Void> initialize() {
        if (initialization != null) return initialization;
        initialization = submit(() -> {
            Files.createDirectories(file.toAbsolutePath().getParent());
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys=ON");
                    statement.execute("PRAGMA busy_timeout=5000");
                    statement.execute("PRAGMA journal_mode=WAL");
                    statement.execute("PRAGMA synchronous=FULL");
                }
                migrate();
            } catch (Exception failure) { connection.close(); connection = null; throw failure; }
            return null;
        });
        return initialization;
    }

    private void migrate() throws Exception {
        int version;
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("PRAGMA user_version")) { version = rs.getInt(1); }
        if (version > 1) throw new SQLException("Database version " + version + " is newer than supported version 1");
        if (version == 1) {
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT version FROM schema_migrations WHERE version=1")) {
                if (!rs.next()) throw new SQLException("Missing migration history for version 1");
            }
            return;
        }
        String sql;
        try (var input = Database.class.getResourceAsStream("/database/V001__foundation.sql")) {
            if (input == null) throw new IOException("Migration V001 is missing");
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                for (String part : sql.split(";")) if (!part.isBlank()) statement.execute(part);
                statement.execute("PRAGMA user_version=1");
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO schema_migrations VALUES (1, ?)")) {
                statement.setString(1, Instant.now().toString()); statement.executeUpdate();
            }
            connection.commit();
        } catch (Exception failure) { connection.rollback(); throw failure; }
        finally { connection.setAutoCommit(true); }
    }

    public CompletableFuture<Void> recordLogin(UUID player, String name, Instant time) {
        return submit(() -> {
            if (connection == null) throw new SQLException("Database is not initialized");
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO players(uuid,last_known_name,first_join_at,last_seen_at) VALUES(?,?,?,?)
                    ON CONFLICT(uuid) DO UPDATE SET last_known_name=excluded.last_known_name,last_seen_at=excluded.last_seen_at
                    """)) {
                statement.setString(1, player.toString()); statement.setString(2, name);
                statement.setString(3, time.toString()); statement.setString(4, time.toString()); statement.executeUpdate();
            }
            return null;
        });
    }

    private synchronized <T> CompletableFuture<T> submit(Callable<T> action) {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Database is closed"));
        return CompletableFuture.supplyAsync(() -> {
            try { return action.call(); }
            catch (Exception failure) { throw new CompletionException(failure); }
        }, worker);
    }

    @Override public void close() {
        Future<?> closing;
        synchronized (this) {
            if (closed) return;
            closing = worker.submit(() -> {
                if (connection != null) {
                    try { connection.close(); }
                    catch (SQLException failure) { throw new CompletionException(failure); }
                    finally { connection = null; }
                }
            });
            closed = true;
            worker.shutdown();
        }
        try {
            closing.get(15, TimeUnit.SECONDS);
            if (!worker.awaitTermination(15, TimeUnit.SECONDS)) throw new IllegalStateException("Database shutdown timed out");
        } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException("Database shutdown interrupted", failure); }
        catch (ExecutionException | TimeoutException failure) { throw new IllegalStateException("Database close failed", failure); }
    }
}
