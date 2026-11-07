package br.com.shirokaidex.rewards;

import br.com.shirokaidex.config.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SavedItemsConfigTest {
    @TempDir Path directory;
    private static String config(String item){return "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"rewards\":[{\"id\":\"gift\",\"collection\":\"national\",\"minimum_registered\":1,\"items\":["+item+"]}]}";}
    private static SavedItem saved(){return SavedItem.create("gift","minecraft:paper",3,"{id:\"minecraft:paper\",count:1}");}
    @Test void referencesResolveOnceAndCountOverrideIsExplicit()throws Exception{
        String json=config("{\"saved_item\":\"gift\"}");var parsed=ConfigManager.parse(json,id->saved());
        assertEquals(saved().snapshot(null),parsed.rewards().getFirst().items().getFirst());
        assertEquals(1,ConfigManager.parse(config("{\"saved_item\":\"gift\",\"count\":1}"),id->saved()).rewards().getFirst().items().getFirst().count());
        assertEquals(saved().snapshot(null),ConfigManager.parse(config("{\"type\":\"SAVED_ITEM\",\"saved_item\":\"gift\"}"),id->saved()).rewards().getFirst().items().getFirst());
        assertThrows(java.io.IOException.class,()->ConfigManager.parse(json));
    }
    @ParameterizedTest @ValueSource(strings={
        "{\"saved_item\":\"gift\",\"id\":\"minecraft:paper\",\"count\":1}",
        "{\"saved_item\":\"gift\",\"payload_snbt\":\"{}\"}",
        "{\"saved_item\":\"bad-id\"}","{\"saved_item\":5}",
        "{\"type\":\"POKEMON\",\"id\":\"minecraft:paper\",\"count\":1}",
        "{\"type\":\"ITEM\",\"saved_item\":\"gift\"}",
        "{\"type\":\"CUSTOM_ITEM\",\"id\":\"minecraft:paper\",\"count\":1}",
        "{\"type\":\"SAVED_ITEM\",\"id\":\"minecraft:paper\",\"count\":1}",
        "{\"id\":\"minecraft:paper\",\"count\":1,\"payload_snbt\":false}"
    }) void rejectsAmbiguousPayloadOrReference(String item){assertThrows(java.io.IOException.class,()->ConfigManager.parse(config(item),id->saved()));}
    @Test void recoveryBackupFreezesPayloadAndDoesNotRequireDeletedTemplate()throws Exception{
        String original=config("{\"type\":\"SAVED_ITEM\",\"saved_item\":\"gift\"}");Files.writeString(directory.resolve("config.json"),original);
        var manager=new ConfigManager(directory);manager.setItemResolver(id->saved());manager.initialize();
        assertEquals(original,Files.readString(directory.resolve("config.json")));
        String backup=Files.readString(directory.resolve("config.last-good.json"));assertFalse(backup.contains("saved_item"));assertTrue(backup.contains("payload_snbt"));
        var restarted=new ConfigManager(directory);restarted.initialize();assertTrue(restarted.recoveredFromBackup());
        assertEquals(saved().snapshot(null),restarted.current().rewards().getFirst().items().getFirst());
        assertEquals(original,Files.readString(directory.resolve("config.json")));
        var lookupFailure=new ConfigManager(directory);lookupFailure.setItemResolver(id->{throw new java.util.concurrent.CompletionException(new IllegalStateException("corrupt template"));});
        lookupFailure.initialize();assertTrue(lookupFailure.recoveredFromBackup());
    }
    @Test void inlinePayloadIsRetainedAndCannotSilentlyExceedLimits()throws Exception{
        var settings=ConfigManager.parse(config("{\"id\":\"minecraft:paper\",\"count\":2,\"payload_snbt\":\"{}\"}"));
        assertEquals("{}",settings.rewards().getFirst().items().getFirst().data());
        assertThrows(IllegalArgumentException.class,()->new RewardPlan("big","national",1,null,java.util.Collections.nCopies(5,new RewardPlan.Item("minecraft:paper",1,"x".repeat(16384)))));
    }
    @Test void encodedPlanExpansionIsRejectedDuringCandidateValidation(){
        var item=new RewardPlan.Item("minecraft:paper",1,String.valueOf((char)1).repeat(16384));
        var reward=new RewardPlan("expanded","national",1,null,java.util.Collections.nCopies(4,item));
        var settings=new Settings(1,"pt_br","Dex",br.com.shirokaidex.pokedex.CollectionDefinition.defaults(),new br.com.shirokaidex.categories.CategoryCatalog(java.util.List.of()),java.util.List.of(reward));
        var validator=new CollectionValidator(java.util.List.of(),java.util.Set.of("minecraft:paper","minecraft:book","minecraft:amethyst_shard","minecraft:nether_star"));
        assertThrows(java.io.IOException.class,()->validator.validate(settings));
    }

}
