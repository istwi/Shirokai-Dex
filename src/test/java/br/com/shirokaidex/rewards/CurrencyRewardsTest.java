package br.com.shirokaidex.rewards;

import br.com.shirokaidex.config.ConfigManager;
import br.com.shirokaidex.database.Database;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CurrencyRewardsTest {
    @TempDir Path directory;
    private static CurrencyReward money(String id,String amount){return new CurrencyReward(id,new BigDecimal(amount));}
    private static RewardPlan plan(boolean mixed){return new RewardPlan("money","national",1,null,mixed?List.of(new RewardPlan.Item("minecraft:paper",1)):List.of(),false,List.of(),List.of(money("impactor:dollars","12.50"),money("impactor:tokens","3")));}
    private static <T>T await(CompletableFuture<T> future)throws Exception{return future.get(10,TimeUnit.SECONDS);}
    private static String config(String currency){return "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"rewards\":[{\"id\":\"money\",\"collection\":\"national\",\"minimum_registered\":1,\"currencies\":"+currency+"}]}";}
    @Test void exactDecimalsCanonicalIdentityAndCompatibility()throws Exception{
        var a=ConfigManager.parse(config("[{\"type\":\"CURRENCY\",\"currency\":\"impactor:dollars\",\"amount\":12.50}]")).rewards().getFirst();
        var b=ConfigManager.parse(config("[{\"currency\":\"impactor:dollars\",\"amount\":12.5000}]")).rewards().getFirst();
        assertEquals(a.json(),b.json());assertEquals(new BigDecimal("12.5"),a.currencies().getFirst().amount());assertEquals(1,a.steps());
        assertEquals(a,a.reuse(a.json()));assertEquals(a,a.definition());assertFalse(new RewardPlan("item","national",1,null,List.of(new RewardPlan.Item("minecraft:paper",1))).json().contains("currencies"));
        assertEquals(3,plan(true).steps());assertThrows(UnsupportedOperationException.class,()->a.currencies().clear());
        assertEquals("custom:credits",money("custom:credits","1").currency());
    }
    @ParameterizedTest @ValueSource(strings={
        "[{\"currency\":\"impactor:dollars\",\"amount\":0}]","[{\"currency\":\"impactor:dollars\",\"amount\":-1}]",
        "[{\"currency\":\"impactor:dollars\",\"amount\":1e2}]","[{\"currency\":\"impactor:dollars\",\"amount\":\"1\"}]",
        "[{\"currency\":\"dollars\",\"amount\":1}]","[{\"currency\":\"impactor:dollars\",\"amount\":1000000000000001}]",
        "[{\"currency\":\"impactor:dollars\",\"amount\":0.000000001}]","[{\"currency\":\"impactor:dollars\",\"amount\":1,\"amount\":2}]",
        "[{\"currency\":\"impactor:dollars\",\"amount\":1,\"command\":\"pay\"}]","[{\"currency\":\"impactor:dollars\",\"amount\":1,\"type\":\"COMMAND\"}]",
        "[{\"currency\":\"impactor:dollars\"}]","null","[]"
    }) void invalidMoneyConfigurationIsRejected(String values){assertThrows(java.io.IOException.class,()->ConfigManager.parse(config(values)));}
    private static RewardDelivery.Adapter base(AtomicInteger effects){return new RewardDelivery.Adapter(){
        public CompletableFuture<Boolean> preflight(RewardPlan p,int step){assertTrue(step<=p.items().size()+p.pokemon().size());return CompletableFuture.completedFuture(true);}
        public CompletableFuture<String> deliver(RewardPlan p,int step){effects.incrementAndGet();return CompletableFuture.completedFuture("base acknowledged");}
    };}
    @Test void missingCurrencyRejectsWholeMixedPlanBeforeAnyItemOrReservation()throws Exception{
        try(var db=new Database(directory.resolve("missing.sqlite"))){await(db.initialize());var effects=new AtomicInteger();var player=UUID.randomUUID();
            var port=new CurrencyRewardAdapter.Port(){public CompletableFuture<Boolean> available(List<CurrencyReward> entries){assertEquals(2,entries.size());return CompletableFuture.completedFuture(false);}public CompletableFuture<String> deposit(RewardPlan p,CurrencyReward e){fail("No fallback or partial credit allowed");return null;}};
            assertEquals(RewardDelivery.Result.NOT_ELIGIBLE,await(new RewardDelivery(db).claim(player,plan(true),new CurrencyRewardAdapter(base(effects),port))));
            assertEquals(0,effects.get());assertTrue(await(db.rewardForPlayer(player,"money")).isEmpty());
        }
    }
    @Test void mixedStepsHaveIntentBeforeCreditAndCompletedClaimsNeverRepeat()throws Exception{
        try(var db=new Database(directory.resolve("mixed.sqlite"))){await(db.initialize());var baseEffects=new AtomicInteger();var credits=new AtomicInteger();var player=UUID.randomUUID();var sequence=new ArrayList<String>();
            var port=new CurrencyRewardAdapter.Port(){public CompletableFuture<Boolean> available(List<CurrencyReward> entries){return CompletableFuture.completedFuture(true);}public CompletableFuture<String> deposit(RewardPlan p,CurrencyReward e){return db.rewardForPlayer(player,p.id()).thenCompose(op->{assertEquals(RewardJournal.State.PROCESSING,op.orElseThrow().state());int step=e.currency().endsWith("dollars")?1:2;return db.rewardIntent(op.get().id(),step);}).thenApply(repeated->{assertFalse(repeated);assertEquals(1,baseEffects.get());credits.incrementAndGet();sequence.add(e.currency());return "provider confirmed and saved";});}};
            var service=new RewardDelivery(db);var adapter=new CurrencyRewardAdapter(base(baseEffects),port);
            assertEquals(RewardDelivery.Result.COMPLETED,await(service.claim(player,plan(true),adapter)));assertEquals(List.of("impactor:dollars","impactor:tokens"),sequence);assertEquals(2,credits.get());
            assertEquals(RewardDelivery.Result.BLOCKED,await(service.claim(player,plan(true),adapter)));assertEquals(2,credits.get());assertEquals(1,baseEffects.get());
        }
    }
    @Test void waitsForPersistenceAndUncertainSaveRemainsBlockedAfterRestart()throws Exception{
        var file=directory.resolve("save.sqlite");var player=UUID.randomUUID();var credits=new AtomicInteger();var save=new CompletableFuture<String>();var called=new CompletableFuture<Void>();
        var one=new RewardPlan("money","national",1,null,List.of(),false,List.of(),List.of(money("impactor:dollars","1")));
        var port=new CurrencyRewardAdapter.Port(){public CompletableFuture<Boolean> available(List<CurrencyReward> entries){return CompletableFuture.completedFuture(true);}public CompletableFuture<String> deposit(RewardPlan p,CurrencyReward e){credits.incrementAndGet();called.complete(null);return save;}};
        try(var db=new Database(file)){await(db.initialize());var result=new RewardDelivery(db).claim(player,one,new CurrencyRewardAdapter(base(new AtomicInteger()),port));await(called);
            assertFalse(result.isDone());assertEquals(RewardJournal.State.PROCESSING,await(db.rewardForPlayer(player,"money")).orElseThrow().state());
            save.completeExceptionally(new IllegalStateException("Provider persistence uncertain"));assertEquals(RewardDelivery.Result.NEEDS_REVIEW,await(result));
        }
        try(var db=new Database(file)){await(db.initialize());assertEquals(RewardJournal.State.NEEDS_REVIEW,await(db.rewardForPlayer(player,"money")).orElseThrow().state());assertEquals(RewardDelivery.Result.BLOCKED,await(new RewardDelivery(db).claim(player,one,new CurrencyRewardAdapter(base(new AtomicInteger()),port))));assertEquals(1,credits.get());}
    }
}
