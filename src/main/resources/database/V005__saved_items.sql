CREATE TABLE saved_items (
    id TEXT PRIMARY KEY NOT NULL,
    item_id TEXT NOT NULL,
    item_count INTEGER NOT NULL CHECK(item_count BETWEEN 1 AND 2304),
    payload TEXT NOT NULL,
    checksum TEXT NOT NULL,
    updated_by TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
CREATE TABLE saved_item_audit (
    event_id INTEGER PRIMARY KEY,
    item_id TEXT NOT NULL,
    action TEXT NOT NULL CHECK(action IN ('SAVE','DELETE')),
    actor TEXT NOT NULL,
    previous_payload TEXT,
    new_payload TEXT,
    recorded_at TEXT NOT NULL
);
