CREATE TABLE schema_migrations (version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL);
CREATE TABLE players (
    uuid TEXT PRIMARY KEY NOT NULL,
    last_known_name TEXT NOT NULL,
    first_join_at TEXT NOT NULL,
    last_seen_at TEXT NOT NULL
);
CREATE TABLE player_settings (
    player_uuid TEXT PRIMARY KEY NOT NULL REFERENCES players(uuid),
    language TEXT CHECK (language IS NULL OR language IN ('pt_br', 'en_us'))
);
