package br.com.shirokaidex.rewards;

import br.com.shirokaidex.database.Database;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RewardJournalTest {
    @TempDir Path directory;
    private static RewardPlan plan(){return new RewardPlan("first","national",1,null,List.of(new RewardPlan.Item("minecraft:diamond",1),new RewardPlan.Item("minecraft:paper",2)));}
    private static <T>T await(CompletableFuture<T> value)throws Exception{return value.get(10,TimeUnit.SECONDS);}
    private Database open(Path file)throws Exception{var db=new Database(file);await(db.initialize());return db;}
    @Test void concurrentReservationsAndBeginsHaveOneWinnerAndPreserveOriginalPlan()throws Exception{
        try(var db=open(directory.resolve("concurrent.sqlite"))){
            UUID player=UUID.randomUUID();List<CompletableFuture<RewardJournal.Operation>> reservations=new ArrayList<>();
            for(int i=0;i<100;i++)reservations.add(db.reserveReward(player,plan()));
            UUID operation=await(reservations.getFirst()).id();for(var future:reservations)assertEquals(operation,await(future).id());
            List<CompletableFuture<Boolean>> begins=new ArrayList<>();for(int i=0;i<100;i++)begins.add(db.beginReward(operation));
            int winners=0;for(var begin:begins)if(await(begin))winners++;assertEquals(1,winners);
            var changed=new RewardPlan("first","national",2,null,List.of(new RewardPlan.Item("minecraft:nether_star",64)));
            assertEquals(plan().json(),await(db.reserveReward(player,changed)).plan());
            assertNotEquals(operation,await(db.reserveReward(UUID.randomUUID(),plan())).id());
        }
    }
    @Test void intentPrecedesAcknowledgementAndCompletionRequiresEveryStep()throws Exception{
        try(var db=open(directory.resolve("steps.sqlite"))){
            var op=await(db.reserveReward(UUID.randomUUID(),plan()));assertFalse(await(db.rewardIntent(op.id(),0)));
            assertTrue(await(db.beginReward(op.id())));assertFalse(await(db.rewardDelivered(op.id(),0,"not intended")));
            assertFalse(await(db.completeReward(op.id())));assertTrue(await(db.rewardIntent(op.id(),0)));assertFalse(await(db.rewardIntent(op.id(),0)));
            assertTrue(await(db.rewardDelivered(op.id(),0,"inventory confirmed")));assertFalse(await(db.completeReward(op.id())));
            assertTrue(await(db.rewardIntent(op.id(),1)));assertTrue(await(db.rewardDelivered(op.id(),1,"inventory confirmed")));assertTrue(await(db.completeReward(op.id())));
            assertFalse(await(db.beginReward(op.id())));assertFalse(await(db.rewardIntent(op.id(),1)));
            assertEquals(RewardJournal.State.COMPLETED,await(db.rewardOperation(op.id())).orElseThrow().state());
        }
    }
    @Test void interruptedOperationNeverAutomaticallyRetriesAfterRestart()throws Exception{
        Path file=directory.resolve("restart.sqlite");UUID player=UUID.randomUUID(),id;
        try(var db=open(file)){id=await(db.reserveReward(player,plan())).id();await(db.beginReward(id));await(db.rewardIntent(id,0));}
        try(var db=open(file)){
            assertEquals(RewardJournal.State.NEEDS_REVIEW,await(db.rewardOperation(id)).orElseThrow().state());
            assertEquals(id,await(db.reserveReward(player,plan())).id());assertFalse(await(db.beginReward(id)));assertFalse(await(db.rewardIntent(id,0)));
            assertTrue(await(db.resolveReward(id,"admin:test","External inventory evidence reviewed")));
            assertFalse(await(db.resolveReward(id,"admin:test","duplicate resolution")));
        }
        try(var db=open(file)){assertEquals(RewardJournal.State.COMPLETED,await(db.rewardOperation(id)).orElseThrow().state());}
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT count(*) FROM reward_audit WHERE action='PROCESSING->NEEDS_REVIEW'")){assertEquals(1,rs.getInt(1));}
    }
    @Test void cancellationIsFinalAndRequiresReason()throws Exception{
        try(var db=open(directory.resolve("cancel.sqlite"))){
            UUID player=UUID.randomUUID();var op=await(db.reserveReward(player,plan()));
            assertThrows(ExecutionException.class,()->await(db.cancelReward(op.id(),"admin:test","")));
            assertTrue(await(db.cancelReward(op.id(),"admin:test","Operator cancelled before delivery")));
            assertFalse(await(db.beginReward(op.id())));assertEquals(RewardJournal.State.CANCELLED,await(db.reserveReward(player,plan())).state());
        }
    }
    @Test void auditFailureRollsBackStateAndReservation()throws Exception{
        Path file=directory.resolve("audit.sqlite");
        try(var db=open(file)){
            var first=await(db.reserveReward(UUID.randomUUID(),plan()));
            try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=c.createStatement()){
                s.execute("CREATE TRIGGER reject_audit BEFORE INSERT ON reward_audit BEGIN SELECT RAISE(ABORT,'simulated audit failure'); END");
            }
            assertThrows(ExecutionException.class,()->await(db.beginReward(first.id())));
            assertEquals(RewardJournal.State.PENDING,await(db.rewardOperation(first.id())).orElseThrow().state());
            assertThrows(ExecutionException.class,()->await(db.reserveReward(UUID.randomUUID(),plan())));
            try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT count(*) FROM reward_operations")){assertEquals(1,rs.getInt(1));}
        }
    }
    @Test void reviewQueueIsBoundedAndExcludesFinalStates()throws Exception{
        try(var db=open(directory.resolve("review-list.sqlite"))){
            List<UUID> ids=new ArrayList<>();for(int i=0;i<23;i++)ids.add(await(db.reserveReward(UUID.randomUUID(),plan())).id());
            await(db.cancelReward(ids.getFirst(),"admin:test","Cancelled fixture"));
            await(db.beginReward(ids.get(1)));await(db.reviewReward(ids.get(1),"Uncertain fixture"));await(db.resolveReward(ids.get(1),"admin:test","Reviewed fixture"));
            var first=await(db.incompleteRewards(0));var second=await(db.incompleteRewards(1));var third=await(db.incompleteRewards(2));
            assertEquals(10,first.size());assertEquals(10,second.size());assertEquals(1,third.size());
            Set<UUID> unique=new HashSet<>();for(var page:List.of(first,second,third))for(var row:page){assertEquals(RewardJournal.State.PENDING,row.state());assertTrue(unique.add(row.id()));}
            assertEquals(21,unique.size());assertTrue(await(db.incompleteRewards(3)).isEmpty());assertThrows(ExecutionException.class,()->await(db.incompleteRewards(-1)));
        }
    }

}
