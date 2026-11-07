package br.com.shirokaidex.menus.layout;

import br.com.shirokaidex.rewards.*;
import java.util.Optional;

/** Presentation of an actual journal read, distinct from the permanent milestone unlock. */
public record RewardView(Status status, boolean unlocked, String operation) {
    public enum Status { LOCKED, AVAILABLE, RESERVED, PROCESSING, NEEDS_REVIEW, COMPLETED, CANCELLED, PLAN_CHANGED, UNAVAILABLE }
    public boolean canClaim(){return unlocked&&(status==Status.AVAILABLE||status==Status.RESERVED);}
    public static RewardView state(boolean unlocked,RewardJournal.State state){
        return new RewardView(state==null?(unlocked?Status.AVAILABLE:Status.LOCKED):Status.valueOf(state==RewardJournal.State.PENDING?"RESERVED":state.name()),unlocked,"");
    }
    public static RewardView preview(boolean unlocked,RewardPlan current,Optional<RewardJournal.Operation> operation){
        if(operation.isEmpty())return state(unlocked,null);
        var op=operation.get();var view=state(unlocked,op.state());
        if(op.state()==RewardJournal.State.PENDING){
            try{current.reuse(op.plan());}
            catch(RuntimeException changed){return new RewardView(Status.PLAN_CHANGED,unlocked,op.id().toString());}
        }
        return new RewardView(view.status(),unlocked,op.id().toString());
    }
    public static RewardView unavailable(){return new RewardView(Status.UNAVAILABLE,false,"");}
}
