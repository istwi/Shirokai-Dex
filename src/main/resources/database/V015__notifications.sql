CREATE TABLE notification_outbox (
    sequence INTEGER PRIMARY KEY AUTOINCREMENT,
    event_key TEXT NOT NULL UNIQUE,
    target TEXT NOT NULL,
    event TEXT NOT NULL,
    value TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    consumed_at INTEGER
);
CREATE INDEX notification_pending ON notification_outbox(target,sequence) WHERE consumed_at IS NULL;
CREATE TABLE notification_progress (
    player_uuid TEXT NOT NULL,
    collection_id TEXT NOT NULL,
    version TEXT NOT NULL,
    tier INTEGER NOT NULL CHECK(tier BETWEEN 0 AND 4),
    PRIMARY KEY(player_uuid,collection_id)
);
