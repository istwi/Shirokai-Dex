package br.com.shirokaidex.rewards;

import br.com.shirokaidex.database.Database;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Async journal-before-effect orchestration. Adapter is responsible for returning to the game thread. */
public final class RewardDelivery {
    public interface Adapter {
        CompletableFuture<Boolean> preflight(RewardPlan plan, int firstRemainingStep);
        CompletableFuture<String> deliver(RewardPlan plan, int step);
    }
    public enum Result { COMPLETED, BUSY, NOT_ELIGIBLE, BLOCKED, NEEDS_REVIEW }
    private final Database database;
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    public RewardDelivery(Database database) { this.database=Objects.requireNonNull(database); }
    public CompletableFuture<Result> claim(UUID player, RewardPlan plan, Adapter adapter) {return claim(player,plan,adapter,player.toString());}
    public CompletableFuture<Result> claim(UUID player, RewardPlan plan, Adapter adapter,String actor) {
        if(!active.add(player)) return CompletableFuture.completedFuture(Result.BUSY);
        AtomicReference<RewardJournal.Operation> operation=new AtomicReference<>();
        CompletableFuture<Result> result;
        try {
            result=adapter.preflight(plan,0).thenCompose(eligible->{
                if(!eligible)return CompletableFuture.completedFuture(Result.NOT_ELIGIBLE);
                return database.reserveReward(player,plan,actor).thenCompose(op->{
                    operation.set(op);
                    if(op.state()!=RewardJournal.State.PENDING || !op.plan().equals(plan.json()))return CompletableFuture.completedFuture(Result.BLOCKED);
                    return database.beginReward(op.id()).thenCompose(began->{
                        if(!began)return CompletableFuture.completedFuture(Result.BLOCKED);
                        CompletableFuture<Void> chain=CompletableFuture.completedFuture(null);
                        for(int i=0;i<plan.steps();i++){
                            final int step=i;
                            chain=chain.thenCompose(unused->adapter.preflight(plan,step)).thenCompose(valid->{
                                if(!valid)return CompletableFuture.failedFuture(new IllegalStateException("Preconditions changed before step "+step));
                                return database.rewardIntent(op.id(),step).thenCompose(intent->{
                                    if(!intent)return CompletableFuture.failedFuture(new IllegalStateException("Step intent already exists"));
                                    return adapter.deliver(plan,step);
                                }).thenCompose(evidence->database.rewardDelivered(op.id(),step,evidence)).thenAccept(recorded->{
                                    if(!recorded)throw new CompletionException(new IllegalStateException("Delivery acknowledgement not persisted"));
                                });
                            });
                        }
                        return chain.thenCompose(unused->database.completeReward(op.id())).thenApply(completed->{
                            if(!completed)throw new CompletionException(new IllegalStateException("Completion not persisted"));
                            return Result.COMPLETED;
                        });
                    });
                });
            });
        } catch(RuntimeException failure) { result=CompletableFuture.failedFuture(failure); }
        return result.handle((value,failure)->{
            if(failure==null)return CompletableFuture.completedFuture(value);
            var op=operation.get();
            if(op==null)return CompletableFuture.<Result>failedFuture(failure);
            // Deliberately generic persisted error: adapter exceptions may contain private server data.
            return database.reviewReward(op.id(),"Delivery interrupted or acknowledgement failed; inspect operation and external evidence before resolving")
                    .thenCompose(reviewed -> reviewed ? CompletableFuture.completedFuture(Result.NEEDS_REVIEW)
                            : database.rewardOperation(op.id()).thenApply(current -> current.isPresent() && current.get().state()==RewardJournal.State.NEEDS_REVIEW ? Result.NEEDS_REVIEW : Result.BLOCKED));
        }).thenCompose(value->value).whenComplete((value,failure)->active.remove(player));
    }
}
