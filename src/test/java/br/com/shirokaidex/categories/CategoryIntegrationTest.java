package br.com.shirokaidex.categories;

import br.com.shirokaidex.config.*;
import br.com.shirokaidex.pokedex.ProgressCalculator;
import br.com.shirokaidex.wiki.Wiki;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CategoryIntegrationTest {
    @TempDir Path directory;
    private static String config(String rules) {
        // References before definitions must work, regardless of JSON field order.
        return """
            {"schema_version":1,"language":"pt_br","menu_title":"Dex",
             "collections":[{"id":"custom","name":"Custom","category":"target"}],
             "categories":%s}
            """.formatted(rules);
    }
    private static String rules(String type) {
        return "[{\"id\":\"target\",\"criteria\":{\"types\":[\"" + type + "\"]}}]";
    }
    private static ProgressCalculator.Species species() {
        return new ProgressCalculator.Species("test:one", true, true, List.of(
                new ProgressCalculator.Form("normal", Set.of(), Set.of("fire"), true, true, false),
                new ProgressCalculator.Form("alternate", Set.of(), Set.of("water"), false, false, false)));
    }
    @Test void collectionsAndWikiShareScopeWithoutLeakingOwnedStandardForm() throws Exception {
        var settings = ConfigManager.parse(config(rules("water")));
        var definition = settings.collections().getFirst();
        var progress = ProgressCalculator.calculate(List.of(species(), species()), settings.collections()).get("custom");
        assertEquals(1, progress.progress().total()); assertEquals(0, progress.progress().registered().orElseThrow());
        var page = Wiki.select(List.of(new Wiki.Entry("One", species())), definition, Wiki.Request.initial(""), 18);
        assertEquals(1, page.total()); assertFalse(page.rows().getFirst().owned());
        assertEquals("alternate", page.rows().getFirst().forms().getFirst().name());
    }
    @Test void reloadPublishesWholeCatalogAndInvalidCandidateKeepsSnapshotAndBackup() throws Exception {
        Files.writeString(directory.resolve("config.json"), config(rules("fire")));
        var manager = new ConfigManager(directory);
        manager.setValidator(new CollectionValidator(List.of(species()), Set.of("minecraft:book"))::validate);
        manager.initialize(); var original = manager.current();
        String backup = Files.readString(directory.resolve("config.last-good.json"));
        for (String invalid : List.of(rules("unknown"), "[]", "[{\"id\":\"target\",\"all_of\":[\"target\"]}]")) {
            Files.writeString(directory.resolve("config.json"), config(invalid));
            assertThrows(IOException.class, manager::reload);
            assertSame(original, manager.current());
            assertEquals(backup, Files.readString(directory.resolve("config.last-good.json")));
        }
        Files.writeString(directory.resolve("config.json"), config(rules("water")));
        manager.reload(); assertNotSame(original, manager.current());
        assertEquals(0, ProgressCalculator.calculate(List.of(species()), manager.current().collections())
                .get("custom").progress().registered().orElseThrow());
        // Old immutable snapshots keep their old rules, including after a successful reload.
        assertEquals(1, ProgressCalculator.calculate(List.of(species()), original.collections())
                .get("custom").progress().registered().orElseThrow());
    }
    @Test void backupRecoveryValidatesCategoriesAgainstCurrentRegistry() throws Exception {
        Files.writeString(directory.resolve("config.json"), config(rules("fire")));
        var manager = new ConfigManager(directory); manager.initialize();
        Files.writeString(directory.resolve("config.json"), "invalid");
        var restart = new ConfigManager(directory);
        restart.setValidator(new CollectionValidator(List.of(), Set.of("minecraft:book"))::validate);
        assertThrows(IOException.class, restart::initialize);
        assertThrows(IllegalStateException.class, restart::current);
    }
}
