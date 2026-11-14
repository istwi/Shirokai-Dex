package br.com.shirokaidex.menus;
import br.com.shirokaidex.config.ConfigManager;
import br.com.shirokaidex.pokedex.*;
import br.com.shirokaidex.wiki.Wiki;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FocusedMenuTest {
    @Test void bundledPresetSeparatesOrdinaryAndSpecialLabelsAndKeepsShinyVerified()throws Exception {
        String json;try(var input=getClass().getResourceAsStream("/config/focused.json")){json=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}var settings=ConfigManager.parse(json);assertTrue(settings.home().focused());assertEquals(6,settings.home().milestones().size());assertEquals(List.of("national","special","shiny"),settings.collections().stream().map(CollectionDefinition::id).toList());
        var national=settings.collections().get(0);var special=settings.collections().get(1);for(String label:List.of("legendary","mythical","ultra_beast")){var form=new ProgressCalculator.Form("normal",Set.of(label),Set.of("psychic"),false,false,false);assertFalse(national.acceptsForm("mod:species",form));assertTrue(special.acceptsForm("mod:species",form));}var normal=new ProgressCalculator.Form("normal",Set.of(),Set.of("normal"),false,false,false);assertTrue(national.acceptsForm("mod:species",normal));assertFalse(special.acceptsForm("mod:species",normal));assertEquals(CollectionDefinition.Mode.SHINY_CAPTURED,settings.collections().get(2).mode());assertEquals(1,settings.rewards().size());
    }
    @Test void sixMilestonesMustBeOrderedDistinctAndInsidePercentageRange()throws Exception {
        String prefix="{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"home\":{\"focused\":true,\"milestones\":";
        for(String bad:List.of("[10,25]","[10,25,50,75,100,90]","[10,25,50,75,90,101]","[10,25,50,75,90,90]"))assertThrows(java.io.IOException.class,()->ConfigManager.parse(prefix+bad+"}}"));assertEquals(List.of(5,15,30,60,80,100),ConfigManager.parse(prefix+"[5,15,30,60,80,100]}}").home().milestones());
    }
    @Test void leftAndRightFilterCyclesAreInverseIncludingWrap(){for(var filter:Wiki.Filter.values()){assertEquals(filter,filter.next().previous());assertEquals(filter,filter.previous().next());}assertEquals(Wiki.Filter.SHINY_SEEN,Wiki.Filter.ALL.previous());}
}
