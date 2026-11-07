package br.com.shirokaidex.database;

import br.com.shirokaidex.categories.CategoryRules;
import br.com.shirokaidex.config.*;
import java.nio.file.*;
import java.sql.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CategoryRulesDatabaseTest {
    @TempDir Path directory;
    private static CategoryRules rules(String type) throws Exception {
        return CategoryRules.of(CategoriesCodec.parse("[{\"id\":\"typed\",\"criteria\":{\"types\":[\""+type+"\"]}}]"));
    }
    @Test void upgradesVersionOnePreservesDataAndRetainsOldRulesAcrossRestart() throws Exception {
        Path file = directory.resolve("dex.sqlite");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:"+file); Statement s = c.createStatement()) {
            String sql = new String(getClass().getResourceAsStream("/database/V001__foundation.sql").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String part : sql.split(";")) if (!part.isBlank()) s.execute(part);
            s.execute("INSERT INTO schema_migrations VALUES (1, 'original')");
            s.execute("INSERT INTO players VALUES ('preserved','Original','first','last')"); s.execute("PRAGMA user_version=1");
        }
        var old = rules("water"); var latest = rules("fire");
        try (var db = new Database(file)) {
            db.initialize().get(10,TimeUnit.SECONDS);
            db.preserveCategoryRules(old).get(10,TimeUnit.SECONDS);
            db.preserveCategoryRules(old).get(10,TimeUnit.SECONDS);
            db.preserveCategoryRules(latest).get(10,TimeUnit.SECONDS);
        }
        try (var db = new Database(file)) {
            db.initialize().get(10,TimeUnit.SECONDS); db.preserveCategoryRules(old).get(10,TimeUnit.SECONDS);
            assertEquals(old, db.categoryRules(old.fingerprint()).get(10,TimeUnit.SECONDS).orElseThrow());
            assertEquals(latest, db.categoryRules(latest.fingerprint()).get(10,TimeUnit.SECONDS).orElseThrow());
            assertTrue(db.categoryRules("0".repeat(64)).get(10,TimeUnit.SECONDS).isEmpty());
            assertThrows(ExecutionException.class, () -> db.categoryRules("invalid").get(10,TimeUnit.SECONDS));
        }
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:"+file); Statement s = c.createStatement()) {
            try (ResultSet rs=s.executeQuery("SELECT count(*) FROM category_rule_versions")) { assertEquals(2,rs.getInt(1)); }
            try (ResultSet rs=s.executeQuery("SELECT rules_json FROM category_rule_versions WHERE fingerprint='"+old.fingerprint()+"'")) { assertEquals(old.json(),rs.getString(1)); }
            try (ResultSet rs=s.executeQuery("SELECT last_known_name FROM players WHERE uuid='preserved'")) { assertEquals("Original",rs.getString(1)); }
            try (ResultSet rs=s.executeQuery("PRAGMA integrity_check")) { assertEquals("ok",rs.getString(1)); }
        }
    }
    @Test void secondMigrationFailurePreservesVersionOneAndItsData() throws Exception {
        Path file=directory.resolve("conflict.sqlite");
        try (Connection c=DriverManager.getConnection("jdbc:sqlite:"+file); Statement s=c.createStatement()) {
            s.execute("CREATE TABLE schema_migrations(version INTEGER PRIMARY KEY, applied_at TEXT)");
            s.execute("INSERT INTO schema_migrations VALUES(1,'original')"); s.execute("PRAGMA user_version=1");
            s.execute("CREATE TABLE category_rule_versions(existing_data TEXT)"); s.execute("INSERT INTO category_rule_versions VALUES('keep')");
        }
        try (var db=new Database(file)) { assertThrows(ExecutionException.class,()->db.initialize().get(10,TimeUnit.SECONDS)); }
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file); Statement s=c.createStatement()) {
            try(ResultSet rs=s.executeQuery("PRAGMA user_version")) { assertEquals(1,rs.getInt(1)); }
            try(ResultSet rs=s.executeQuery("SELECT existing_data FROM category_rule_versions")) { assertEquals("keep",rs.getString(1)); }
        }
    }
    @Test void persistenceFailurePreventsConfigPublicationAndBackupReplacement() throws Exception {
        Path file=directory.resolve("fail.sqlite");
        var db=new Database(file);db.initialize().get(10,TimeUnit.SECONDS);
        var config=new ConfigManager(directory);
        config.setValidator(settings->{
            try { db.preserveCategoryRules(CategoryRules.of(settings.categories())).join(); }
            catch(CompletionException failure) { throw new java.io.IOException("Cannot preserve rules",failure); }
        });
        config.initialize();var before=config.current();String backup=Files.readString(directory.resolve("config.last-good.json"));
        db.close();
        Files.writeString(directory.resolve("config.json"),"{\"schema_version\":1,\"language\":\"en_us\",\"menu_title\":\"Changed\"}");
        assertThrows(java.io.IOException.class,config::reload);
        assertSame(before,config.current());assertEquals(backup,Files.readString(directory.resolve("config.last-good.json")));
    }
    @Test void corruptedStoredContentIsRefusedWithoutOverwritingIt() throws Exception {
        Path file=directory.resolve("tampered.sqlite");var rules=rules("water");
        try(var db=new Database(file)) {
            db.initialize().get(10,TimeUnit.SECONDS);db.preserveCategoryRules(rules).get(10,TimeUnit.SECONDS);
            try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file); Statement s=c.createStatement()) {
                s.executeUpdate("UPDATE category_rule_versions SET rules_json='tampered'");
            }
            assertThrows(ExecutionException.class,()->db.categoryRules(rules.fingerprint()).get(10,TimeUnit.SECONDS));
            assertThrows(ExecutionException.class,()->db.preserveCategoryRules(rules).get(10,TimeUnit.SECONDS));
        }
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT rules_json FROM category_rule_versions")) {
            assertEquals("tampered",rs.getString(1));
        }
    }
}
