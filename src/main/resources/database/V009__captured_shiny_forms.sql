CREATE TABLE captured_shiny_forms (
    player_uuid TEXT NOT NULL,
    species TEXT NOT NULL,
    form TEXT NOT NULL,
    first_captured_at INTEGER NOT NULL,
    PRIMARY KEY(player_uuid,species,form)
);
INSERT INTO captured_shiny_forms
SELECT player_uuid,species,lower(form),min(captured_at) FROM capture_events
WHERE origin='NATIVE_PLAYER_SPAWN' AND shiny=1 GROUP BY player_uuid,species,lower(form);
