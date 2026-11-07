package br.com.shirokaidex.database;

import br.com.shirokaidex.rewards.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RewardStatesTest {
    @TempDir Path directory;
    @Test void displayQueryIsScopedBoundedImmutableAndDoesNotReserve() throws Exception {
        try(var db=new Database(directory.resolve("states.sqlite"))){
            db.initialize().get(10,TimeUnit.SECONDS);
            var owner=UUID.randomUUID();var other=UUID.randomUUID();
            var plan=new RewardPlan("first","national",1,null,List.of(new RewardPlan.Item("minecraft:diamond",1)));
            var op=db.reserveReward(owner,plan).get(10,TimeUnit.SECONDS);
            assertTrue(db.rewardStates(other,List.of("first")).get().isEmpty());
            assertTrue(db.rewardStates(owner,List.of("missing")).get().isEmpty());
            assertTrue(db.rewardForPlayer(owner,"missing").get().isEmpty());
            var states=db.rewardStates(owner,List.of("first","first","missing")).get();
            assertEquals(Map.of("first",RewardJournal.State.PENDING),states);
            assertThrows(UnsupportedOperationException.class,()->states.clear());
            db.beginReward(op.id()).get();
            assertEquals(RewardJournal.State.PROCESSING,db.rewardStates(owner,List.of("first")).get().get("first"));
            assertTrue(db.rewardStates(owner,List.of()).get().isEmpty());
            var tooMany=java.util.stream.IntStream.range(0,129).mapToObj(i->"r"+i).toList();
            assertThrows(java.util.concurrent.ExecutionException.class,()->db.rewardStates(owner,tooMany).get());
        }
    }
}
