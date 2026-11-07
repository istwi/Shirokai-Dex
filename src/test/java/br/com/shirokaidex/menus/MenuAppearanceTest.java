package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.*;
import br.com.shirokaidex.menus.layout.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class MenuAppearanceTest {
    @TempDir Path directory;
    private String config(String menus){return "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"menus\":"+menus+"}";}
    @Test void expandedMenusKeepContentAndValidateNewNavigationSpace() throws Exception {
        var settings=ConfigManager.parse(config("{\"reward\":{\"rows\":6,\"title\":\"{player} • {title}\",\"palette\":\"OCEAN\",\"slots\":{\"close\":53},\"buttons\":{\"status\":{\"icon\":\"minecraft:clock\",\"name\":\"Estado: {name}\",\"lore\":[\"Treinador: {player}\"]}}}}"));
        var layout=settings.menus().layout(MenuNavigation.Screen.REWARD);
        assertEquals(6,layout.rows());assertEquals(53,layout.slots().get("close"));assertEquals(HomeLayout.Palette.OCEAN,layout.palette().orElseThrow());
        assertEquals("Estado: {name}",layout.buttons().get("status").name());
        assertThrows(UnsupportedOperationException.class,()->layout.buttons().clear());assertThrows(UnsupportedOperationException.class,()->layout.buttons().get("status").lore().clear());
        assertEquals(3,Settings.defaults().menus().layout(MenuNavigation.Screen.REWARD).rows());
    }
    @Test void renderingDoesNotInterpretTokensInsideDynamicData(){
        assertEquals("Alex | Wiki {player}",MenuAppearance.render("{player} | {title}","title","Wiki {player}","Alex"));
        assertEquals("Resgate concluído / Alex",MenuAppearance.render("{name} / {player}","name","Resgate concluído","Alex"));
    }
    @ParameterizedTest @ValueSource(strings={
        "{\"wiki\":{\"rows\":3}}","{\"wiki\":{\"rows\":7}}","{\"wiki\":{\"rows\":4.0}}","{\"wiki\":{\"rows\":\"4\"}}",
        "{\"wiki\":{\"title\":\"{command}\"}}","{\"wiki\":{\"title\":\"{player\"}}","{\"wiki\":{\"palette\":\"unknown\"}}",
        "{\"wiki\":{\"buttons\":{\"unknown\":{}}}}","{\"wiki\":{\"buttons\":{\"close\":{\"icon\":\"minecraft:air\"}}}}",
        "{\"wiki\":{\"buttons\":{\"close\":{\"icon\":\"stone\"}}}}","{\"wiki\":{\"buttons\":{\"close\":{\"name\":\"Hidden original state\"}}}}",
        "{\"wiki\":{\"buttons\":{\"close\":{\"name\":\"{name} {unknown}\"}}}}","{\"wiki\":{\"buttons\":{\"close\":{\"command\":\"op player\"}}}}",
        "{\"wiki\":{\"buttons\":{\"close\":{},\"close\":{}}}}","{\"wiki\":{\"buttons\":{\"close\":{\"icon\":\"minecraft:book\",\"icon\":\"minecraft:paper\"}}}}",
        "{\"wiki\":{\"buttons\":{\"close\":{\"lore\":[1]}}}}","{\"wiki\":{\"rows\":6,\"slots\":{\"close\":54}}}"
    }) void unsafePresentationIsRejected(String menus){assertThrows(java.io.IOException.class,()->ConfigManager.parse(config(menus)));}
    @Test void limitsAndRegistryValidationPreserveLastGoodConfiguration()throws Exception{
        assertThrows(IllegalArgumentException.class,()->new MenuAppearance.Button("","{name}",Collections.nCopies(9,"line")));
        assertThrows(IllegalArgumentException.class,()->new MenuAppearance.Button("","{name}",List.of("x".repeat(161))));
        assertThrows(java.io.IOException.class,()->ConfigManager.parse(config("{\"wiki\":{\"title\":\""+"x".repeat(161)+"\"}}")));
        var manager=new ConfigManager(directory);manager.initialize();
        var known=new HashSet<String>();Settings.defaults().collections().forEach(c->known.add(c.icon()));known.add("minecraft:clock");
        manager.setValidator(new CollectionValidator(List.of(),known)::validate);
        Files.writeString(directory.resolve("config.json"),config("{\"wiki\":{\"buttons\":{\"close\":{\"icon\":\"minecraft:clock\"}}}}"));manager.reload();
        var before=manager.current();String backup=Files.readString(directory.resolve("config.last-good.json"));
        Files.writeString(directory.resolve("config.json"),config("{\"wiki\":{\"buttons\":{\"close\":{\"icon\":\"missing:icon\"}}}}"));
        assertThrows(java.io.IOException.class,manager::reload);assertSame(before,manager.current());assertEquals(backup,Files.readString(directory.resolve("config.last-good.json")));
    }
}
