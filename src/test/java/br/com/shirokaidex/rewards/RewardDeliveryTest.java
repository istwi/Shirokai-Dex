package br.com.shirokaidex.rewards;

import br.com.shirokaidex.database.Database;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RewardDeliveryTest {
    @TempDir Path directory;
    private static RewardPlan plan(){return new RewardPlan("first","national",1,null,List.of(new RewardPlan.Item("minecraft:diamond",1),new RewardPlan.Item("minecraft:paper",1)));}
    private static <T>T await(CompletableFuture<T> future)throws Exception{return future.get(10,TimeUnit.SECONDS);}
    @Test void journalIntentExistsBeforeEveryEffectAndRepeatedClaimDoesNotRedeliver()throws Exception{
        try(var db=new Database(directory.resolve("delivery.sqlite"))){await(db.initialize());var service=new RewardDelivery(db);UUID player=UUID.randomUUID();AtomicInteger effects=new AtomicInteger();
            var adapter=new RewardDelivery.Adapter(){
                public CompletableFuture<Boolean> preflight(RewardPlan p,int step){return CompletableFuture.completedFuture(true);}
                public CompletableFuture<String> deliver(RewardPlan p,int step){
                    return db.reserveReward(player,p).thenCompose(op->db.rewardIntent(op.id(),step)).thenApply(repeated->{assertFalse(repeated);effects.incrementAndGet();return "Test effect confirmed";});
                }
            };
            assertEquals(RewardDelivery.Result.COMPLETED,await(service.claim(player,plan(),adapter)));assertEquals(2,effects.get());
            assertEquals(RewardDelivery.Result.BLOCKED,await(service.claim(player,plan(),adapter)));assertEquals(2,effects.get());
        }
    }
    @Test void failureAfterEffectBlocksAutomaticRetryAndKeepsPartialEvidence()throws Exception{
        try(var db=new Database(directory.resolve("uncertain.sqlite"))){await(db.initialize());var service=new RewardDelivery(db);UUID player=UUID.randomUUID();AtomicInteger effects=new AtomicInteger();
            var adapter=new RewardDelivery.Adapter(){
                public CompletableFuture<Boolean> preflight(RewardPlan p,int step){return CompletableFuture.completedFuture(true);}
                public CompletableFuture<String> deliver(RewardPlan p,int step){effects.incrementAndGet();return step==0?CompletableFuture.completedFuture("First effect confirmed"):CompletableFuture.failedFuture(new IllegalStateException("Crash after external effect"));}
            };
            assertEquals(RewardDelivery.Result.NEEDS_REVIEW,await(service.claim(player,plan(),adapter)));assertEquals(2,effects.get());
            assertEquals(RewardDelivery.Result.BLOCKED,await(service.claim(player,plan(),adapter)));assertEquals(2,effects.get());
        }
    }
    @Test void lockedOrFullInventoryDoesNotReserveAndPerPlayerBusyGuardIsReleased()throws Exception{
        try(var db=new Database(directory.resolve("preflight.sqlite"))){await(db.initialize());var service=new RewardDelivery(db);UUID player=UUID.randomUUID();var gate=new CompletableFuture<Boolean>();
            var adapter=new RewardDelivery.Adapter(){public CompletableFuture<Boolean> preflight(RewardPlan p,int step){return gate;}public CompletableFuture<String> deliver(RewardPlan p,int step){throw new AssertionError("Must not deliver");}};
            var first=service.claim(player,plan(),adapter);assertEquals(RewardDelivery.Result.BUSY,await(service.claim(player,plan(),adapter)));
            gate.complete(false);assertEquals(RewardDelivery.Result.NOT_ELIGIBLE,await(first));assertEquals(RewardDelivery.Result.NOT_ELIGIBLE,await(service.claim(player,plan(),adapter)));
            assertTrue(await(db.rewardForPlayer(player,plan().id())).isEmpty());
            assertEquals(RewardJournal.State.PENDING,await(db.reserveReward(player,plan())).state());
        }
    }
    @Test void failedBeginDoesNotMisreportPendingOperationAsReviewOrDeliver()throws Exception{
        Path file=directory.resolve("begin-failure.sqlite");
        try(var db=new Database(file)){
            await(db.initialize());UUID player=UUID.randomUUID();var op=await(db.reserveReward(player,plan()));
            try(java.sql.Connection c=java.sql.DriverManager.getConnection("jdbc:sqlite:"+file);java.sql.Statement s=c.createStatement()){
                s.execute("CREATE TRIGGER fail_audit BEFORE INSERT ON reward_audit BEGIN SELECT RAISE(ABORT,'simulated failure'); END");
            }
            var service=new RewardDelivery(db);
            var adapter=new RewardDelivery.Adapter(){public CompletableFuture<Boolean> preflight(RewardPlan p,int step){return CompletableFuture.completedFuture(true);}public CompletableFuture<String> deliver(RewardPlan p,int step){throw new AssertionError("Must not execute external effect");}};
            assertEquals(RewardDelivery.Result.BLOCKED,await(service.claim(player,plan(),adapter)));
            assertEquals(RewardJournal.State.PENDING,await(db.rewardOperation(op.id())).orElseThrow().state());
        }
    }

}
