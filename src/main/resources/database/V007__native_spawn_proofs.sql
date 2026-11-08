ALTER TABLE capture_events ADD COLUMN entity_uuid TEXT;
ALTER TABLE capture_events ADD COLUMN verification TEXT NOT NULL DEFAULT 'LEGACY' CHECK(verification IN ('LEGACY','LIVE_NATIVE','PERSISTED_NATIVE','UNVERIFIED'));
CREATE TABLE native_spawn_proofs (
    pokemon_uuid TEXT NOT NULL,
    entity_uuid TEXT NOT NULL,
    dimension TEXT NOT NULL,
    observed_at INTEGER NOT NULL,
    observed_via TEXT NOT NULL CHECK(observed_via IN ('ENTITY_LOAD','PREHIT')),
    consumed_event TEXT UNIQUE,
    PRIMARY KEY(pokemon_uuid,entity_uuid)
);
