package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.*;
import br.com.shirokaidex.menus.layout.MenuNavigation;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class MenuNavigationTest {
    @TempDir Path directory;
    private String config(String menus){return "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"menus\":"+menus+"}";}
    @Test void defaultsKeepEveryExistingPositionAndAllowSafeSwaps() throws Exception {
        var defaults=Settings.defaults().menus();
        for(var screen:MenuNavigation.Screen.values()){
            var layout=defaults.layout(screen);assertTrue(layout.footer());
            screen.defaults.forEach((role,slot)->assertEquals(slot,layout.moves().get(slot)));
        }
        var value=ConfigManager.parse(config("{\"rewards\":{\"footer\":false,\"slots\":{\"close\":26,\"next\":22}},\"wiki\":{\"slots\":{\"close\":35}}}"));
        var layout=value.menus().layout(MenuNavigation.Screen.REWARDS);
        assertFalse(layout.footer());assertEquals(26,layout.moves().get(22));assertEquals(22,layout.moves().get(26));
        assertEquals(35,value.menus().layout(MenuNavigation.Screen.WIKI).slots().get("close"));
        assertThrows(UnsupportedOperationException.class,()->layout.slots().clear());
        assertThrows(UnsupportedOperationException.class,()->value.menus().layouts().clear());
    }
    @ParameterizedTest @ValueSource(strings={
        "null","{\"unknown\":{}}","{\"WIKI\":{}}","{\"wiki\":{},\"wiki\":{}}",
        "{\"wiki\":{\"footer\":1}}","{\"wiki\":{\"footer\":true,\"footer\":false}}",
        "{\"wiki\":{\"command\":\"op someone\"}}","{\"wiki\":{\"slots\":{\"close\":17}}}",
        "{\"wiki\":{\"slots\":{\"close\":36}}}","{\"rewards\":{\"slots\":{\"close\":26}}}",
        "{\"wiki\":{\"slots\":{\"unknown\":30}}}","{\"wiki\":{\"slots\":{\"close\":35.0}}}",
        "{\"wiki\":{\"slots\":{\"close\":\"35\"}}}","{\"wiki\":{\"slots\":{\"close\":35,\"close\":34}}}"
    }) void unsafeLayoutsRejectWholeConfiguration(String menus){assertThrows(java.io.IOException.class,()->ConfigManager.parse(config(menus)));}
    @Test void invalidReloadPreservesLastGoodNavigationAndRecoveryBackup()throws Exception {
        var manager=new ConfigManager(directory);manager.initialize();
        Files.writeString(directory.resolve("config.json"),config("{\"wiki\":{\"slots\":{\"close\":35}}}"));manager.reload();
        var before=manager.current();var backup=Files.readString(directory.resolve("config.last-good.json"));
        Files.writeString(directory.resolve("config.json"),config("{\"wiki\":{\"slots\":{\"close\":1}}}"));
        assertThrows(java.io.IOException.class,manager::reload);assertSame(before,manager.current());assertEquals(backup,Files.readString(directory.resolve("config.last-good.json")));
    }
}
