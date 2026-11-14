ALTER TABLE competition_editions ADD COLUMN reward_templates TEXT NOT NULL DEFAULT '{}';
CREATE TABLE competition_awards (
    award_id TEXT PRIMARY KEY NOT NULL,
    edition_id TEXT NOT NULL REFERENCES competition_editions(edition_id),
    player_uuid TEXT NOT NULL,
    source_position INTEGER NOT NULL,
    band INTEGER NOT NULL CHECK(band IN (1,3,10)),
    reward_id TEXT NOT NULL,
    plan_snapshot TEXT NOT NULL,
    assigned_at INTEGER NOT NULL,
    expires_at INTEGER,
    UNIQUE(edition_id,player_uuid)
);
CREATE INDEX competition_awards_player ON competition_awards(player_uuid,assigned_at DESC);
