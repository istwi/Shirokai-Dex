package br.com.shirokaidex.rewards;
import br.com.shirokaidex.database.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class PermissionBundleTest {
 @TempDir Path directory;
 private RewardPlan plan(){return new RewardPlan("bundle","national",1,null,List.of(new RewardPlan.Item("minecraft:diamond",1)),false,List.of(),List.of(new CurrencyReward("impactor:dollars",new BigDecimal("500"))),List.of(),List.of(new PermissionReward("shirokai.dex.national.005")));}
 @Test void journalOrderIsItemsThenCurrencyThenPermissionAndFailedGrantCannotReplayMaterialSteps()throws Exception {
  var events=new ArrayList<String>();try(var db=new Database(directory.resolve("bundle.sqlite"))){db.initialize().get(10,TimeUnit.SECONDS);var material=new RewardDelivery.Adapter(){public CompletableFuture<Boolean> preflight(RewardPlan p,int from){return CompletableFuture.completedFuture(true);}public CompletableFuture<String> deliver(RewardPlan p,int step){events.add("item");return CompletableFuture.completedFuture("saved");}};var currency=new CurrencyRewardAdapter(material,new CurrencyRewardAdapter.Port(){public CompletableFuture<Boolean> available(List<CurrencyReward> e){return CompletableFuture.completedFuture(true);}public CompletableFuture<String> deposit(RewardPlan p,CurrencyReward e){events.add("currency");return CompletableFuture.completedFuture("deposit acknowledged");}});var adapter=new PermissionRewardAdapter(currency,new PermissionRewardAdapter.Port(){public CompletableFuture<Boolean> available(List<PermissionReward> e){return CompletableFuture.completedFuture(true);}public CompletableFuture<String> grant(PermissionReward e){events.add("permission");return CompletableFuture.failedFuture(new IllegalStateException("storage failed"));}});var delivery=new RewardDelivery(db);var player=UUID.randomUUID();assertEquals(RewardDelivery.Result.NEEDS_REVIEW,delivery.claim(player,plan(),adapter).get(10,TimeUnit.SECONDS));assertEquals(List.of("item","currency","permission"),events);assertEquals(RewardDelivery.Result.BLOCKED,delivery.claim(player,plan(),adapter).get(10,TimeUnit.SECONDS));assertEquals(3,events.size());}
 }
 @Test void missingPermissionProviderAbortsBeforeAnyMaterialOrReservation()throws Exception {
  var count=new AtomicInteger();try(var db=new Database(directory.resolve("missing.sqlite"))){db.initialize().get(10,TimeUnit.SECONDS);var base=new RewardDelivery.Adapter(){public CompletableFuture<Boolean> preflight(RewardPlan p,int f){return CompletableFuture.completedFuture(true);}public CompletableFuture<String> deliver(RewardPlan p,int n){count.incrementAndGet();return CompletableFuture.completedFuture("effect");}};var adapter=new PermissionRewardAdapter(base,new PermissionRewardAdapter.Port(){public CompletableFuture<Boolean> available(List<PermissionReward> e){return CompletableFuture.completedFuture(false);}public CompletableFuture<String> grant(PermissionReward e){throw new AssertionError();}});var player=UUID.randomUUID();assertEquals(RewardDelivery.Result.NOT_ELIGIBLE,new RewardDelivery(db).claim(player,plan(),adapter).get(10,TimeUnit.SECONDS));assertEquals(0,count.get());assertTrue(db.rewardForPlayer(player,"bundle").get(10,TimeUnit.SECONDS).isEmpty());}
 }
 @Test void oldSnapshotsAndJournalJsonRemainCompatibleWhileNewSnapshotPreservesPermissions(){var old=new RewardPlan("old","national",1,null,List.of(new RewardPlan.Item("minecraft:paper",1)));var encoded=RewardSnapshot.encode(old);var tree=com.google.gson.JsonParser.parseString(encoded).getAsJsonObject();tree.getAsJsonObject("plan").remove("permissions");assertEquals(old,RewardSnapshot.decode(tree.toString()));assertFalse(old.json().contains("permissions"));assertEquals(plan(),RewardSnapshot.decode(RewardSnapshot.encode(plan())));assertEquals(3,plan().steps());assertThrows(IllegalArgumentException.class,()->new PermissionReward("group.admin"));assertThrows(IllegalArgumentException.class,()->new PermissionReward("shirokai.dex.*"));}
}
