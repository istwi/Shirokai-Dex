package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.*;
import br.com.shirokaidex.menus.layout.HomeLayout;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class HomeLayoutTest {
    @TempDir Path directory;
    private static String config(String home){return "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"home\":"+home+"}";}
    @Test void defaultsAndPaletteOverridesRemainImmutable()throws Exception{
        assertEquals(HomeLayout.defaults(),Settings.defaults().home());
        var home=ConfigManager.parse(config("{\"palette\":\"OCEAN\",\"slots\":{\"dex\":19}}" )).home();assertEquals(19,home.slots().get("dex"));assertEquals(HomeLayout.Palette.OCEAN,home.palette());assertThrows(UnsupportedOperationException.class,()->home.slots().clear());
    }
    @ParameterizedTest @ValueSource(strings={
        "null","{\"rows\":2}","{\"rows\":7}","{\"rows\":3.0}","{\"rows\":\"6\"}","{\"palette\":\"MISSING\"}",
        "{\"slots\":{\"dex\":22}}","{\"slots\":{\"dex\":54}}","{\"slots\":{\"dex\":-1}}","{\"slots\":{\"dex\":20,\"dex\":21}}",
        "{\"slots\":{\"unknown\":2}}","{\"rows\":6,\"rows\":6}","{\"command\":\"op someone\"}"
    }) void invalidOrExecutableLayoutsAreRejected(String home){assertThrows(java.io.IOException.class,()->ConfigManager.parse(config(home)));}
    @Test void changingRowsRequiresAllPositionsToFitAndRejectsWholeReload()throws Exception{
        var slots=new HashMap<>(HomeLayout.DEFAULT_SLOTS);slots.put("settings",28);slots.put("back",30);slots.put("close",32);
        assertEquals(4,new HomeLayout(4,HomeLayout.Palette.EMBER,slots).rows());
        var manager=new ConfigManager(directory);manager.initialize();var before=manager.current();String backup=Files.readString(directory.resolve("config.last-good.json"));Files.writeString(directory.resolve("config.json"),config("{\"rows\":4}"));assertThrows(java.io.IOException.class,manager::reload);assertSame(before,manager.current());assertEquals(backup,Files.readString(directory.resolve("config.last-good.json")));
    }
}
