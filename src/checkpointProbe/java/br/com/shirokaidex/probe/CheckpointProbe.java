package br.com.shirokaidex.probe;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import java.util.concurrent.ExecutorService;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.world.item.ItemStack;

/** Only in the separate test JAR; no player access, no production world, no secret access. */
public final class CheckpointProbe implements ModInitializer {
    @Override public void onInitialize(){
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(literal("dexcheckpointprobe")
            .requires(source->source.getPlayer()==null&&source.getServer().getWorldData().getLevelName().equals("crate-checkpoint-validation"))
            .executes(context->{
                try{
                    Object service=Class.forName("dev.shirokaicrates.ShirokaiCrates").getMethod("service").invoke(null);
                    Object checkpoints=service.getClass().getMethod("checkpoints").invoke(service);
                    var field=checkpoints.getClass().getDeclaredField("io");field.setAccessible(true);ExecutorService io=(ExecutorService)field.get(checkpoints);
                    io.submit(()->{try{Thread.sleep(2000);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}});
                    context.getSource().sendSuccess(()->Component.literal("Checkpoint writer delayed 2000ms"),false);return 1;
                }catch(ReflectiveOperationException|RuntimeException failure){throw new IllegalStateException("Checkpoint probe failed",failure);}
            })
            .then(literal("keys").then(argument("player",StringArgumentType.word()).then(argument("crate",StringArgumentType.word()).executes(context->{
                try{
                    var player=context.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context,"player"));
                    if(player==null||!player.getGameProfile().getName().startsWith("DexCheckpoint"))throw new IllegalStateException("Reserved player required");
                    Object service=Class.forName("dev.shirokaicrates.ShirokaiCrates").getMethod("service").invoke(null);
                    Object snapshot=service.getClass().getMethod("snapshot").invoke(service);
                    var crates=(java.util.Map<?,?>)snapshot.getClass().getMethod("crates").invoke(snapshot);
                    Object crate=crates.get(StringArgumentType.getString(context,"crate"));if(crate==null)throw new IllegalStateException("Unknown crate");
                    var field=service.getClass().getDeclaredField("tokens");field.setAccessible(true);Object tokens=field.get(service);
                    var matches=tokens.getClass().getMethod("matches",ItemStack.class,crate.getClass(),boolean.class);int count=0;
                    for(int slot=0;slot<36;slot++){var stack=player.getInventory().getItem(slot);if(Boolean.TRUE.equals(matches.invoke(tokens,stack,crate,false)))count+=stack.getCount();}
                    int total=count;context.getSource().sendSuccess(()->Component.literal("Valid keys: "+total),false);return total;
                }catch(ReflectiveOperationException|RuntimeException failure){throw new IllegalStateException("Key validation probe failed",failure);}
            }))))));
    }
}
