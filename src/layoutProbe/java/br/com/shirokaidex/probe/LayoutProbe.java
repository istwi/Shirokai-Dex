package br.com.shirokaidex.probe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
/** Test-only external menu stub; cannot act as console/elevated source and never ships in the Dex. */
public final class LayoutProbe implements ModInitializer {
    public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->{
        if(dispatcher.getRoot().getChild("menu")!=null)throw new IllegalStateException("Existing /menu must not be overwritten by test fixture");
        dispatcher.register(Commands.literal("menu").executes(context->{
            var source=context.getSource();var player=source.getPlayer();
            if(player==null||source.hasPermission(2)||!player.getGameProfile().getName().startsWith("DexLayout")||!source.getServer().getWorldPath(LevelResource.ROOT).toString().contains("layout-validation"))throw new IllegalStateException("Layout probe requires disposable world and non-op player source");
            source.sendSuccess(()->Component.literal("DEX_LAYOUT_EXTERNAL_OK "+player.getGameProfile().getName()),false);return 1;
        }));
    });}
}
