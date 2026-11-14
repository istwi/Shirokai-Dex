CREATE TABLE capture_receipts (
    event_key TEXT PRIMARY KEY NOT NULL,
    player_uuid TEXT NOT NULL,
    captured_at INTEGER NOT NULL,
    verified INTEGER NOT NULL CHECK(verified IN (0,1))
);
INSERT INTO capture_receipts(event_key,player_uuid,captured_at,verified)
SELECT event_key,player_uuid,captured_at,CASE WHEN origin='NATIVE_PLAYER_SPAWN' THEN 1 ELSE 0 END FROM capture_events;
CREATE INDEX capture_receipts_player_time ON capture_receipts(player_uuid,verified,captured_at);
CREATE INDEX capture_history_player_time ON capture_events(player_uuid,captured_at DESC,event_key ASC);
