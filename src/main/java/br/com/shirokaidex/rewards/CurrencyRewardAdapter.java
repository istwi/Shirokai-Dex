package br.com.shirokaidex.rewards;

import java.util.*;
import java.util.concurrent.*;

/** Adds monetary steps after existing item/Pokemon steps without changing their persisted ordering. */
public final class CurrencyRewardAdapter implements RewardDelivery.Adapter {
    public interface Port {
        CompletableFuture<Boolean> available(List<CurrencyReward> entries);
        CompletableFuture<String> deposit(RewardPlan plan,CurrencyReward entry);
    }
    private final RewardDelivery.Adapter base;
    private final Port port;
    public CurrencyRewardAdapter(RewardDelivery.Adapter base,Port port){this.base=Objects.requireNonNull(base);this.port=Objects.requireNonNull(port);}
    @Override public CompletableFuture<Boolean> preflight(RewardPlan plan,int from){
        int baseSteps=plan.items().size()+plan.pokemon().size();
        return base.preflight(plan,Math.min(from,baseSteps)).thenCompose(valid->!valid?CompletableFuture.completedFuture(false):port.available(plan.currencies().subList(Math.max(0,from-baseSteps),plan.currencies().size())));
    }
    @Override public CompletableFuture<String> deliver(RewardPlan plan,int step){
        int baseSteps=plan.items().size()+plan.pokemon().size();
        return step<baseSteps?base.deliver(plan,step):port.deposit(plan,plan.currencies().get(step-baseSteps));
    }
}
