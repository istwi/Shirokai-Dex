package br.com.shirokaidex.rewards;

import br.com.shirokaidex.database.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RewardRecoveryTest {
    @TempDir Path directory;
    private static RewardPlan plan(){return new RewardPlan("recover","national",1,null,List.of(new RewardPlan.Item("minecraft:diamond",2)));}
    private static <T>T await(CompletableFuture<T> future)throws Exception{return future.get(8,TimeUnit.SECONDS);}
    private static RewardDelivery.Adapter adapter(AtomicInteger effects){return new RewardDelivery.Adapter(){
        public CompletableFuture<Boolean> preflight(RewardPlan p,int first){return CompletableFuture.completedFuture(true);}
        public CompletableFuture<String> deliver(RewardPlan p,int step){effects.incrementAndGet();return CompletableFuture.completedFuture("Simulated effect verified");}
    };}
    @Test void pendingWithoutStepsResumesOriginalOperationWithActorAndReason()throws Exception{
        try(var db=new Database(directory.resolve("recovery.sqlite"))){await(db.initialize());var player=UUID.randomUUID();var op=await(db.reserveReward(player,plan()));var effects=new AtomicInteger();assertTrue(await(db.rewardResumable(op.id())));
            assertEquals(RewardDelivery.Result.COMPLETED,await(new RewardDelivery(db).resume(op.id(),player,plan(),adapter(effects),"operator","Checked untouched operation")));assertEquals(1,effects.get());assertEquals(op.id(),await(db.rewardForPlayer(player,plan().id())).orElseThrow().id());
            try(var c=java.sql.DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("recovery.sqlite"));var q=c.createStatement();var r=q.executeQuery("select actor,reason from reward_audit where action='RESUME_AUTHORIZED'")){assertTrue(r.next());assertEquals("operator",r.getString(1));assertEquals("Checked untouched operation",r.getString(2));}
        }
    }
    @Test void restartConvertsProcessingWithoutIntentToReviewAndAllowsSupervisedResume()throws Exception{
        var file=directory.resolve("restart.sqlite");var player=UUID.randomUUID();UUID id;
        try(var db=new Database(file)){await(db.initialize());id=await(db.reserveReward(player,plan())).id();await(db.beginReward(id));}
        try(var db=new Database(file)){await(db.initialize());assertEquals(RewardJournal.State.NEEDS_REVIEW,await(db.rewardOperation(id)).orElseThrow().state());var effects=new AtomicInteger();assertEquals(RewardDelivery.Result.COMPLETED,await(new RewardDelivery(db).resume(id,player,plan(),adapter(effects),"operator","Confirmed no step journal")));assertEquals(1,effects.get());}
    }
    @Test void uncertainIntentIsNeverErasedOrDispatchedAgain()throws Exception{
        try(var db=new Database(directory.resolve("intent.sqlite"))){await(db.initialize());var player=UUID.randomUUID();var op=await(db.reserveReward(player,plan()));await(db.beginReward(op.id()));await(db.rewardIntent(op.id(),0));await(db.reviewReward(op.id(),"External result uncertain"));var effects=new AtomicInteger();
            assertFalse(await(db.rewardResumable(op.id())));assertEquals(RewardDelivery.Result.BLOCKED,await(new RewardDelivery(db).resume(op.id(),player,plan(),adapter(effects),"operator","Inspect uncertain operation")));assertEquals(0,effects.get());assertEquals(RewardJournal.State.NEEDS_REVIEW,await(db.rewardOperation(op.id())).orElseThrow().state());
        }
    }
    @Test void acknowledgedPrefixCannotBeAssumedDurableAfterCrash()throws Exception{
        try(var db=new Database(directory.resolve("partial.sqlite"))){await(db.initialize());var player=UUID.randomUUID();var mixed=new RewardPlan("recover","national",1,null,List.of(new RewardPlan.Item("minecraft:diamond",1),new RewardPlan.Item("minecraft:paper",1)));var op=await(db.reserveReward(player,mixed));await(db.beginReward(op.id()));await(db.rewardIntent(op.id(),0));await(db.rewardDelivered(op.id(),0,"In-memory prefix acknowledged"));await(db.reviewReward(op.id(),"Interrupted before final inventory readback"));var effects=new AtomicInteger();
            assertFalse(await(db.rewardResumable(op.id())));assertEquals(RewardDelivery.Result.BLOCKED,await(new RewardDelivery(db).resume(op.id(),player,mixed,adapter(effects),"operator","Inspect acknowledged prefix")));assertEquals(0,effects.get());
        }
    }
    @Test void stalePreviewCannotAuthorizeAfterAnIntentAppears()throws Exception{
        try(var db=new Database(directory.resolve("race.sqlite"))){await(db.initialize());var player=UUID.randomUUID();var op=await(db.reserveReward(player,plan()));assertTrue(await(db.rewardResumable(op.id())));await(db.beginReward(op.id()));await(db.rewardIntent(op.id(),0));await(db.reviewReward(op.id(),"Concurrent effect may have started"));assertTrue(await(db.authorizeRewardResume(op.id(),player,plan(),"operator","Stale preview confirmation")).isEmpty());}
    }
    @Test void changedPlanWrongRecipientAndClosedStateRefuseWithoutEffects()throws Exception{
        try(var db=new Database(directory.resolve("identity.sqlite"))){await(db.initialize());var player=UUID.randomUUID();var op=await(db.reserveReward(player,plan()));var changed=new RewardPlan("recover","national",1,null,List.of(new RewardPlan.Item("minecraft:diamond",3)));var effects=new AtomicInteger();var delivery=new RewardDelivery(db);
            assertEquals(RewardDelivery.Result.BLOCKED,await(delivery.resume(op.id(),UUID.randomUUID(),plan(),adapter(effects),"operator","Check recipient identity")));assertEquals(RewardDelivery.Result.BLOCKED,await(delivery.resume(op.id(),player,changed,adapter(effects),"operator","Check immutable payload")));await(db.cancelReward(op.id(),"operator","Cancelled after inspection"));assertEquals(RewardDelivery.Result.BLOCKED,await(delivery.resume(op.id(),player,plan(),adapter(effects),"operator","Check cancelled operation")));assertEquals(0,effects.get());
        }
    }
    @Test void revokedPreconditionsAndShortReasonLeaveOriginalReservationUntouched()throws Exception{
        try(var db=new Database(directory.resolve("preflight.sqlite"))){await(db.initialize());var player=UUID.randomUUID();var op=await(db.reserveReward(player,plan()));var effects=new AtomicInteger();var denied=new RewardDelivery.Adapter(){public CompletableFuture<Boolean> preflight(RewardPlan p,int first){return CompletableFuture.completedFuture(false);}public CompletableFuture<String> deliver(RewardPlan p,int step){throw new AssertionError("No effect allowed");}};
            assertEquals(RewardDelivery.Result.NOT_ELIGIBLE,await(new RewardDelivery(db).resume(op.id(),player,plan(),denied,"operator","Recipient currently unavailable")));assertThrows(ExecutionException.class,()->await(new RewardDelivery(db).resume(op.id(),player,plan(),adapter(effects),"operator","short")));assertEquals(RewardJournal.State.PENDING,await(db.rewardOperation(op.id())).orElseThrow().state());assertEquals(0,effects.get());
        }
    }
    @Test void concurrentOperatorsCannotDeliverTheSameReservationTwice()throws Exception{
        try(var db=new Database(directory.resolve("concurrent.sqlite"))){await(db.initialize());var player=UUID.randomUUID();var op=await(db.reserveReward(player,plan()));var effects=new AtomicInteger();var entered=new CountDownLatch(1);var release=new CompletableFuture<String>();var delivery=new RewardDelivery(db);
            var delayed=new RewardDelivery.Adapter(){public CompletableFuture<Boolean> preflight(RewardPlan p,int first){return CompletableFuture.completedFuture(true);}public CompletableFuture<String> deliver(RewardPlan p,int step){effects.incrementAndGet();entered.countDown();return release;}};
            var first=delivery.resume(op.id(),player,plan(),delayed,"first-operator","First operator inspection");assertTrue(entered.await(5,TimeUnit.SECONDS));assertEquals(RewardDelivery.Result.BUSY,await(delivery.resume(op.id(),player,plan(),adapter(effects),"second-operator","Second operator inspection")));release.complete("Simulated persistent acknowledgement");assertEquals(RewardDelivery.Result.COMPLETED,await(first));assertEquals(RewardDelivery.Result.BLOCKED,await(delivery.resume(op.id(),player,plan(),adapter(effects),"second-operator","Repeated operator confirmation")));assertEquals(1,effects.get());
        }
    }
}
