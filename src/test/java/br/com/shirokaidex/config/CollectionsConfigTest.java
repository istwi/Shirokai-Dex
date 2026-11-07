package br.com.shirokaidex.config;

import br.com.shirokaidex.pokedex.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CollectionsConfigTest {
    @TempDir Path directory;
    private static String json(String definitions) {
        return "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"collections\":" + definitions + "}";
    }
    private static CollectionValidator registry() {
        var form = new ProgressCalculator.Form("normal", Set.of("legendary"), Set.of("normal"), false, false, false);
        return new CollectionValidator(List.of(new ProgressCalculator.Species("test:one", false, false, List.of(form))),
                Set.of("minecraft:book", "minecraft:amethyst_shard", "minecraft:nether_star", "minecraft:paper"));
    }
    @Test void oldConfigurationRetainsDefaultCollectionsWithoutRewritingFile() throws Exception {
        String old = "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Old\"}";
        Files.writeString(directory.resolve("config.json"), old);
        var manager = new ConfigManager(directory); manager.setValidator(registry()::validate); manager.initialize();
        assertEquals(CollectionDefinition.defaults(), manager.current().collections());
        assertEquals(old, Files.readString(directory.resolve("config.json")));
    }
    @Test void customOnlyCollectionAndAllSupportedModesAreExplicit() throws Exception {
        for (var mode : CollectionDefinition.Mode.values()) {
            var settings = ConfigManager.parse(json("[{\"id\":\"custom\",\"name\":\"Custom\",\"progress_mode\":\"" + mode + "\",\"icon\":\"minecraft:paper\",\"permission\":\"dex.custom\",\"criteria\":{\"include_species\":[\"test:one\"],\"labels\":[\"legendary\"],\"types\":[\"normal\"],\"forms\":[\"normal\"]}}]"));
            registry().validate(settings);
            assertEquals(mode, settings.collections().getFirst().mode());
        }
    }
    @ParameterizedTest @ValueSource(strings = {
        "[]", "null", "{}", "[{\"id\":\"custom\"}]",
        "[{\"id\":\"status\",\"name\":\"Reserved\"}]",
        "[{\"id\":\"Uppercase\",\"name\":\"Bad\"}]",
        "[{\"id\":\"national\",\"id\":\"national\"}]",
        "[{\"id\":\"national\"},{\"id\":\"national\"}]",
        "[{\"id\":\"national\",\"progress_mode\":\"FORMS_OWNED\"}]",
        "[{\"id\":\"national\",\"permission\":true}]",
        "[{\"id\":\"national\",\"permission\":\"admin;op\"}]",
        "[{\"id\":\"national\",\"icon\":\"minecraft:air\"}]",
        "[{\"id\":\"national\",\"unknown\":1}]",
        "[{\"id\":\"national\",\"criteria\":{\"unknown\":[]}}]",
        "[{\"id\":\"national\",\"criteria\":{\"types\":[\"normal\",\"normal\"]}}]",
        "[{\"id\":\"national\",\"criteria\":{\"types\":[5]}}]",
        "[{\"id\":\"national\",\"criteria\":{\"types\":[\"Normal\"]}}]",
        "[{\"id\":\"national\",\"criteria\":{\"include_species\":[\"pikachu\"]}}]"
    }) void rejectsMalformedOrAmbiguousDefinitions(String definitions) {
        assertThrows(IOException.class, () -> ConfigManager.parse(json(definitions)));
    }
    @ParameterizedTest @ValueSource(strings = {
        "\"icon\":\"test:missing\"",
        "\"criteria\":{\"include_species\":[\"test:missing\"]}",
        "\"criteria\":{\"exclude_species\":[\"test:missing\"]}",
        "\"criteria\":{\"labels\":[\"made_up_label\"]}",
        "\"criteria\":{\"types\":[\"made_up_type\"]}",
        "\"criteria\":{\"forms\":[\"made_up_form\"]}"
    }) void rejectsUnknownReferencesAgainstImmutableRegistry(String field) throws Exception {
        var settings = ConfigManager.parse(json("[{\"id\":\"national\"," + field + "}]"));
        IOException error = assertThrows(IOException.class, () -> registry().validate(settings));
        assertTrue(error.getMessage().contains("collections.national"));
    }
    @Test void invalidCollectionRejectsWholeReloadAndPreservesBackup() throws Exception {
        var manager = new ConfigManager(directory); manager.setValidator(registry()::validate); manager.initialize();
        Settings before = manager.current(); String backup = Files.readString(directory.resolve("config.last-good.json"));
        Files.writeString(directory.resolve("config.json"), json("[{\"id\":\"custom\",\"name\":\"Changed\",\"icon\":\"test:missing\"}]"));
        assertThrows(IOException.class, manager::reload);
        assertSame(before, manager.current()); assertEquals(backup, Files.readString(directory.resolve("config.last-good.json")));
    }
    @Test void validReloadAppliesCollectionsAndSettingsTogether() throws Exception {
        var manager = new ConfigManager(directory); manager.setValidator(registry()::validate); manager.initialize();
        Files.writeString(directory.resolve("config.json"), json("[{\"id\":\"custom\",\"name\":\"Only custom\"}]"));
        manager.reload();
        assertEquals("Dex", manager.current().menuTitle()); assertEquals(1, manager.current().collections().size());
        assertEquals("custom", manager.current().collections().getFirst().id());
    }
    @Test void recoveryBackupAlsoRequiresSemanticValidation() throws Exception {
        String custom = json("[{\"id\":\"custom\",\"name\":\"One\",\"criteria\":{\"include_species\":[\"test:one\"]}}]");
        Files.writeString(directory.resolve("config.json"), custom);
        var first = new ConfigManager(directory); first.setValidator(registry()::validate); first.initialize();
        var next = new ConfigManager(directory);
        next.setValidator(new CollectionValidator(List.of(), Set.of("minecraft:book"))::validate);
        assertThrows(IOException.class, next::initialize);
        assertThrows(IllegalStateException.class, next::current);
        assertEquals(custom, Files.readString(directory.resolve("config.last-good.json")));
    }
    @Test void collectionLimitRejectsUnboundedConfiguration() {
        String definitions = java.util.stream.IntStream.range(0, 65).mapToObj(i -> "{\"id\":\"collection_" + i + "\",\"name\":\"Custom\"}").collect(java.util.stream.Collectors.joining(",", "[", "]"));
        assertThrows(IOException.class, () -> ConfigManager.parse(json(definitions)));
    }
    @Test void malformedUtf8RetainsPreviousSnapshot() throws Exception {
        var manager = new ConfigManager(directory); manager.initialize(); Settings before = manager.current();
        Files.write(directory.resolve("config.json"), new byte[]{(byte) 0xff});
        assertThrows(IOException.class, manager::reload); assertSame(before, manager.current());
    }
}
