package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.*;
import br.com.shirokaidex.rewards.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerRewardsConfigTest {
    private Map<String,String> files()throws Exception{
        var files=new HashMap<String,String>();
        for(String name:DirectMenuCatalog.FILES)try(var input=getClass().getResourceAsStream("/config/menus/"+name+".json")){files.put(name,new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
        for(String name:List.of("nacional","special","shiny"))files.put("dex_"+name,Files.readString(Path.of("tests/fixtures/server-rewards/menus/dex_"+name+".json")));
        return files;
    }
    @Test void eighteenTxtPackagesKeepIdsTagsAmountsAndOnlyVisualVoucher()throws Exception{
        var files=files();var catalog=DirectMenuCatalog.parse(files);
        var settings=ConfigManager.parse(catalog.effective("{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\"}"),id->SavedItem.create(id,"minecraft:tripwire_hook",1,"{id:\"minecraft:tripwire_hook\",count:1}"));
        assertEquals(18,settings.rewards().size());
        int[][] dollars={{10000,20000,40000,75000,150000,300000},{15000,30000,60000,120000,250000,500000},{25000,50000,100000,200000,400000,1000000}};
        int[][] tokens={{3,6,10,15,25,50},{5,10,20,40,75,150},{10,20,40,80,150,300}};
        var dex=List.of("national","special","shiny");var tagDex=List.of("national","legendary","shiny");
        for(int group=0;group<3;group++)for(int step=0;step<6;step++){
            var plan=settings.rewards().get(group*6+step);assertEquals("dex_"+dex.get(group)+"_marco_0"+(step+1),plan.id());
            assertEquals(List.of("5","15","30","50","75","100").get(step),plan.minimumPercent().toPlainString());assertEquals(dollars[group][step],plan.currencies().getFirst().amount().intValueExact());assertEquals(tokens[group][step],plan.currencies().get(1).amount().intValueExact());
            var suffix=group==2?List.of("001","003","010","025","050","100").get(step):List.of("005","015","030","050","075","100").get(step);
            assertEquals("shirokai.dex."+tagDex.get(group)+"."+suffix,plan.permissions().getFirst().permission());assertTrue(plan.commands().isEmpty());
        }
        var last=settings.rewards().getLast();assertEquals("legendary",last.pokemon().getFirst().randomPool());assertTrue(last.pokemon().getFirst().shiny());assertEquals(6,last.pokemon().getFirst().ivs().size());assertTrue(last.items().stream().anyMatch(i->i.id().equals("cobblemon:rare_candy")&&i.count()==100));
        assertTrue(files.get("dex_shiny").contains("Vale Shiny"));assertFalse(files.values().stream().anyMatch(s->s.contains("VIP Top")));assertFalse(settings.rewards().stream().flatMap(p->p.items().stream()).anyMatch(i->i.id().contains("voucher")));
        for(String name:List.of("dex_nacional","dex_special","dex_shiny")){var close=catalog.menus().get(name).buttons().get("close");assertEquals("pokedex",close.command());assertEquals("cobblemon:eject_button",close.icon().item());assertEquals(19,catalog.menus().get(name).buttons().get("wiki").slot());}
    }
    @Test void missingGenuineKeyTemplatesRejectWholeRewardCandidate()throws Exception{
        var catalog=DirectMenuCatalog.parse(files());assertThrows(java.io.IOException.class,()->ConfigManager.parse(catalog.effective("{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\"}")));
    }
}
