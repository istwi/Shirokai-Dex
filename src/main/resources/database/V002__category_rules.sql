CREATE TABLE category_rule_versions (
    fingerprint TEXT PRIMARY KEY NOT NULL CHECK(length(fingerprint) = 64),
    format_version INTEGER NOT NULL CHECK(format_version = 1),
    rules_json TEXT NOT NULL,
    first_observed_at TEXT NOT NULL
);
