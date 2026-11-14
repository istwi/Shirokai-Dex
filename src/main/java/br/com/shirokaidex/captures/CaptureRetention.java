package br.com.shirokaidex.captures;

import java.sql.*;
import java.time.*;

/** Only detailed verified history is disposable. Receipts, unknown-origin audit and aggregates survive. */
public record CaptureRetention(boolean enabled, int retentionDays, boolean cleanupEnabled, int batchSize) {
    public CaptureRetention {
        if (retentionDays < 1 || retentionDays > 36500 || batchSize < 1 || batchSize > 10000)
            throw new IllegalArgumentException("Invalid capture retention limits");
    }
    public static CaptureRetention defaults() { return new CaptureRetention(true, 90, true, 1000); }
    public int cleanup(Connection connection, Instant now) throws SQLException {
        if (!cleanupEnabled) return 0;
        long cutoff = now.minus(Duration.ofDays(retentionDays)).toEpochMilli();
        try (var query = connection.prepareStatement("DELETE FROM capture_events WHERE event_key IN (SELECT e.event_key FROM capture_events e JOIN capture_receipts r ON e.event_key=r.event_key WHERE e.origin='NATIVE_PLAYER_SPAWN' AND r.verified=1 AND r.player_uuid=e.player_uuid AND r.captured_at=e.captured_at AND e.captured_at<? AND NOT EXISTS (SELECT 1 FROM competition_points p JOIN competition_editions d ON d.edition_id=p.edition_id WHERE p.event_key=e.event_key AND (d.state<>'FINAL' OR p.state='HELD')) AND NOT EXISTS (SELECT 1 FROM competition_audit a WHERE a.event_key=e.event_key) ORDER BY e.captured_at,e.event_key LIMIT ?)")) {
            query.setLong(1, cutoff); query.setInt(2, batchSize); return query.executeUpdate();
        }
    }
}
