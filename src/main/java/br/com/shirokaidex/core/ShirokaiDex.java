package br.com.shirokaidex.core;

import br.com.shirokaidex.cobblemon.CobblemonAccess;
import br.com.shirokaidex.config.ConfigManager;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.menus.MainMenu;
import br.com.shirokaidex.permissions.Permissions;
import br.com.shirokaidex.pokedex.DexCollection;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokedexDataChangedEvent;
import net.minecraft.server.MinecraftServer;
import java.util.function.Consumer;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.minecraft.commands.Commands.literal;

public final class ShirokaiDex implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("shirokaidex");
    private final ExecutorService configWorker = Executors.newSingleThreadExecutor(r -> new Thread(r, "shirokaidex-config"));
    private final CobblemonAccess cobblemon = new CobblemonAccess();
    private final Messages messages = new Messages();
    private ConfigManager config;
    private Database database;
    private volatile boolean ready;
    private volatile boolean stopping;
    private volatile MinecraftServer activeServer;
    private CompletableFuture<Void> initialization;

    @Override public void onInitialize() {
        Path directory = FabricLoader.getInstance().getConfigDir().resolve("shirokaidex");
        config = new ConfigManager(directory);
        database = new Database(directory.resolve("data/shirokaidex.sqlite"));
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            if (dispatcher.getRoot().getChild("dex") != null) {
                LOG.error("Command /dex already exists; refusing to overwrite it");
                return;
            }
            var root = literal("dex").executes(context -> open(context.getSource(), true))
                    .then(literal("status").executes(context -> open(context.getSource(), false)))
                    .then(literal("admin").requires(source -> Permissions.mayReload(source.getPlayer()))
                            .then(literal("reload").executes(context -> reload(context.getSource()))));
            for (var collection : DexCollection.values()) {
                root.then(literal(collection.id()).executes(context -> collection(context.getSource(), collection, true))
                        .then(literal("status").executes(context -> collection(context.getSource(), collection, false))));
            }
            dispatcher.register(root);
        });
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe((Consumer<PokedexDataChangedEvent.Post>) event -> {
            MinecraftServer server = activeServer;
            if (server != null) server.execute(() -> cobblemon.invalidate(event.getPlayerUUID()));
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            activeServer = server; stopping = false; cobblemon.clear();
            initialization = CompletableFuture.runAsync(() -> {
                try { config.initialize(); }
                catch (Exception failure) { throw new CompletionException(failure); }
            }, configWorker).thenCompose(unused -> database.initialize());
            initialization.whenComplete((unused, failure) -> {
                if (failure != null) LOG.error("Shirokai Dex disabled: foundation initialization failed", failure);
                else if (!stopping) {
                    ready = true;
                    if (config.recoveredFromBackup()) LOG.warn("Invalid config.json: using validated config.last-good.json; original file preserved");
                    LOG.info("Shirokai Dex ready: configuration validated, SQLite migration 1 applied/verified");
                }
            });
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            cobblemon.invalidate(handler.player.getUUID());
            var uuid = handler.player.getUUID();
            String name = handler.player.getGameProfile().getName();
            Instant time = Instant.now();
            if (initialization != null) initialization.thenCompose(unused -> database.recordLogin(uuid, name, time))
                    .exceptionally(failure -> { LOG.error("Could not persist player login {}", uuid, failure); return null; });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cobblemon.invalidate(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { stopping = true; ready = false; activeServer = null; cobblemon.clear(); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            configWorker.shutdown();
            try {
                if (!configWorker.awaitTermination(15, TimeUnit.SECONDS)) LOG.error("Configuration worker did not stop within deadline");
                database.close();
            } catch (InterruptedException failure) { Thread.currentThread().interrupt(); LOG.error("Shutdown interrupted", failure); }
            catch (RuntimeException failure) { LOG.error("SQLite shutdown failed", failure); }
        });
        LOG.info("Shirokai Dex experimental foundation registered; not a complete Pokédex/reward system");
    }

    private String language() { return ready ? config.current().language() : "pt_br"; }
    private int open(CommandSourceStack source, boolean menu) {
        if (!ready) { source.sendFailure(messages.text(language(), "not_ready")); return 0; }
        ServerPlayer player = source.getPlayer();
        if (player == null) { source.sendFailure(messages.text(language(), "players_only")); return 0; }
        try {
            var progress = cobblemon.progress(player);
            if (menu) MainMenu.open(player, config.current(), messages, cobblemon.collections(player), selected -> collection(source, selected, true));
            else source.sendSuccess(() -> messages.text(language(), "progress", progress.owned(), progress.seen(), progress.total()).copy()
                    .append(" ").append(messages.text(language(), "collection_percent", percentage(player, DexCollection.NATIONAL))), false);
            return 1;
        } catch (RuntimeException failure) {
            LOG.error("Official Pokédex query failed for {}", player.getUUID(), failure);
            source.sendFailure(messages.text(language(), "query_failed")); return 0;
        }
    }

    private String percentage(ServerPlayer player, DexCollection selected) {
        return cobblemon.collections(player).get(selected).progress().formattedPercentage(language())
                .orElse(messages.text(language(), "percent_unavailable").getString());
    }
    private int collection(CommandSourceStack source, DexCollection selected, boolean menu) {
        if (!ready) { source.sendFailure(messages.text(language(), "not_ready")); return 0; }
        ServerPlayer player = source.getPlayer();
        if (player == null) { source.sendFailure(messages.text(language(), "players_only")); return 0; }
        try {
            var value = cobblemon.collections(player).get(selected);
            if (menu) MainMenu.openCollection(player, config.current(), messages, selected, value, () -> open(source, true));
            else source.sendSuccess(() -> {
                String name = messages.text(language(), "dex." + selected.id()).getString();
                if (value.progress().registered().isEmpty()) return messages.text(language(), "collection_unknown_status", name).copy()
                        .append(" ").append(messages.text(language(), "shiny_reason"));
                return messages.text(language(), "collection_status", name, value.progress().registered().getAsInt(), value.progress().total(), percentage(player, selected));
            }, false);
            return 1;
        } catch (RuntimeException failure) {
            LOG.error("Collection query failed for {}", player.getUUID(), failure);
            source.sendFailure(messages.text(language(), "query_failed")); return 0;
        }
    }

    private int reload(CommandSourceStack source) {
        if (!Permissions.mayReload(source.getPlayer()) || !ready) return 0;
        CompletableFuture.runAsync(() -> {
            try { config.reload(); }
            catch (Exception failure) { throw new CompletionException(failure); }
        }, configWorker).whenComplete((unused, failure) -> {
            if (failure != null) LOG.error("Configuration reload rejected; previous configuration retained", failure);
            source.getServer().execute(() -> {
                if (stopping) return;
                if (failure == null) cobblemon.clear();
                if (failure == null) source.sendSuccess(() -> messages.text(language(), "reload_ok"), false);
                else source.sendFailure(messages.text(language(), "reload_failed"));
            });
        });
        return 1;
    }
}
