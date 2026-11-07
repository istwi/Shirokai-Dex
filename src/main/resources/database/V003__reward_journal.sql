CREATE TABLE reward_operations (
    operation_id TEXT PRIMARY KEY NOT NULL,
    player_uuid TEXT NOT NULL,
    reward_id TEXT NOT NULL,
    plan_json TEXT NOT NULL,
    state TEXT NOT NULL CHECK(state IN ('PENDING','PROCESSING','NEEDS_REVIEW','COMPLETED','CANCELLED')),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    UNIQUE(player_uuid,reward_id)
);
CREATE TABLE reward_steps (
    operation_id TEXT NOT NULL REFERENCES reward_operations(operation_id),
    step_index INTEGER NOT NULL CHECK(step_index >= 0),
    state TEXT NOT NULL CHECK(state IN ('INTENT','COMPLETED')),
    evidence TEXT NOT NULL,
    PRIMARY KEY(operation_id,step_index)
);
CREATE TABLE reward_audit (
    event_id INTEGER PRIMARY KEY,
    operation_id TEXT NOT NULL REFERENCES reward_operations(operation_id),
    action TEXT NOT NULL,
    actor TEXT NOT NULL,
    reason TEXT NOT NULL,
    recorded_at TEXT NOT NULL
);
