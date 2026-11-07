package br.com.shirokaidex.rewards;

import br.com.shirokaidex.config.ConfigManager;
import br.com.shirokaidex.database.Database;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PokemonJournalTest {
    @TempDir Path directory;
    private static <T>T await(CompletableFuture<T> value)throws Exception{return value.get(10,TimeUnit.SECONDS);}
    private static RewardPlan plan(boolean item)throws Exception{
        String json="{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"rewards\":[{\"id\":\"partner\",\"collection\":\"national\",\"minimum_registered\":1,"+(item?"\"items\":[{\"id\":\"minecraft:paper\",\"count\":1}],":"")+"\"pokemon\":[{\"species\":\"cobblemon:bulbasaur\"}]}]}";
        var plan=ConfigManager.parse(json).rewards().getFirst();return plan.withPokemon(List.of(plan.pokemon().getFirst().withSnapshot("{UUID:[I;1,2,3,4]}")));
    }
    @Test void pokemonOnlyOperationRequiresAnAcknowledgedPokemonStep()throws Exception{
        try(var db=new Database(directory.resolve("pokemon.sqlite"))){await(db.initialize());var plan=plan(false);var op=await(db.reserveReward(UUID.randomUUID(),plan));await(db.beginReward(op.id()));assertFalse(await(db.completeReward(op.id())));assertTrue(await(db.rewardIntent(op.id(),0)));assertFalse(await(db.completeReward(op.id())));assertTrue(await(db.rewardDelivered(op.id(),0,"UUID readback verified fixture")));assertTrue(await(db.completeReward(op.id())));assertEquals(plan.json(),await(db.rewardOperation(op.id())).orElseThrow().plan());}
    }
    @Test void mixedActionsRemainOrderedAndSecondEffectFailureDoesNotRepeatFirst()throws Exception{
        try(var db=new Database(directory.resolve("mixed.sqlite"))){await(db.initialize());var plan=plan(true);UUID player=UUID.randomUUID();var seen=new ArrayList<Integer>();var service=new RewardDelivery(db);
            var adapter=new RewardDelivery.Adapter(){public CompletableFuture<Boolean> preflight(RewardPlan p,int step){return CompletableFuture.completedFuture(true);}public CompletableFuture<String> deliver(RewardPlan p,int step){seen.add(step);return step==0?CompletableFuture.completedFuture("Item delivered"):CompletableFuture.failedFuture(new IllegalStateException("Pokemon acknowledgement failed"));}};
            assertEquals(RewardDelivery.Result.NEEDS_REVIEW,await(service.claim(player,plan,adapter)));assertEquals(List.of(0,1),seen);assertEquals(RewardDelivery.Result.BLOCKED,await(service.claim(player,plan,adapter)));assertEquals(List.of(0,1),seen);
        }
    }
}
