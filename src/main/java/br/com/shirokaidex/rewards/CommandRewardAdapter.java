package br.com.shirokaidex.rewards;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.concurrent.*;
import java.util.function.*;

/** One dispatch after persisted intent; external effects always require explicit operator review. */
public final class CommandRewardAdapter implements RewardDelivery.Adapter {
    private final MinecraftServer server;private final ServerPlayer player;private final Predicate<RewardPlan> valid;private final Supplier<CommandPolicy> policy;
    public CommandRewardAdapter(MinecraftServer server,ServerPlayer player,Predicate<RewardPlan> valid,Supplier<CommandPolicy> policy){this.server=server;this.player=player;this.valid=valid;this.policy=policy;}
    private CommandSourceStack source(CommandReward entry){return (entry.context()==CommandReward.Context.PLAYER?player.createCommandSourceStack():server.createCommandSourceStack()).withSuppressedOutput();}
    private boolean allowed(RewardPlan plan){
        if(player.hasDisconnected()||server.getPlayerList().getPlayer(player.getUUID())!=player||!valid.test(plan)||plan.commands().size()!=1)return false;
        var entry=plan.commands().getFirst();if(!policy.get().authorizes(entry)||!entry.snapshot().equals(entry.render(player.getGameProfile().getName(),player.getUUID())))return false;
        var parsed=server.getCommands().getDispatcher().parse(entry.snapshot(),source(entry));return !parsed.getReader().canRead()&&parsed.getExceptions().isEmpty()&&parsed.getContext().getCommand()!=null;
    }
    public CompletableFuture<Boolean> preflight(RewardPlan plan,int from){return onServer(()->from==0&&allowed(plan));}
    public CompletableFuture<String> deliver(RewardPlan plan,int step){return onServer(()->{
        if(step!=0||!allowed(plan))throw new IllegalStateException("Command policy, recipient or permission changed");
        var entry=plan.commands().getFirst();
        server.getCommands().performPrefixedCommand(source(entry),entry.snapshot());
        // Even a successful synchronous callback does not attest to external durability.
        throw new RewardDelivery.ManualReview();
    });}
    private <T> CompletableFuture<T> onServer(Callable<T> action){var result=new CompletableFuture<T>();server.execute(()->{try{result.complete(action.call());}catch(Exception failure){result.completeExceptionally(failure);}});return result;}
}
