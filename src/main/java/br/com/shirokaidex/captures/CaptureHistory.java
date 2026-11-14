package br.com.shirokaidex.captures;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Read-only, bounded history queries. Execute on the database worker only. */
public final class CaptureHistory {
    private CaptureHistory() { }
    public static final int PAGE_SIZE = 10;
    public record Entry(String eventKey, String species, String form, boolean shiny,
                        Instant capturedAt, String origin, String source, String dimension) { }
    public record Page(List<Entry> entries, boolean hasMore) {
        public Page { entries = List.copyOf(entries); }
    }
    public static Page read(Connection connection, UUID player, int page) throws SQLException {
        Objects.requireNonNull(player);
        if (page < 0 || page > 100_000) throw new IllegalArgumentException("History page out of range");
        var entries = new ArrayList<Entry>();
        try (var query = connection.prepareStatement("SELECT event_key,species,form,shiny,captured_at,origin,native_source,dimension FROM capture_events WHERE player_uuid=? ORDER BY captured_at DESC,event_key ASC LIMIT ? OFFSET ?")) {
            query.setString(1, player.toString());
            query.setInt(2, PAGE_SIZE + 1);
            query.setInt(3, page * PAGE_SIZE);
            try (var rows = query.executeQuery()) {
                while (rows.next()) entries.add(new Entry(rows.getString(1), rows.getString(2), rows.getString(3),
                        rows.getBoolean(4), Instant.ofEpochMilli(rows.getLong(5)), rows.getString(6), rows.getString(7), rows.getString(8)));
            }
        }
        boolean more = entries.size() > PAGE_SIZE;
        if (more) entries.removeLast();
        return new Page(entries, more);
    }
}
