CREATE TABLE competition_editions (
    edition_id TEXT PRIMARY KEY NOT NULL,
    competition_id TEXT NOT NULL,
    starts_at INTEGER NOT NULL,
    ends_at INTEGER NOT NULL CHECK(ends_at>starts_at),
    review_until INTEGER NOT NULL,
    state TEXT NOT NULL CHECK(state IN ('ACTIVE','REVIEW','FINAL')),
    definition_json TEXT NOT NULL,
    rules_json TEXT NOT NULL,
    rules_fingerprint TEXT NOT NULL,
    finalized_at INTEGER,
    UNIQUE(competition_id,starts_at)
);
CREATE INDEX competition_editions_state ON competition_editions(state,ends_at,review_until);
CREATE TABLE competition_entries (
    edition_id TEXT NOT NULL REFERENCES competition_editions(edition_id),
    player_uuid TEXT NOT NULL,
    joined_at INTEGER NOT NULL,
    cancelled_at INTEGER,
    score INTEGER NOT NULL DEFAULT 0 CHECK(score>=0),
    reached_at INTEGER,
    PRIMARY KEY(edition_id,player_uuid)
);
CREATE TABLE competition_points (
    edition_id TEXT NOT NULL REFERENCES competition_editions(edition_id),
    event_key TEXT NOT NULL REFERENCES capture_receipts(event_key),
    player_uuid TEXT NOT NULL,
    points INTEGER NOT NULL CHECK(points>=0),
    state TEXT NOT NULL CHECK(state IN ('COUNTED','HELD','REJECTED')),
    rule_fingerprint TEXT NOT NULL,
    recorded_at INTEGER NOT NULL,
    PRIMARY KEY(edition_id,event_key)
);
CREATE TABLE competition_audit (
    sequence INTEGER PRIMARY KEY AUTOINCREMENT,
    edition_id TEXT NOT NULL REFERENCES competition_editions(edition_id),
    player_uuid TEXT,
    event_key TEXT,
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    reason TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
CREATE TABLE competition_results (
    edition_id TEXT NOT NULL REFERENCES competition_editions(edition_id),
    player_uuid TEXT NOT NULL,
    position INTEGER NOT NULL,
    score INTEGER NOT NULL,
    reached_at INTEGER,
    eligible INTEGER NOT NULL CHECK(eligible IN (0,1)),
    PRIMARY KEY(edition_id,player_uuid),
    UNIQUE(edition_id,position)
);
CREATE INDEX competition_points_pending ON competition_points(edition_id,state,event_key);
CREATE INDEX competition_points_event ON competition_points(event_key,edition_id,state);
CREATE INDEX competition_audit_event ON competition_audit(event_key);
CREATE INDEX competition_entries_player ON competition_entries(player_uuid,edition_id,cancelled_at,joined_at);
CREATE INDEX competition_editions_latest ON competition_editions(competition_id,ends_at DESC);
CREATE TABLE competition_adjustments (
    adjustment_id TEXT PRIMARY KEY NOT NULL,
    edition_id TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    delta INTEGER NOT NULL CHECK(delta<>0),
    reverses TEXT UNIQUE REFERENCES competition_adjustments(adjustment_id),
    actor TEXT NOT NULL,
    reason TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(edition_id,player_uuid) REFERENCES competition_entries(edition_id,player_uuid)
);
