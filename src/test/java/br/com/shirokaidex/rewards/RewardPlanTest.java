package br.com.shirokaidex.rewards;

import br.com.shirokaidex.pokedex.DexProgress;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RewardPlanTest {
    private static List<RewardPlan.Item> items(){return List.of(new RewardPlan.Item("minecraft:diamond",2));}
    @Test void exactThresholdsDoNotUseRoundedDisplayPercentOrUnavailableKnowledge(){
        var percentage=new RewardPlan("third","national",0,new BigDecimal("33.34"),items());
        assertFalse(percentage.unlocked(DexProgress.known(3,1)));assertTrue(percentage.unlocked(DexProgress.known(3,2)));
        assertFalse(percentage.unlocked(DexProgress.unavailable(3)));assertFalse(percentage.unlocked(DexProgress.known(0,0)));
        var count=new RewardPlan("count","national",2,null,items());
        assertFalse(count.unlocked(DexProgress.known(3,1)));assertTrue(count.unlocked(DexProgress.known(3,2)));
    }
    @Test void rejectsAmbiguousThresholdsInvalidItemsAndUnboundedPlans(){
        assertThrows(IllegalArgumentException.class,()->new RewardPlan("bad","national",0,null,items()));
        assertThrows(IllegalArgumentException.class,()->new RewardPlan("bad","national",1,BigDecimal.ONE,items()));
        for(String value:List.of("0","-1","100.01","1.001"))assertThrows(IllegalArgumentException.class,()->new RewardPlan("bad","national",0,new BigDecimal(value),items()));
        assertThrows(IllegalArgumentException.class,()->new RewardPlan.Item("diamond",1));
        assertThrows(IllegalArgumentException.class,()->new RewardPlan.Item("minecraft:diamond",0));
        assertThrows(IllegalArgumentException.class,()->new RewardPlan("bad","national",1,null,List.of()));
    }
    @Test void plansAreImmutableAndEquivalentPercentsSerializeIdentically(){
        var mutable=new ArrayList<>(items());var plan=new RewardPlan("first","national",0,new BigDecimal("50.00"),mutable);mutable.clear();
        assertEquals(1,plan.items().size());assertThrows(UnsupportedOperationException.class,()->plan.items().clear());
        assertEquals(plan.json(),new RewardPlan("first","national",0,new BigDecimal("50"),items()).json());
    }
}
