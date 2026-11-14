CREATE TABLE defeat_receipts (
    battle_uuid TEXT NOT NULL,
    pokemon_uuid TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    species TEXT NOT NULL,
    defeated_at INTEGER NOT NULL,
    PRIMARY KEY(battle_uuid,pokemon_uuid)
);
CREATE TABLE defeat_totals (
    player_uuid TEXT PRIMARY KEY NOT NULL,
    total INTEGER NOT NULL CHECK(total>0),
    reached_at INTEGER NOT NULL
);
CREATE INDEX defeat_ranking ON defeat_totals(total DESC,reached_at,player_uuid);
