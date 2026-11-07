package br.com.shirokaidex.test;

import eu.pb4.placeholders.api.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.concurrent.*;
import org.slf4j.LoggerFactory;
import static net.minecraft.commands.Commands.*;

/** For the development server only. Exercises the real optional API, not mocked handlers. */
public final class PlaceholderProbe implements ModInitializer {
    private static final ResourceLocation REMAINING=ResourceLocation.parse("shirokaidex:remaining");
    private static final ResourceLocation PERCENT=ResourceLocation.parse("shirokaidex:percent");
    private static final ResourceLocation LEGACY=ResourceLocation.parse("dexrewards:player_dex_percent");
    private final PlaceholderHandler external=(context,argument)->PlaceholderResult.value(Component.literal("EXTERNAL"));
    private final PlaceholderHandler replaced=(context,argument)->PlaceholderResult.value(Component.literal("REPLACED"));
    private boolean didReplace;
    @Override public void onInitialize() {
        Placeholders.register(REMAINING,external);Placeholders.register(LEGACY,external);
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)-> {
            var root=literal("dexprobe").requires(source->source.hasPermission(4));
            for(String mode:new String[]{"parse","async"}) {
                root.then(literal(mode).then(argument("player",StringArgumentType.word()).then(argument("text",StringArgumentType.greedyString()).executes(context->{
                    var server=context.getSource().getServer();String name=StringArgumentType.getString(context,"player");
                    var player=server.getPlayerList().getPlayerByName(name);
                    if(!name.equals("server")&&player==null)throw new IllegalArgumentException("Probe target offline");
                    var placeholderContext=player==null?PlaceholderContext.of(server):PlaceholderContext.of(player);
                    String input=StringArgumentType.getString(context,"text");
                    try {
                        // A bounded wait is deliberate in this test fixture: unsafe blocking callbacks fail.
                        String value=mode.equals("async")?CompletableFuture.supplyAsync(()->Placeholders.parseText(Component.literal(input),placeholderContext).getString()).get(5,TimeUnit.SECONDS)
                                :Placeholders.parseText(Component.literal(input),placeholderContext).getString();
                        context.getSource().sendSuccess(()->Component.literal("PROBE "+value),false);return 1;
                    }catch(Exception failure){throw new IllegalStateException("Probe parse failed",failure);}
                }))));
            }
            root.then(literal("replace").executes(context->{Placeholders.register(PERCENT,replaced);didReplace=true;context.getSource().sendSuccess(()->Component.literal("PROBE replaced"),false);return 1;}));
            dispatcher.register(root);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{
            boolean pass=didReplace&&Placeholders.getPlaceholders().get(PERCENT)==replaced
                    &&Placeholders.getPlaceholders().get(REMAINING)==external&&Placeholders.getPlaceholders().get(LEGACY)==external
                    &&!Placeholders.getPlaceholders().containsKey(ResourceLocation.parse("shirokaidex:total"));
            LoggerFactory.getLogger("dex-placeholder-probe").info("DEX_PROBE cleanup: {}",pass?"PASS":"FAIL");
        });
    }
}
