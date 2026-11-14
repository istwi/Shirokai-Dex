CREATE TABLE capture_ranking_scores (
    player_uuid TEXT NOT NULL,
    metric TEXT NOT NULL,
    entry_key TEXT NOT NULL,
    score INTEGER NOT NULL CHECK(score>=0),
    reached_at INTEGER,
    PRIMARY KEY(player_uuid,metric,entry_key)
);
CREATE INDEX capture_ranking_order ON capture_ranking_scores(metric,entry_key,score DESC,reached_at,player_uuid);
CREATE TABLE player_ranking_privacy (
    player_uuid TEXT PRIMARY KEY NOT NULL,
    hidden INTEGER NOT NULL CHECK(hidden IN (0,1))
);
CREATE TABLE capture_record_zones (
    player_uuid TEXT NOT NULL,
    timezone TEXT NOT NULL,
    PRIMARY KEY(player_uuid,timezone)
);
CREATE TABLE capture_record_buckets (
    player_uuid TEXT NOT NULL,
    timezone TEXT NOT NULL,
    period TEXT NOT NULL CHECK(period IN ('DAY','WEEK','MONTH')),
    bucket TEXT NOT NULL,
    total INTEGER NOT NULL CHECK(total>0),
    PRIMARY KEY(player_uuid,timezone,period,bucket)
);
CREATE INDEX capture_record_best ON capture_record_buckets(player_uuid,timezone,period,total DESC,bucket);
CREATE INDEX capture_history_expiry ON capture_events(origin,captured_at,event_key);
