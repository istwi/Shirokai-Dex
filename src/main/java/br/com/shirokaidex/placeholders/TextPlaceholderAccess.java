package br.com.shirokaidex.placeholders;

import eu.pb4.placeholders.api.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** Loaded only when the optional placeholder-api mod is present. */
public final class TextPlaceholderAccess implements PlaceholderBridge {
    @FunctionalInterface public interface Resolver { String resolve(ServerPlayer player, String metric, String argument); }
    private final OwnedRegistrations<ResourceLocation, PlaceholderHandler> registrations = new OwnedRegistrations<>(
            id -> Placeholders.getPlaceholders().get(id), Placeholders::register, Placeholders::remove);
    private TextPlaceholderAccess() { }
    private void register(Resolver resolver, Logger logger) {
        for (String metric : PlaceholderValues.METRICS) {
            var id = ResourceLocation.fromNamespaceAndPath("shirokaidex", metric);
            PlaceholderHandler handler = (context, argument) -> {
                if (context == null || context.server() == null || !context.server().isSameThread() || !context.hasPlayer())
                    return PlaceholderResult.value(Component.literal(PlaceholderValues.UNKNOWN));
                return PlaceholderResult.value(Component.literal(resolver.resolve(context.player(), metric, argument)));
            };
            if (!registrations.install(id, handler)) logger.warn("Placeholder {} already registered; preserving its provider", id);
        }
        var legacy=ResourceLocation.fromNamespaceAndPath("dexrewards","player_dex_percent");
        PlaceholderHandler alias=(context,argument)->{
            if(context==null||context.server()==null||!context.server().isSameThread()||!context.hasPlayer()||argument!=null&&!argument.isBlank())return PlaceholderResult.value(Component.literal(PlaceholderValues.UNKNOWN));
            return PlaceholderResult.value(Component.literal(resolver.resolve(context.player(),"percent","national")));
        };
        if(!registrations.install(legacy,alias))logger.warn("Legacy placeholder {} already registered; preserving its provider",legacy);
    }
    public static PlaceholderBridge install(Resolver resolver, Logger logger) {
        var bridge = new TextPlaceholderAccess();
        try { bridge.register(resolver, logger); return bridge; }
        catch (RuntimeException | LinkageError failure) { bridge.close(); throw failure; }
    }
    @Override public String preview(ServerPlayer player, String metric, String collection) {
        if(metric.equals("legacy")){var id=ResourceLocation.fromNamespaceAndPath("dexrewards","player_dex_percent");return registrations.owns(id)?Placeholders.parseText(Component.literal("%dexrewards:player_dex_percent%"),PlaceholderContext.of(player)).getString():PlaceholderValues.UNKNOWN;}
        if (!PlaceholderValues.METRICS.contains(metric)) return PlaceholderValues.UNKNOWN;
        var id = ResourceLocation.fromNamespaceAndPath("shirokaidex", metric);
        if (!registrations.owns(id)) return PlaceholderValues.UNKNOWN;
        return Placeholders.parseText(Component.literal("%shirokaidex:" + metric + " " + collection + "%"), PlaceholderContext.of(player)).getString();
    }
    @Override public void close() { registrations.close(); }
}
