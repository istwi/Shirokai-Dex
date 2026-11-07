package br.com.shirokaidex.core;

import br.com.shirokaidex.cobblemon.CobblemonAccess;
import br.com.shirokaidex.config.ConfigManager;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.menus.MainMenu;
import br.com.shirokaidex.wiki.Wiki;
import br.com.shirokaidex.placeholders.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import br.com.shirokaidex.permissions.Permissions;
import br.com.shirokaidex.pokedex.*;
import br.com.shirokaidex.config.Settings;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
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
import static net.minecraft.commands.Commands.argument;

public final class ShirokaiDex implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("shirokaidex");
    private final ExecutorService configWorker = Executors.newSingleThreadExecutor(r -> new Thread(r, "shirokaidex-config"));
    private final CobblemonAccess cobblemon = new CobblemonAccess();
    private final Messages messages = new Messages();
    private final PlaceholderCache placeholderCache = new PlaceholderCache();
    private PlaceholderBridge placeholderBridge;
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
                root.then(literal(collection.id()).requires(source -> canAccess(source.getPlayer(), collection.id()))
                        .executes(context -> collection(context.getSource(), collection.id(), true))
                        .then(literal("status").executes(context -> collection(context.getSource(), collection.id(), false)))
                        .then(literal("placeholder").then(argument("metric", StringArgumentType.word())
                                .suggests((context, builder) -> { PlaceholderValues.METRICS.stream().filter(value -> value.startsWith(builder.getRemaining())).sorted().forEach(builder::suggest); return builder.buildFuture(); })
                                .executes(context -> placeholder(context.getSource(), collection.id(), StringArgumentType.getString(context, "metric")))))
                        .then(literal("wiki").executes(context -> wiki(context.getSource(), collection.id(), Wiki.Request.initial("")))
                                .then(argument("query", StringArgumentType.greedyString()).executes(context -> wiki(context.getSource(), collection.id(), Wiki.Request.initial(StringArgumentType.getString(context, "query")))))));
            }
            root.then(argument("collection", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        if (ready && context.getSource().getPlayer() != null) {
                            for (var definition : visible(context.getSource().getPlayer(), config.current())) {
                                if (definition.id().startsWith(builder.getRemaining())) builder.suggest(definition.id());
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> collection(context.getSource(), StringArgumentType.getString(context, "collection"), true))
                    .then(literal("status").executes(context -> collection(context.getSource(), StringArgumentType.getString(context, "collection"), false)))
                    .then(literal("placeholder").then(argument("metric", StringArgumentType.word())
                            .suggests((context, builder) -> { PlaceholderValues.METRICS.stream().filter(value -> value.startsWith(builder.getRemaining())).sorted().forEach(builder::suggest); return builder.buildFuture(); })
                            .executes(context -> placeholder(context.getSource(), StringArgumentType.getString(context, "collection"), StringArgumentType.getString(context, "metric")))))
                    .then(literal("wiki").executes(context -> wiki(context.getSource(), StringArgumentType.getString(context, "collection"), Wiki.Request.initial("")))
                            .then(argument("query", StringArgumentType.greedyString()).executes(context -> wiki(context.getSource(), StringArgumentType.getString(context, "collection"), Wiki.Request.initial(StringArgumentType.getString(context, "query")))))));
            dispatcher.register(root);
        });
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe((Consumer<PokedexDataChangedEvent.Post>) event -> {
            MinecraftServer server = activeServer;
            if (server != null) server.execute(() -> { cobblemon.invalidate(event.getPlayerUUID()); if (placeholderBridge != null) placeholderCache.invalidate(event.getPlayerUUID()); });
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            activeServer = server; stopping = false; cobblemon.clear();
            config.setValidator(cobblemon.validationSnapshot()::validate);
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
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (FabricLoader.getInstance().isModLoaded("placeholder-api")) {
                try {
                    placeholderBridge = TextPlaceholderAccess.install(this::placeholderValue, LOG);
                    LOG.info("Text Placeholder API integration enabled; existing providers preserved");
                } catch (RuntimeException | LinkageError failure) {
                    LOG.error("Text Placeholder API integration unavailable; Dex remains enabled", failure);
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!ready || placeholderBridge == null) return;
            // Only requested/invalidated players, at most four per tick. No global player scan.
            for (int i = 0; i < 4; i++) {
                var uuid = placeholderCache.poll(); if (uuid == null) break;
                var player = server.getPlayerList().getPlayer(uuid); if (player == null) continue;
                Settings settings = config.current();
                try { placeholderCache.publish(uuid, settings, System.nanoTime(), cobblemon.collections(player, settings.collections())); }
                catch (RuntimeException failure) {
                    placeholderCache.publish(uuid, settings, System.nanoTime(), java.util.Map.of());
                    LOG.error("Placeholder snapshot failed for {}; retry deferred", uuid, failure);
                }
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            cobblemon.invalidate(handler.player.getUUID());
            if (placeholderBridge != null) placeholderCache.invalidate(handler.player.getUUID());
            var uuid = handler.player.getUUID();
            String name = handler.player.getGameProfile().getName();
            Instant time = Instant.now();
            if (initialization != null) initialization.thenCompose(unused -> database.recordLogin(uuid, name, time))
                    .exceptionally(failure -> { LOG.error("Could not persist player login {}", uuid, failure); return null; });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { cobblemon.invalidate(handler.player.getUUID()); placeholderCache.remove(handler.player.getUUID()); });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { stopping = true; ready = false; activeServer = null; cobblemon.clear(); placeholderCache.clear();
            if (placeholderBridge != null) {
                try { placeholderBridge.close(); }
                catch (RuntimeException | LinkageError failure) { LOG.error("Placeholder cleanup failed", failure); }
                finally { placeholderBridge = null; }
            } });
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
            if (menu) return selector(source, 0);
            else source.sendSuccess(() -> messages.text(language(), "progress", progress.owned(), progress.seen(), progress.total()).copy()
                    .append(" ").append(messages.text(language(), "collection_percent", cobblemon.collections(player).get(DexCollection.NATIONAL).progress().formattedPercentage(language()).orElse(messages.text(language(), "percent_unavailable").getString()))), false);
            return 1;
        } catch (RuntimeException failure) {
            LOG.error("Official Pokédex query failed for {}", player.getUUID(), failure);
            source.sendFailure(messages.text(language(), "query_failed")); return 0;
        }
    }

    private List<CollectionDefinition> visible(ServerPlayer player, Settings settings) {
        return settings.collections().stream().filter(definition -> Permissions.has(player, definition.permission())).toList();
    }
    private boolean canAccess(ServerPlayer player, String id) {
        if (!ready || player == null) return false;
        return config.current().collections().stream().filter(value -> value.id().equals(id))
                .anyMatch(value -> Permissions.has(player, value.permission()));
    }
    private int selector(CommandSourceStack source, int page) {
        if (!ready || source.getPlayer() == null) return 0;
        ServerPlayer player = source.getPlayer();
        Settings settings = config.current();
        try {
            MainMenu.open(player, settings, messages, visible(player, settings), cobblemon.collections(player, settings.collections()), page,
                    id -> collection(source, id, true), next -> selector(source, next));
            return 1;
        } catch (RuntimeException failure) {
            LOG.error("Collection selector failed for {}", player.getUUID(), failure);
            source.sendFailure(messages.text(language(), "query_failed")); return 0;
        }
    }
    private int collection(CommandSourceStack source, String id, boolean menu) {
        if (!ready) { source.sendFailure(messages.text(language(), "not_ready")); return 0; }
        ServerPlayer player = source.getPlayer();
        if (player == null) { source.sendFailure(messages.text(language(), "players_only")); return 0; }
        Settings settings = config.current();
        var definition = settings.collections().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        if (definition == null) { source.sendFailure(messages.text(language(), "collection_missing", id)); return 0; }
        if (!Permissions.has(player, definition.permission())) { source.sendFailure(messages.text(language(), "collection_denied")); return 0; }
        try {
            var value = cobblemon.collections(player, settings.collections()).get(id);
            if (menu) MainMenu.openCollection(player, settings, messages, definition, value, () -> selector(source, 0), () -> wiki(source, id, Wiki.Request.initial("")));
            else source.sendSuccess(() -> {
                String name = MainMenu.displayName(definition, settings.language(), messages);
                if (value.progress().registered().isEmpty()) return messages.text(settings.language(), "collection_unknown_status", name).copy()
                        .append(" ").append(messages.text(settings.language(), "shiny_reason"));
                String percent = value.progress().formattedPercentage(settings.language()).orElse(messages.text(settings.language(), "percent_unavailable").getString());
                return messages.text(settings.language(), "collection_status", name, value.progress().registered().getAsInt(), value.progress().total(), percent)
                        .copy().append(" ").append(messages.text(settings.language(), "mode." + definition.mode().name()));
            }, false);
            return 1;
        } catch (RuntimeException failure) {
            LOG.error("Collection query failed for {}", player.getUUID(), failure);
            source.sendFailure(messages.text(language(), "query_failed")); return 0;
        }
    }

    private int wiki(CommandSourceStack source, String id, Wiki.Request request) {
        if (!ready) { source.sendFailure(messages.text(language(), "not_ready")); return 0; }
        ServerPlayer player = source.getPlayer();
        if (player == null) { source.sendFailure(messages.text(language(), "players_only")); return 0; }
        Settings settings = config.current();
        var definition = settings.collections().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        if (definition == null) { source.sendFailure(messages.text(language(), "collection_missing", id)); return 0; }
        if (!Permissions.has(player, definition.permission())) { source.sendFailure(messages.text(language(), "collection_denied")); return 0; }
        if (request.query().length() > 128) { source.sendFailure(messages.text(language(), "wiki_query_long")); return 0; }
        try {
            var page = Wiki.select(cobblemon.wiki(player), definition, request, 18);
            MainMenu.openWiki(player, settings, messages, definition, request, page,
                    next -> wiki(source, id, next), () -> collection(source, id, true),
                    () -> { player.closeContainer(); source.sendSuccess(() -> messages.text(language(), "wiki_search_hint", id), false); });
            return 1;
        } catch (RuntimeException failure) {
            LOG.error("Wiki query failed for {}", player.getUUID(), failure);
            source.sendFailure(messages.text(language(), "query_failed")); return 0;
        }
    }

    private String placeholderValue(ServerPlayer player, String metric, String argument) {
        if (!ready || player == null || activeServer == null || !activeServer.isSameThread()
                || activeServer.getPlayerList().getPlayer(player.getUUID()) != player) return PlaceholderValues.UNKNOWN;
        String id = PlaceholderValues.collection(argument);
        Settings settings = config.current();
        if (id == null) return PlaceholderValues.UNKNOWN;
        var definition = settings.collections().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        if (definition == null || !Permissions.has(player, definition.permission())) return PlaceholderValues.UNKNOWN;
        var values = placeholderCache.read(player.getUUID(), settings, System.nanoTime());
        return PlaceholderValues.format(metric, values.get(id), settings.language());
    }
    private int placeholder(CommandSourceStack source, String id, String metric) {
        if (!ready) { source.sendFailure(messages.text(language(), "not_ready")); return 0; }
        if (source.getPlayer() == null) { source.sendFailure(messages.text(language(), "players_only")); return 0; }
        if (!canAccess(source.getPlayer(), id)) { source.sendFailure(messages.text(language(), "collection_denied")); return 0; }
        if (placeholderBridge == null) { source.sendFailure(messages.text(language(), "placeholder_unavailable")); return 0; }
        if (!PlaceholderValues.METRICS.contains(metric)) { source.sendFailure(messages.text(language(), "placeholder_metric_invalid")); return 0; }
        String value = placeholderBridge.preview(source.getPlayer(), metric, id);
        source.sendSuccess(() -> messages.text(language(), "placeholder_preview", metric, id, value), false);
        return 1;
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
                if (failure == null) {
                    cobblemon.clear(); placeholderCache.clear();
                    if (placeholderBridge != null) source.getServer().getPlayerList().getPlayers().forEach(player -> placeholderCache.invalidate(player.getUUID()));
                    source.getServer().getPlayerList().getPlayers().forEach(player -> source.getServer().getCommands().sendCommands(player));
                }
                if (failure == null) source.sendSuccess(() -> messages.text(language(), "reload_ok"), false);
                else source.sendFailure(messages.text(language(), "reload_failed"));
            });
        });
        return 1;
    }
}
