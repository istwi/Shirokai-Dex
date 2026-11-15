package br.com.shirokaidex.menus;
import br.com.shirokaidex.config.*;
import br.com.shirokaidex.menus.direct.RichText;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DirectMenusConfigTest {
 @TempDir Path directory;
 private Map<String,String> bundled()throws Exception{var files=new HashMap<String,String>();for(String name:DirectMenuCatalog.FILES)try(var input=getClass().getResourceAsStream("/config/menus/"+name+".json")){files.put(name,new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}return files;}
 @Test void importedMenusKeepEighteenTypedPackagesAndDifferentShinyMilestones()throws Exception {
  var catalog=DirectMenuCatalog.parse(bundled());var settings=ConfigManager.parse(catalog.effective("{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\"}"));assertEquals(18,settings.rewards().size());assertTrue(settings.competitions().isEmpty());assertTrue(settings.achievements().isEmpty());var first=settings.rewards().getFirst();assertEquals(new BigDecimal("5"),first.minimumPercent());assertEquals("cobblemon:poke_ball",first.items().getFirst().id());assertEquals(12,first.items().getFirst().count());assertEquals(2,first.currencies().size());assertEquals(0,new BigDecimal("500").compareTo(first.currencies().getFirst().amount()));assertEquals("shirokai.dex.national.005",first.permissions().getFirst().permission());assertTrue(first.commands().isEmpty());assertEquals(List.of("1","3","10","25","50","100"),settings.rewards().stream().filter(p->p.collection().equals("shiny")).map(p->p.minimumPercent().toPlainString()).toList());
  var dex=catalog.menus().get("dex_nacional");assertEquals(5,dex.rows());assertEquals(List.of(12,14,16,30,32,34),dex.rewardSlots());assertEquals(10,dex.buttons().get("info").slot());assertEquals(28,dex.buttons().get("close").slot());var wiki=catalog.menus().get("wiki_nacional");assertEquals(6,wiki.rows());assertEquals(28,wiki.contentSlots().size());assertEquals(48,wiki.buttons().get("filter").slot());assertEquals(49,wiki.buttons().get("search").slot());assertEquals(50,wiki.buttons().get("clear").slot());
 }
 @Test void editingOneMenuIsAtomicAndInvalidReloadKeepsAllViewsAndRewardDefinitions()throws Exception {
  var manager=new ConfigManager(directory,true);manager.initialize();var before=manager.snapshot();var file=directory.resolve("menus/wiki_nacional.json");var original=Files.readString(file);var object=JsonParser.parseString(original).getAsJsonObject();object.getAsJsonObject("buttons").getAsJsonObject("filter").addProperty("slot",49);Files.writeString(file,object.toString());assertThrows(java.io.IOException.class,manager::reload);assertSame(before,manager.snapshot());assertEquals(18,manager.current().rewards().size());Files.writeString(file,original.replace("Wiki nacional","Minha Wiki"));manager.reload();assertNotSame(before,manager.snapshot());assertTrue(manager.directMenus().menus().get("wiki_nacional").title().contains("Minha Wiki"));assertEquals(18,manager.current().rewards().size());
 }
 @Test void startupRecoversWholeValidatedDirectSnapshotWithoutOverwritingBrokenMenu()throws Exception {
  var manager=new ConfigManager(directory,true);manager.initialize();String title=manager.directMenus().menus().get("dex_nacional").title();var file=directory.resolve("menus/dex_nacional.json");Files.writeString(file,"{broken");var restarted=new ConfigManager(directory,true);restarted.initialize();assertTrue(restarted.recoveredFromBackup());assertEquals(title,restarted.directMenus().menus().get("dex_nacional").title());assertEquals("{broken",Files.readString(file));assertEquals(18,restarted.current().rewards().size());
 }
 @Test void trailingJsonUnknownCommandsAndTextActionsDoNotBecomeMenuActions()throws Exception {
  assertThrows(java.io.IOException.class,()->DirectMenuCatalog.object("{} {}"));assertThrows(IllegalArgumentException.class,()->RichText.validate("<click:run_command:'/op x'>click</click>"));assertThrows(IllegalArgumentException.class,()->RichText.validate("bad\nline"));var files=bundled();files.put("wiki_nacional",files.get("wiki_nacional").replace("\"filter\"","\"run_command\""));assertThrows(java.io.IOException.class,()->DirectMenuCatalog.parse(files));
  var longLore=bundled();var dex=JsonParser.parseString(longLore.get("dex_nacional")).getAsJsonObject();var lore=new JsonArray();for(int i=0;i<32;i++)lore.add("line");dex.getAsJsonArray("reward_groups").get(0).getAsJsonObject().add("lore",lore);longLore.put("dex_nacional",dex.toString());assertThrows(java.io.IOException.class,()->DirectMenuCatalog.parse(longLore));
 }
 @Test void closeCommandsAreOptionalIndependentAndNormalizeSlash()throws Exception {
  var files=bundled();assertEquals("",DirectMenuCatalog.parse(files).menus().get("dex_nacional").buttons().get("close").command());
  for(String command:List.of("/menu","pokedex","menu principal")){
   var dex=JsonParser.parseString(files.get("dex_nacional")).getAsJsonObject();dex.getAsJsonObject("menu").getAsJsonObject("buttons").getAsJsonObject("close").addProperty("command",command);files.put("dex_nacional",dex.toString());
   var catalog=DirectMenuCatalog.parse(files);assertEquals(command.startsWith("/")?command.substring(1):command,catalog.menus().get("dex_nacional").buttons().get("close").command());assertEquals("",catalog.menus().get("dex_shiny").buttons().get("close").command());
  }
 }
 @Test void malformedCommandsAndCommandsOnOtherControlsAreRejected()throws Exception {
  for(String command:List.of("/","menu\nother","x".repeat(257),"<click:run_command>")){
   var files=bundled();var dex=JsonParser.parseString(files.get("dex_nacional")).getAsJsonObject();dex.getAsJsonObject("menu").getAsJsonObject("buttons").getAsJsonObject("close").addProperty("command",command);files.put("dex_nacional",dex.toString());assertThrows(java.io.IOException.class,()->DirectMenuCatalog.parse(files));
  }
  for(JsonElement command:List.of(JsonNull.INSTANCE,new JsonPrimitive(42),new JsonPrimitive(true))){
   var files=bundled();var dex=JsonParser.parseString(files.get("dex_nacional")).getAsJsonObject();dex.getAsJsonObject("menu").getAsJsonObject("buttons").getAsJsonObject("close").add("command",command);files.put("dex_nacional",dex.toString());assertThrows(java.io.IOException.class,()->DirectMenuCatalog.parse(files));
  }
  var files=bundled();var wiki=JsonParser.parseString(files.get("wiki_nacional")).getAsJsonObject();wiki.getAsJsonObject("buttons").getAsJsonObject("filter").addProperty("command","menu");files.put("wiki_nacional",wiki.toString());assertThrows(java.io.IOException.class,()->DirectMenuCatalog.parse(files));
 }
 @Test void commandReloadIsAtomicAndRecoveryPreservesConfiguredCommand()throws Exception {
  var manager=new ConfigManager(directory,true);manager.initialize();var file=directory.resolve("menus/dex_nacional.json");var dex=JsonParser.parseString(Files.readString(file)).getAsJsonObject();dex.getAsJsonObject("menu").getAsJsonObject("buttons").getAsJsonObject("close").addProperty("command","/menu");Files.writeString(file,dex.toString());manager.reload();assertEquals("menu",manager.directMenus().menus().get("dex_nacional").buttons().get("close").command());var before=manager.snapshot();
  dex.getAsJsonObject("menu").getAsJsonObject("buttons").getAsJsonObject("close").addProperty("command",42);Files.writeString(file,dex.toString());assertThrows(java.io.IOException.class,manager::reload);assertSame(before,manager.snapshot());
  var restarted=new ConfigManager(directory,true);restarted.initialize();assertTrue(restarted.recoveredFromBackup());assertEquals("menu",restarted.directMenus().menus().get("dex_nacional").buttons().get("close").command());
 }
 @Test void wikiButtonDefaultsMigrateOldMenusAndAvoidExistingSlots()throws Exception {
  for(String name:List.of("dex_nacional","dex_special","dex_shiny")){
   var files=bundled();var dex=JsonParser.parseString(files.get(name)).getAsJsonObject();var buttons=dex.getAsJsonObject("menu").getAsJsonObject("buttons");buttons.remove("wiki");files.put(name,dex.toString());assertEquals(19,DirectMenuCatalog.parse(files).menus().get(name).buttons().get("wiki").slot());
   buttons.getAsJsonObject("info").addProperty("slot",19);files.put(name,dex.toString());var migrated=DirectMenuCatalog.parse(files).menus().get(name);assertNotEquals(19,migrated.buttons().get("wiki").slot());assertEquals(19,migrated.buttons().get("info").slot());
  }
 }
 @Test void wikiAppearanceRemainsEditableButCannotRunCommands()throws Exception {
  var files=bundled();var dex=JsonParser.parseString(files.get("dex_nacional")).getAsJsonObject();var wiki=dex.getAsJsonObject("menu").getAsJsonObject("buttons").getAsJsonObject("wiki");wiki.addProperty("item","minecraft:compass");wiki.addProperty("name","Consultar");wiki.addProperty("slot",20);files.put("dex_nacional",dex.toString());var button=DirectMenuCatalog.parse(files).menus().get("dex_nacional").buttons().get("wiki");assertEquals(20,button.slot());assertEquals("minecraft:compass",button.icon().item());
  wiki.addProperty("command","op someone");files.put("dex_nacional",dex.toString());assertThrows(java.io.IOException.class,()->DirectMenuCatalog.parse(files));
 }
 @Test void templateDataNeverBecomesFormattingAndRealProgressDenominatorIsNotHardcoded(){assertEquals("Player <red>value",RichText.render("Player %player%",Map.of("player","<red>value")).getString());}
}
