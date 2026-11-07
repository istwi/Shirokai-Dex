package br.com.shirokaidex.config;

import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {
    @TempDir Path directory;
    private static final String VALID = "{\"schema_version\":1,\"language\":\"en_us\",\"menu_title\":\"Dex\"}";

    @Test void invalidReloadRetainsCurrentAndRecoveryCopy() throws Exception {
        var manager = new ConfigManager(directory);
        manager.initialize();
        Files.writeString(directory.resolve("config.json"), VALID);
        manager.reload();
        String backup = Files.readString(directory.resolve("config.last-good.json"));
        Files.writeString(directory.resolve("config.json"), "{broken");
        assertThrows(IOException.class, manager::reload);
        assertEquals("en_us", manager.current().language());
        assertEquals(backup, Files.readString(directory.resolve("config.last-good.json")));
        var restarted = new ConfigManager(directory);
        restarted.initialize();
        assertTrue(restarted.recoveredFromBackup());
        assertEquals("en_us", restarted.current().language());
        assertEquals("{broken", Files.readString(directory.resolve("config.json")));
    }
    @Test void invalidFirstConfigurationDoesNotInventValidSettings() throws Exception {
        Files.writeString(directory.resolve("config.json"), "{}");
        assertThrows(IOException.class, () -> new ConfigManager(directory).initialize());
        assertFalse(Files.exists(directory.resolve("config.last-good.json")));
    }
    @Test void oversizedConfigurationRetainsLastValid() throws Exception {
        var manager = new ConfigManager(directory); manager.initialize();
        Files.writeString(directory.resolve("config.json"), " ".repeat(65537));
        assertThrows(IOException.class, manager::reload);
        assertEquals(Settings.defaults(), manager.current());
    }
    @ParameterizedTest @ValueSource(strings = {
        "{}", "null", "[]", "{broken", "{\"schema_version\":1,}",
        "{\"schema_version\":1,\"language\":\"pt_br\",\"language\":\"en_us\",\"menu_title\":\"Dex\"}",
        "{\"schema_version\":\"1\",\"language\":\"pt_br\",\"menu_title\":\"Dex\"}",
        "{\"schema_version\":1.0,\"language\":\"pt_br\",\"menu_title\":\"Dex\"}",
        "{\"schema_version\":1,\"language\":true,\"menu_title\":\"Dex\"}",
        "{\"schema_version\":1,\"language\":\"fr_fr\",\"menu_title\":\"Dex\"}",
        "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"\"}",
        "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"unknown\":1}"
    }) void rejectsMalformedDuplicateMissingUnknownAndWrongTypes(String json) {
        assertThrows(IOException.class, () -> ConfigManager.parse(json));
    }
    @Test void rejectsTrailingDocument() {
        assertThrows(IOException.class, () -> ConfigManager.parse(VALID + " {}"));
    }
}
