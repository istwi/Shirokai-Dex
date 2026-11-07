package br.com.shirokaidex.core;

import br.com.shirokaidex.cobblemon.CobblemonAccess;
import br.com.shirokaidex.config.ConfigManager;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.menus.MainMenu;
import br.com.shirokaidex.permissions.Permissions;
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
            dispatcher.register(literal("dex").executes(context -> open(context.getSource(), true))
                    .then(literal("status").executes(context -> open(context.getSource(), false)))
                    .then(literal("admin").requires(source -> Permissions.mayReload(source.getPlayer()))
                            .then(literal("reload").executes(context -> reload(context.getSource())))));
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
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
            var uuid = handler.player.getUUID();
            String name = handler.player.getGameProfile().getName();
            Instant time = Instant.now();
            if (initialization != null) initialization.thenCompose(unused -> database.recordLogin(uuid, name, time))
                    .exceptionally(failure -> { LOG.error("Could not persist player login {}", uuid, failure); return null; });
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { stopping = true; ready = false; });
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
            if (menu) MainMenu.open(player, config.current(), messages, progress);
            else source.sendSuccess(() -> messages.text(language(), "progress", progress.owned(), progress.seen(), progress.total()), false);
            return 1;
        } catch (RuntimeException failure) {
            LOG.error("Official Pokédex query failed for {}", player.getUUID(), failure);
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
                if (failure == null) source.sendSuccess(() -> messages.text(language(), "reload_ok"), false);
                else source.sendFailure(messages.text(language(), "reload_failed"));
            });
        });
        return 1;
    }
}
