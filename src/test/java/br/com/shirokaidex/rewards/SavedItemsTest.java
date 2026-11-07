package br.com.shirokaidex.rewards;

import br.com.shirokaidex.database.Database;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SavedItemsTest {
    @TempDir Path directory;
    private static SavedItem item(String id,String name){return SavedItem.create(id,"minecraft:paper",3,"{id:\"minecraft:paper\",count:1,components:{\"minecraft:custom_name\":'\""+name+"\"'}}");}
    private static <T>T await(CompletableFuture<T> future)throws Exception{return future.get(10,TimeUnit.SECONDS);}
    @Test void checksumAndPayloadBoundsAreValidated(){
        var saved=item("gift","First");assertEquals(3,saved.snapshot(null).count());assertEquals(2,saved.snapshot(2).count());
        assertThrows(IllegalArgumentException.class,()->new SavedItem(saved.id(),saved.item(),saved.count(),saved.data()+" ",saved.checksum()));
        assertThrows(IllegalArgumentException.class,()->SavedItem.create("bad-id","minecraft:paper",1,"{}"));
        assertThrows(IllegalArgumentException.class,()->SavedItem.create("gift","minecraft:paper",1,"x".repeat(16385)));
        assertThrows(IllegalArgumentException.class,()->new RewardPlan.Item("minecraft:paper",1,null));
    }
    @Test void editsDeletionAndRestartPreserveAuditAndAlreadyReservedPayload()throws Exception{
        Path file=directory.resolve("saved.sqlite");UUID actor=UUID.randomUUID(),player=UUID.randomUUID();var original=item("gift","First");UUID operation;
        try(var db=new Database(file)){
            await(db.initialize());await(db.saveItem(original,actor));
            var plan=new RewardPlan("first","national",1,null,List.of(original.snapshot(null)));
            operation=await(db.reserveReward(player,plan)).id();await(db.saveItem(item("gift","Second"),actor));
            assertEquals(original.data(),com.google.gson.JsonParser.parseString(await(db.rewardOperation(operation)).orElseThrow().plan()).getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject().get("payload_snbt").getAsString());
            assertTrue(await(db.deleteItem("gift",actor)));assertFalse(await(db.deleteItem("gift",actor)));assertTrue(await(db.savedItems()).isEmpty());
        }
        try(var db=new Database(file)){await(db.initialize());assertTrue(await(db.savedItem("gift")).isEmpty());assertTrue(await(db.rewardOperation(operation)).isPresent());}
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT action,actor,previous_payload,new_payload FROM saved_item_audit ORDER BY event_id")){
            assertTrue(rs.next());assertEquals("SAVE",rs.getString(1));assertEquals(actor.toString(),rs.getString(2));assertNull(rs.getString(3));assertTrue(rs.getString(4).contains("First"));
            assertTrue(rs.next());assertTrue(rs.getString(3).contains("First"));assertTrue(rs.getString(4).contains("Second"));
            assertTrue(rs.next());assertEquals("DELETE",rs.getString(1));assertTrue(rs.getString(3).contains("Second"));assertNull(rs.getString(4));assertFalse(rs.next());
        }
    }
    @Test void auditFailureRollsBackSaveAndDelete()throws Exception{
        Path file=directory.resolve("failure.sqlite");UUID actor=UUID.randomUUID();var first=item("gift","First");
        try(var db=new Database(file)){
            await(db.initialize());await(db.saveItem(first,actor));
            try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=c.createStatement()){s.execute("CREATE TRIGGER fail_saved_audit BEFORE INSERT ON saved_item_audit BEGIN SELECT RAISE(ABORT,'simulated failure'); END");}
            assertThrows(ExecutionException.class,()->await(db.saveItem(item("gift","Second"),actor)));
            assertThrows(ExecutionException.class,()->await(db.deleteItem("gift",actor)));
            assertEquals(first,await(db.savedItem("gift")).orElseThrow());
        }
    }
    @Test void corruptedStoredPayloadIsRefused()throws Exception{
        Path file=directory.resolve("corrupt.sqlite");
        try(var db=new Database(file)){
            await(db.initialize());await(db.saveItem(item("gift","First"),UUID.randomUUID()));
            try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=c.createStatement()){s.executeUpdate("UPDATE saved_items SET payload='corrupt'");}
            assertThrows(ExecutionException.class,()->await(db.savedItem("gift")));
            var repaired=item("gift","Repaired");await(db.saveItem(repaired,UUID.randomUUID()));assertEquals(repaired,await(db.savedItem("gift")).orElseThrow());
        }
    }
    @Test void manualPlansDoNotPretendToMeetPokedexMilestonesAndRecordInitiator()throws Exception{
        var plan=RewardPlan.manual("manual",List.of(item("gift","First").snapshot(null)));
        assertFalse(plan.unlocked(br.com.shirokaidex.pokedex.DexProgress.known(10,10)));assertTrue(plan.json().contains("ADMIN_ITEM"));assertFalse(plan.json().contains("minimum_registered"));
        UUID actor=UUID.randomUUID(),recipient=UUID.randomUUID();Path file=directory.resolve("actor.sqlite");
        try(var db=new Database(file)){await(db.initialize());await(db.reserveReward(recipient,plan,actor.toString()));assertEquals(recipient,await(db.rewardForPlayer(recipient,plan.id())).orElseThrow().player());}
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT actor FROM reward_audit WHERE action='RESERVED'")){assertTrue(rs.next());assertEquals(actor.toString(),rs.getString(1));}
    }
    @Test void boundedCatalogAllowsEditsAtQuotaAndSortedListing()throws Exception{
        try(var db=new Database(directory.resolve("quota.sqlite"))){
            await(db.initialize());UUID actor=UUID.randomUUID();
            for(int i=0;i<256;i++)await(db.saveItem(item("item_"+i,"Gift"),actor));
            assertThrows(ExecutionException.class,()->await(db.saveItem(item("overflow","Gift"),actor)));
            await(db.saveItem(item("item_0","Edited"),actor));var ids=await(db.savedItems());assertEquals(256,ids.size());assertEquals(ids.stream().sorted().toList(),ids);
            await(db.deleteItem("item_0",actor));await(db.saveItem(item("replacement","Gift"),actor));assertEquals(256,await(db.savedItems()).size());
        }
    }

}
