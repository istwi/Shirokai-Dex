package br.com.shirokaidex.menus;

import br.com.shirokaidex.menus.layout.*;
import br.com.shirokaidex.pokedex.DexProgress;
import br.com.shirokaidex.rewards.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RewardPresentationTest {
    private RewardPlan plan(int count){return new RewardPlan("first","national",1,null,List.of(new RewardPlan.Item("minecraft:diamond",count)));}
    @Test void barNeverShowsPrematureCompletion(){
        assertEquals(11,ProgressMeter.filled(DexProgress.known(10000,9999),12).orElseThrow());
        assertEquals(12,ProgressMeter.filled(DexProgress.known(3,3),12).orElseThrow());
        assertEquals(0,ProgressMeter.filled(DexProgress.known(3,0),12).orElseThrow());
        assertTrue(ProgressMeter.filled(DexProgress.unavailable(3),12).isEmpty());
        assertTrue(ProgressMeter.filled(DexProgress.known(0,0),12).isEmpty());
        assertThrows(IllegalArgumentException.class,()->ProgressMeter.filled(DexProgress.known(1,1),33));
    }
    @Test void journalStateTakesPrecedenceOverUnlockAndCannotOfferRedelivery(){
        for(var state:RewardJournal.State.values()){
            var view=RewardView.state(true,state);
            assertEquals(state==RewardJournal.State.PENDING,view.canClaim());
            assertFalse(RewardView.state(false,state).canClaim());
        }
        assertTrue(RewardView.state(true,null).canClaim());
        assertFalse(RewardView.state(false,null).canClaim());
        assertFalse(RewardView.unavailable().canClaim());
    }
    @Test void changedPendingPlanBlocksConfirmationAndKeepsOperationReference(){
        var id=UUID.randomUUID();var op=new RewardJournal.Operation(id,UUID.randomUUID(),"first",plan(1).json(),RewardJournal.State.PENDING);
        assertTrue(RewardView.preview(true,plan(1),Optional.of(op)).canClaim());
        var changed=RewardView.preview(true,plan(2),Optional.of(op));
        assertEquals(RewardView.Status.PLAN_CHANGED,changed.status());assertFalse(changed.canClaim());assertEquals(id.toString(),changed.operation());
        assertTrue(RewardView.preview(true,plan(1),Optional.empty()).canClaim());
    }
}
