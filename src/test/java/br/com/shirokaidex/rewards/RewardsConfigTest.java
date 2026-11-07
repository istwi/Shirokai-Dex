package br.com.shirokaidex.rewards;

import br.com.shirokaidex.config.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class RewardsConfigTest {
    @TempDir Path directory;
    private static String config(String rewards){return "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"rewards\":"+rewards+"}";}
    private static String reward(String threshold){return "[{\"id\":\"first\",\"collection\":\"national\","+threshold+",\"items\":[{\"id\":\"minecraft:diamond\",\"count\":2}]}]";}
    @Test void oldConfigHasNoRewardsAndExplicitModesParse()throws Exception{
        assertTrue(Settings.defaults().rewards().isEmpty());
        assertEquals(1,ConfigManager.parse(config(reward("\"minimum_registered\":1"))).rewards().getFirst().minimumRegistered());
        assertEquals("33.34",ConfigManager.parse(config(reward("\"minimum_percent\":33.34"))).rewards().getFirst().minimumPercent().toPlainString());
    }
    @ParameterizedTest @ValueSource(strings={
        "[] []","null","[{\"id\":\"missing\"}]",
        "[{\"id\":\"x\",\"id\":\"x\"}]",
        "[{\"id\":\"x\",\"unknown\":1}]"
    }) void malformedDefinitionsAreRejected(String rewards){assertThrows(java.io.IOException.class,()->ConfigManager.parse(config(rewards)));}
    @ParameterizedTest @ValueSource(strings={
        "\"minimum_registered\":0","\"minimum_registered\":1.0","\"minimum_registered\":\"1\"",
        "\"minimum_registered\":2147483648","\"minimum_percent\":100.01","\"minimum_percent\":\"50\"",
        "\"minimum_registered\":1,\"minimum_percent\":50"
    }) void ambiguousThresholdsAreRejected(String threshold){assertThrows(java.io.IOException.class,()->ConfigManager.parse(config(reward(threshold))));}
    @Test void referencesAndItemsAreValidatedAndFailedReloadPreservesBackup()throws Exception{
        var validator=new CollectionValidator(List.of(),Set.of("minecraft:book","minecraft:amethyst_shard","minecraft:nether_star","minecraft:diamond"));
        var manager=new ConfigManager(directory);manager.setValidator(validator::validate);manager.initialize();var before=manager.current();
        String backup=Files.readString(directory.resolve("config.last-good.json"));
        String valid=config(reward("\"minimum_registered\":1"));validator.validate(ConfigManager.parse(valid));
        assertThrows(java.io.IOException.class,()->ConfigManager.parse(valid.replace("national","missing")));
        for(String item:List.of("minecraft:air","test:missing")){
            Files.writeString(directory.resolve("config.json"),valid.replace("minecraft:diamond",item));assertThrows(java.io.IOException.class,manager::reload);
            assertSame(before,manager.current());assertEquals(backup,Files.readString(directory.resolve("config.last-good.json")));
        }
        Files.writeString(directory.resolve("config.json"),valid);manager.reload();assertEquals(1,manager.current().rewards().size());
    }
}
