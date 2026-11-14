package br.com.shirokaidex.rewards;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Optional provider, loaded only after presence is confirmed; rechecks session before external mutation. */
public final class LuckPermsRewardPort implements PermissionRewardAdapter.Port {
    private final MinecraftServer server;
    private final ServerPlayer player;
    private final BooleanSupplier permitted;
    public LuckPermsRewardPort(MinecraftServer server,ServerPlayer player,BooleanSupplier permitted){
        this.server=Objects.requireNonNull(server);this.player=Objects.requireNonNull(player);this.permitted=Objects.requireNonNull(permitted);
    }
    private <T> CompletableFuture<T> onServer(Callable<T> action){
        var result=new CompletableFuture<T>();server.execute(()->{try{result.complete(action.call());}catch(Exception failure){result.completeExceptionally(failure);}});return result;
    }
    private boolean valid(){return server.getPlayerList().getPlayer(player.getUUID())==player&&!player.hasDisconnected()&&permitted.getAsBoolean();}
    public CompletableFuture<Boolean> available(List<PermissionReward> entries){
        if(entries.isEmpty())return CompletableFuture.completedFuture(true);
        try{return LuckPermsProvider.get().getUserManager().loadUser(player.getUUID()).orTimeout(15,TimeUnit.SECONDS)
            .thenCompose(user->onServer(()->user!=null&&user.getUniqueId().equals(player.getUUID())&&valid()));}
        catch(RuntimeException failure){return CompletableFuture.completedFuture(false);}
    }
    public CompletableFuture<String> grant(PermissionReward entry){
        var manager=LuckPermsProvider.get().getUserManager();
        return manager.loadUser(player.getUUID()).orTimeout(15,TimeUnit.SECONDS).thenCompose(user->onServer(()->{
            if(!valid()||!user.getUniqueId().equals(player.getUUID()))throw new IllegalStateException("Player or permission changed before Dex tag grant");
            user.data().add(Node.builder(entry.permission()).value(true).build());return user;
        })).thenCompose(user->manager.saveUser(user).orTimeout(15,TimeUnit.SECONDS))
          .thenCompose(unused->manager.loadUser(player.getUUID()).orTimeout(15,TimeUnit.SECONDS)).thenApply(user->{
            boolean exists=user.getNodes().stream().anyMatch(node->node.getKey().equals(entry.permission())&&node.getValue()&&!node.hasExpiry()&&node.getContexts().isEmpty());
            if(!exists)throw new CompletionException(new IllegalStateException("Saved Dex tag permission not confirmed"));
            return "LuckPerms save acknowledged for Dex tag "+entry.permission();
        });
    }
}
