CREATE TABLE capture_events (
    event_key TEXT PRIMARY KEY NOT NULL,
    ball_uuid TEXT NOT NULL,
    pokemon_uuid TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    species TEXT NOT NULL,
    form TEXT NOT NULL,
    shiny INTEGER NOT NULL CHECK(shiny IN (0,1)),
    labels_json TEXT NOT NULL,
    types_json TEXT NOT NULL,
    categories_json TEXT NOT NULL,
    category_rules TEXT NOT NULL,
    captured_at INTEGER NOT NULL,
    dimension TEXT NOT NULL,
    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,
    origin TEXT NOT NULL CHECK(origin IN ('NATIVE_PLAYER_SPAWN','UNKNOWN')),
    UNIQUE(ball_uuid,pokemon_uuid)
);
CREATE INDEX capture_events_player_time ON capture_events(player_uuid,origin,captured_at);
CREATE TABLE capture_totals (
    player_uuid TEXT PRIMARY KEY NOT NULL,
    total INTEGER NOT NULL CHECK(total>=0),
    shiny INTEGER NOT NULL CHECK(shiny>=0),
    legendary INTEGER NOT NULL CHECK(legendary>=0),
    mythical INTEGER NOT NULL CHECK(mythical>=0)
);
CREATE TABLE capture_counts (
    player_uuid TEXT NOT NULL,
    kind TEXT NOT NULL CHECK(kind IN ('SPECIES','CATEGORY')),
    entry_key TEXT NOT NULL,
    total INTEGER NOT NULL CHECK(total>=0),
    PRIMARY KEY(player_uuid,kind,entry_key)
);
