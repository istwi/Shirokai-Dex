CREATE TABLE player_stat_privacy (
    player_uuid TEXT NOT NULL,
    statistic TEXT NOT NULL,
    visible INTEGER NOT NULL CHECK(visible IN (0,1)),
    PRIMARY KEY(player_uuid,statistic)
);
CREATE TABLE profile_collection_points (
    player_uuid TEXT NOT NULL,
    collection_id TEXT NOT NULL,
    definition_version TEXT NOT NULL,
    registered INTEGER,
    total INTEGER NOT NULL CHECK(total>=0),
    ratio TEXT,
    reached_at INTEGER,
    observed_at INTEGER NOT NULL,
    PRIMARY KEY(player_uuid,collection_id),
    CHECK(registered IS NULL OR (registered>=0 AND registered<=total))
);
CREATE INDEX profile_collection_versions ON profile_collection_points(collection_id,definition_version);
CREATE TABLE captured_forms (
    player_uuid TEXT NOT NULL,
    species TEXT NOT NULL,
    form TEXT NOT NULL,
    first_captured_at INTEGER NOT NULL,
    shiny INTEGER NOT NULL CHECK(shiny IN (0,1)),
    PRIMARY KEY(player_uuid,species,form)
);
INSERT INTO captured_forms SELECT player_uuid,species,lower(form),min(captured_at),max(shiny) FROM capture_events WHERE origin='NATIVE_PLAYER_SPAWN' GROUP BY player_uuid,species,lower(form);
INSERT INTO captured_forms SELECT player_uuid,species,form,first_captured_at,1 FROM captured_shiny_forms WHERE 1 ON CONFLICT(player_uuid,species,form) DO UPDATE SET shiny=1,first_captured_at=min(first_captured_at,excluded.first_captured_at);
CREATE TABLE achievement_definitions (
    achievement_id TEXT PRIMARY KEY NOT NULL,
    activated_at INTEGER NOT NULL,
    fingerprint TEXT NOT NULL,
    definition_json TEXT NOT NULL
);
CREATE TABLE player_achievements (
    player_uuid TEXT NOT NULL,
    achievement_id TEXT NOT NULL REFERENCES achievement_definitions(achievement_id),
    active INTEGER NOT NULL CHECK(active IN (0,1)),
    unlocked_at INTEGER,
    updated_at INTEGER NOT NULL,
    definition_json TEXT NOT NULL,
    baseline_json TEXT,
    PRIMARY KEY(player_uuid,achievement_id)
);
CREATE TABLE player_medal_favorites (
    player_uuid TEXT NOT NULL,
    slot INTEGER NOT NULL CHECK(slot BETWEEN 0 AND 2),
    achievement_id TEXT NOT NULL,
    PRIMARY KEY(player_uuid,slot),
    UNIQUE(player_uuid,achievement_id),
    FOREIGN KEY(player_uuid,achievement_id) REFERENCES player_achievements(player_uuid,achievement_id)
);
CREATE TABLE player_preferences (
    player_uuid TEXT PRIMARY KEY NOT NULL,
    language TEXT CHECK(language IN ('pt_br','en_us')),
    notifications INTEGER NOT NULL DEFAULT 1 CHECK(notifications IN (0,1))
);
CREATE TABLE achievement_audit (
    sequence INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    achievement_id TEXT NOT NULL,
    action TEXT NOT NULL,
    definition_fingerprint TEXT NOT NULL,
    evidence_json TEXT NOT NULL,
    recorded_at INTEGER NOT NULL
);
