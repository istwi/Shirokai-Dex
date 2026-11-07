CREATE TABLE reward_unlocks (
    player_uuid TEXT NOT NULL,
    reward_id TEXT NOT NULL,
    plan_json TEXT NOT NULL,
    observed_registered INTEGER NOT NULL CHECK(observed_registered >= 0),
    observed_total INTEGER NOT NULL CHECK(observed_total > 0 AND observed_registered <= observed_total),
    unlocked_at TEXT NOT NULL,
    PRIMARY KEY(player_uuid,reward_id)
);
