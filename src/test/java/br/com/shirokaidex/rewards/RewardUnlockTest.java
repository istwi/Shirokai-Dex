package br.com.shirokaidex.rewards;

import br.com.shirokaidex.database.Database;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RewardUnlockTest {
    @TempDir Path directory;
    private static RewardPlan plan(String id,int count){return new RewardPlan(id,"national",count,null,List.of(new RewardPlan.Item("minecraft:diamond",1)));}
    private static <T>T await(CompletableFuture<T> future)throws Exception{return future.get(10,TimeUnit.SECONDS);}
    @Test void evidenceMustActuallyMeetPreservedThreshold(){
        assertThrows(IllegalArgumentException.class,()->new RewardUnlock(plan("first",2),1,3));
        assertThrows(IllegalArgumentException.class,()->new RewardUnlock(plan("first",1),0,0));
        assertThrows(IllegalArgumentException.class,()->new RewardUnlock(plan("first",1),4,3));
    }
    @Test void unlockIsPermanentAcrossRestartAndKeepsFirstEvidenceDespiteRuleChanges()throws Exception{
        Path file=directory.resolve("unlocks.sqlite");UUID player=UUID.randomUUID();var original=plan("first",1);
        try(var db=new Database(file)){
            await(db.initialize());assertEquals(Set.of("first"),await(db.rewardUnlocks(player,List.of(new RewardUnlock(original,1,3)))));
            await(db.rewardUnlocks(player,List.of(new RewardUnlock(plan("first",2),2,3))));
            assertTrue(await(db.rewardUnlocks(UUID.randomUUID(),List.of())).isEmpty());
        }
        try(var db=new Database(file)){await(db.initialize());assertEquals(Set.of("first"),await(db.rewardUnlocks(player,List.of())));}
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT plan_json,observed_registered,observed_total FROM reward_unlocks")){
            assertTrue(rs.next());assertEquals(original.json(),rs.getString(1));assertEquals(1,rs.getInt(2));assertEquals(3,rs.getInt(3));assertFalse(rs.next());
        }
    }
    @Test void unlockBatchAndFirstObservationAreAtomic()throws Exception{
        Path file=directory.resolve("batch.sqlite");
        try(var db=new Database(file)){
            await(db.initialize());
            try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=c.createStatement()){
                s.execute("CREATE TRIGGER fail_second BEFORE INSERT ON reward_unlocks WHEN NEW.reward_id='second' BEGIN SELECT RAISE(ABORT,'simulated unlock failure'); END");
            }
            UUID player=UUID.randomUUID();assertThrows(ExecutionException.class,()->await(db.rewardUnlocks(player,List.of(new RewardUnlock(plan("first",1),1,2),new RewardUnlock(plan("second",1),1,2)))));
            assertTrue(await(db.rewardUnlocks(player,List.of())).isEmpty());
        }
    }
}
