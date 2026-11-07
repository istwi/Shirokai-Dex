package br.com.shirokaidex.rewards;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.*;
import java.util.concurrent.*;

/** Uses the native async checkpoint when Crates owns playerdata; never races its vanilla-save interception. */
public final class PlayerSaveCoordinator {
    private PlayerSaveCoordinator() { }
    private record Backend(Object service,Object checkpoints) { }
    private static Backend crates()throws ReflectiveOperationException {
        String version=FabricLoader.getInstance().getModContainer("shirokaicrates").orElseThrow().getMetadata().getVersion().getFriendlyString();
        if(!version.equals("0.2.1")&&!version.startsWith("0.2.1+"))throw new IllegalStateException("Unaudited Crates persistence version");
        Object service=Class.forName("dev.shirokaicrates.ShirokaiCrates").getMethod("service").invoke(null);
        if(service==null)throw new IllegalStateException("Crates service unavailable");
        return new Backend(service,service.getClass().getMethod("checkpoints").invoke(service));
    }
    public static boolean ready(MinecraftServer server,ServerPlayer player){
        if(!server.isSameThread())throw new IllegalStateException("Player checkpoint must run on server thread");
        if(!FabricLoader.getInstance().isModLoaded("shirokaicrates"))return true;
        try{
            var backend=crates();
            return Boolean.TRUE.equals(backend.service().getClass().getMethod("healthy").invoke(backend.service()))
                    &&Boolean.TRUE.equals(backend.checkpoints().getClass().getMethod("healthy").invoke(backend.checkpoints()))
                    &&Boolean.FALSE.equals(backend.checkpoints().getClass().getMethod("blocked",java.util.UUID.class).invoke(backend.checkpoints(),player.getUUID()));
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return false;}
    }
    public static CompletableFuture<Void> save(MinecraftServer server,ServerPlayer player){
        if(!ready(server,player))return CompletableFuture.failedFuture(new IllegalStateException("Player persistence unavailable or busy"));
        if(!FabricLoader.getInstance().isModLoaded("shirokaicrates")){
            server.getPlayerList().saveAll();return CompletableFuture.completedFuture(null);
        }
        try{
            var backend=crates();
            Object value=backend.checkpoints().getClass().getMethod("save",ServerPlayer.class,boolean.class).invoke(backend.checkpoints(),player,true);
            if(!(value instanceof CompletableFuture<?> future))throw new IllegalStateException("Unknown checkpoint result");
            // Do not cancel or time out the provider's own future/barrier, only our dependent acknowledgement.
            return future.<Void>thenApply(unused->null).orTimeout(30,TimeUnit.SECONDS);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError failure){return CompletableFuture.failedFuture(new IllegalStateException("Native player checkpoint failed",failure));}
    }
}
