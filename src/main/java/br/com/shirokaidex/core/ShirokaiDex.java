package br.com.shirokaidex.core;

import br.com.shirokaidex.cobblemon.CobblemonAccess;
import br.com.shirokaidex.config.ConfigManager;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.menus.MainMenu;
import br.com.shirokaidex.wiki.Wiki;
import br.com.shirokaidex.spawning.*;
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
    private SpawnCatalog spawnCatalog = SpawnCatalog.unavailable();
    private boolean spawnRefreshPending;
    private ConfigManager config;
    private Database database;
    private br.com.shirokaidex.rewards.RewardDelivery rewardDelivery;
    private final java.util.Map<java.util.UUID,java.util.Set<String>> rewardUnlockCache = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID,Long> rewardObservedAt = new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> rewardObserving = new java.util.HashSet<>();
    private int rewardPlayerCursor;
    private long rewardViewSequence;
    private final java.util.Map<java.util.UUID,Long> rewardViewRequests=new java.util.HashMap<>();
    private final java.util.Set<br.com.shirokaidex.rewards.PokemonRewardAdapter> pokemonDeliveries=new java.util.HashSet<>();
    private final java.util.Set<CompletableFuture<Void>> payloadValidations=java.util.concurrent.ConcurrentHashMap.newKeySet();

    private volatile boolean ready;
    private volatile boolean stopping;
    private volatile MinecraftServer activeServer;
    private CompletableFuture<Void> initialization;

    @Override public void onInitialize() {
        Path directory = FabricLoader.getInstance().getConfigDir().resolve("shirokaidex");
        config = new ConfigManager(directory);
        database = new Database(directory.resolve("data/shirokaidex.sqlite"));
        rewardDelivery = new br.com.shirokaidex.rewards.RewardDelivery(database);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            if (dispatcher.getRoot().getChild("dex") != null) {
                LOG.error("Command /dex already exists; refusing to overwrite it");
                return;
            }
            var root = literal("dex").executes(context -> open(context.getSource(), true))
                    .then(literal("status").executes(context -> open(context.getSource(), false)))
                    .then(literal("admin").requires(source -> Permissions.mayReload(source.getPlayer()) || (Permissions.has(source.getPlayer(), "shirokaidex.admin.rewards") || Permissions.has(source.getPlayer(),"shirokaidex.admin.items")))
                            .then(literal("reload").requires(source -> Permissions.mayReload(source.getPlayer())).executes(context -> reload(context.getSource())))
                            .then(rewardAdminCommands()).then(savedItemCommands()));
            root.then(literal("rewards").executes(context -> rewards(context.getSource(), null, 0))
                    .then(argument("reward", StringArgumentType.word()).executes(context -> rewards(context.getSource(), StringArgumentType.getString(context, "reward"), 0))));
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
            if (server != null) server.execute(() -> { rewardObservedAt.remove(event.getPlayerUUID()); cobblemon.invalidate(event.getPlayerUUID()); if (placeholderBridge != null) placeholderCache.invalidate(event.getPlayerUUID()); });
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            activeServer = server; stopping = false; cobblemon.clear();
            var registry = cobblemon.validationSnapshot();
            config.setItemResolver(id -> database.savedItem(id).join().orElse(null));
            config.setValidator(settings -> {
                registry.validate(settings);
                validatePayloads(server,settings);
                try { database.preserveCategoryRules(br.com.shirokaidex.categories.CategoryRules.of(settings.categories())).join(); }
                catch (CompletionException failure) { throw new java.io.IOException("Cannot preserve category rules; configuration not applied", failure); }
            });
            initialization = database.initialize().thenRunAsync(() -> {
                try { config.initialize(); }
                catch (Exception failure) { throw new CompletionException(failure); }
            }, configWorker);
            initialization.whenComplete((unused, failure) -> {
                if (failure != null) LOG.error("Shirokai Dex disabled: foundation initialization failed", failure);
                else if (!stopping) {
                    ready = true;
                    if (config.recoveredFromBackup()) LOG.warn("Invalid config.json: using validated config.last-good.json; original file preserved");
                    LOG.info("Shirokai Dex ready: configuration validated, SQLite migration 5 applied/verified");
                }
            });
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            spawnRefreshPending = true;
            if (FabricLoader.getInstance().isModLoaded("placeholder-api")) {
                try {
                    placeholderBridge = TextPlaceholderAccess.install(this::placeholderValue, LOG);
                    LOG.info("Text Placeholder API integration enabled; existing providers preserved");
                } catch (RuntimeException | LinkageError failure) {
                    LOG.error("Text Placeholder API integration unavailable; Dex remains enabled", failure);
                }
            }
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> server.execute(() -> {
            spawnCatalog = SpawnCatalog.unavailable(); spawnRefreshPending = success;
        }));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (spawnRefreshPending && ready) {
                spawnRefreshPending = false;
                try { spawnCatalog = SpawnAccess.capture(server); }
                catch (RuntimeException failure) { spawnCatalog = SpawnCatalog.unavailable(); LOG.error("World spawn information unavailable", failure); }
            }
            if (ready) observeRewards(server);
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
            rewardObservedAt.remove(handler.player.getUUID());
            cobblemon.invalidate(handler.player.getUUID());
            if (placeholderBridge != null) placeholderCache.invalidate(handler.player.getUUID());
            var uuid = handler.player.getUUID();
            String name = handler.player.getGameProfile().getName();
            Instant time = Instant.now();
            if (initialization != null) initialization.thenCompose(unused -> database.recordLogin(uuid, name, time))
                    .exceptionally(failure -> { LOG.error("Could not persist player login {}", uuid, failure); return null; });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { rewardViewRequests.remove(handler.player.getUUID()); rewardUnlockCache.remove(handler.player.getUUID()); rewardObservedAt.remove(handler.player.getUUID()); cobblemon.invalidate(handler.player.getUUID()); placeholderCache.remove(handler.player.getUUID()); });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { stopping = true; java.util.List.copyOf(pokemonDeliveries).forEach(br.com.shirokaidex.rewards.PokemonRewardAdapter::close); pokemonDeliveries.clear(); payloadValidations.forEach(future -> future.completeExceptionally(new IllegalStateException("Server stopping"))); ready = false; activeServer = null; rewardViewRequests.clear(); rewardUnlockCache.clear(); rewardObservedAt.clear(); cobblemon.clear(); placeholderCache.clear(); spawnCatalog = SpawnCatalog.unavailable(); spawnRefreshPending = false;
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
            if (menu) return home(source);
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
    private int home(CommandSourceStack source){
        if(!ready||source.getPlayer()==null)return 0;var player=source.getPlayer();var settings=config.current();
        java.util.Map<String,Runnable> actions=new java.util.HashMap<>();actions.put("dex",()->selector(source,0));actions.put("rewards",()->rewards(source,null,0));actions.put("close",player::closeContainer);
        if(canAccess(player,"national"))actions.put("wiki",()->wiki(source,"national",Wiki.Request.initial("")));
        if(source.getServer().getCommands().getDispatcher().getRoot().getChild("menu")!=null)actions.put("back",()->{
            if(!ready)return;player.closeContainer();source.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(),"menu");
        });
        var progress=cobblemon.collections(player).get(DexCollection.NATIONAL).progress();
        br.com.shirokaidex.menus.HomeMenu.open(player,settings,messages,progress,visible(player,settings).size(),(int)settings.rewards().stream().filter(plan->canAccess(player,plan.collection())).count(),actions);return 1;
    }
    private int selector(CommandSourceStack source, int page) {
        if (!ready || source.getPlayer() == null) return 0;
        ServerPlayer player = source.getPlayer();
        Settings settings = config.current();
        try {
            MainMenu.open(player, settings, messages, visible(player, settings), cobblemon.collections(player, settings.collections()), page,
                    id -> collection(source, id, true), next -> selector(source, next),()->home(source));
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
            MainMenu.openWiki(player, settings, messages, definition, request, page, spawnCatalog,
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

    private void validatePayloads(MinecraftServer server,Settings settings)throws java.io.IOException {
        var entries=settings.rewards().stream().flatMap(plan->plan.items().stream()).filter(item->!item.data().isEmpty()).distinct().toList();
        var pokemon=settings.rewards().stream().flatMap(plan->plan.pokemon().stream()).distinct().toList();
        if(entries.isEmpty()&&pokemon.isEmpty())return;
        var validation=new CompletableFuture<Void>();payloadValidations.add(validation);
        if(stopping)validation.completeExceptionally(new IllegalStateException("Server stopping"));
        else server.execute(()->{
            try{if(stopping)throw new IllegalStateException("Server stopping");for(var entry:entries)br.com.shirokaidex.rewards.ItemPayload.restore(server,entry);for(var request:pokemon)br.com.shirokaidex.rewards.PokemonPayload.freeze(server,null,request);validation.complete(null);}
            catch(Exception failure){validation.completeExceptionally(failure);}
        });
        try{validation.get(120,TimeUnit.SECONDS);}
        catch(Exception failure){if(failure instanceof InterruptedException)Thread.currentThread().interrupt();throw new java.io.IOException("Saved item component validation failed; candidate not applied",failure);}
        finally{payloadValidations.remove(validation);}
    }
    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> savedItemCommands(){
        var root=literal("item").requires(source->Permissions.has(source.getPlayer(),"shirokaidex.admin.items"));
        root.then(literal("save").then(argument("item",StringArgumentType.word()).executes(context->savedItemCommand(context.getSource(),"save",StringArgumentType.getString(context,"item"),0,null))));
        root.then(literal("list").executes(context->savedItemCommand(context.getSource(),"list","",0,null))
                .then(argument("page",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1)).executes(context->savedItemCommand(context.getSource(),"list","",com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,"page")-1,null))));
        root.then(literal("delete").then(argument("item",StringArgumentType.word()).then(literal("confirm").executes(context->savedItemCommand(context.getSource(),"delete",StringArgumentType.getString(context,"item"),0,null)))));
        root.then(literal("give").then(argument("item",StringArgumentType.word()).then(argument("player",StringArgumentType.word()).executes(context->savedItemCommand(context.getSource(),"give",StringArgumentType.getString(context,"item"),0,StringArgumentType.getString(context,"player"))))));
        return root;
    }
    private int savedItemCommand(CommandSourceStack source,String action,String id,int page,String targetName){
        var actor=source.getPlayer();if(!ready||actor==null||!Permissions.has(actor,"shirokaidex.admin.items"))return 0;
        if(!action.equals("list")&&!id.matches("[a-z][a-z0-9_]{0,47}")){source.sendFailure(messages.text(language(),"saved_item_invalid"));return 0;}
        if(action.equals("give")){
            var target=source.getServer().getPlayerList().getPlayerByName(targetName);
            if(target==null){source.sendFailure(messages.text(language(),"saved_item_invalid"));return 0;}
            database.savedItem(id).whenComplete((value,failure)->source.getServer().execute(()->{
                if(stopping||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.items"))return;
                if(failure!=null||value.isEmpty()){source.sendFailure(messages.text(language(),"saved_item_invalid"));return;}
                try{br.com.shirokaidex.rewards.ItemPayload.restore(source.getServer(),value.get().snapshot(1));}
                catch(RuntimeException invalid){source.sendFailure(messages.text(language(),"saved_item_invalid"));return;}
                var plan=br.com.shirokaidex.rewards.RewardPlan.manual("admin_"+java.util.UUID.randomUUID().toString().replace("-",""),List.of(value.get().snapshot(null)));
                MainMenu.confirmReward(actor,config.current(),messages,plan,id+" → "+target.getGameProfile().getName(),()->{
                    if(!ready||!Permissions.has(actor,"shirokaidex.admin.items")){source.sendFailure(messages.text(language(),"collection_denied"));return;}
                    actor.closeContainer();
                    rewardDelivery.claim(target.getUUID(),plan,new br.com.shirokaidex.rewards.ItemRewardAdapter(source.getServer(),target,current->ready&&!stopping&&!actor.hasDisconnected()&&Permissions.has(actor,"shirokaidex.admin.items")),actor.getUUID().toString())
                            .whenComplete((result,error)->source.getServer().execute(()->{
                                if(stopping||actor.hasDisconnected())return;
                                if(error!=null)source.sendFailure(messages.text(language(),"reward_error"));
                                else{source.sendSuccess(()->messages.text(language(),"reward_result."+result.name().toLowerCase(java.util.Locale.ROOT)),false);
                                    database.rewardForPlayer(target.getUUID(),plan.id()).thenAccept(found->found.ifPresent(op->source.getServer().execute(()->{if(!stopping&&!actor.hasDisconnected()&&Permissions.has(actor,"shirokaidex.admin.items"))source.sendSuccess(()->messages.text(language(),"saved_item_operation",op.id()),false);})));}
                            }));
                },()->actor.closeContainer());
            }));return 1;
        }
        CompletableFuture<String> result;
        try{
            if(action.equals("save")){
                var item=br.com.shirokaidex.rewards.ItemPayload.capture(source.getServer(),id,actor.getMainHandItem());
                result=database.saveItem(item,actor.getUUID()).thenApply(unused->id);
            }else if(action.equals("delete"))result=database.deleteItem(id,actor.getUUID()).thenApply(deleted->deleted?id:"—");
            else result=database.savedItems().thenApply(ids->{var slice=Wiki.paginate(ids,page,18);return "["+(slice.number()+1)+"/"+slice.pages()+"] "+String.join(", ",slice.items());});
        }catch(RuntimeException failure){source.sendFailure(messages.text(language(),"saved_item_invalid"));return 0;}
        result.whenComplete((value,failure)->source.getServer().execute(()->{
            if(stopping||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.items"))return;
            if(failure!=null){LOG.error("Saved item operation failed",failure);source.sendFailure(messages.text(language(),"saved_item_invalid"));}
            else source.sendSuccess(()->messages.text(language(),"saved_item_result",value),false);
        }));return 1;
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> rewardAdminCommands() {
        var root = literal("reward").requires(source -> Permissions.has(source.getPlayer(), "shirokaidex.admin.rewards"));
        root.then(literal("list").executes(context->rewardAdminList(context.getSource(),0))
                .then(argument("page",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1,100001)).executes(context->rewardAdminList(context.getSource(),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,"page")-1))));
        root.then(literal("status").then(argument("operation", StringArgumentType.word()).executes(context -> rewardAdmin(context.getSource(), StringArgumentType.getString(context, "operation"), "status", ""))));
        root.then(literal("resume").then(argument("operation",StringArgumentType.word()).then(argument("reason",StringArgumentType.greedyString())
                .executes(context->previewRewardResume(context.getSource(),StringArgumentType.getString(context,"operation"),StringArgumentType.getString(context,"reason"))))));
        for (String action : List.of("cancel", "complete")) {
            root.then(literal(action).then(argument("operation", StringArgumentType.word())
                    .then(literal("confirm").then(argument("evidence", StringArgumentType.greedyString())
                            .executes(context -> rewardAdmin(context.getSource(), StringArgumentType.getString(context, "operation"), action, StringArgumentType.getString(context, "evidence")))))));
        }
        return root;
    }
    private int previewRewardResume(CommandSourceStack source,String raw,String reason){
        var actor=source.getPlayer();if(!ready||actor==null||!Permissions.has(actor,"shirokaidex.admin.rewards"))return 0;
        java.util.UUID id;try{id=java.util.UUID.fromString(raw);}catch(IllegalArgumentException failure){source.sendFailure(messages.text(language(),"reward_admin_invalid"));return 0;}
        if(reason.strip().length()<8||reason.length()>2048||reason.codePoints().anyMatch(Character::isISOControl)){source.sendFailure(messages.text(language(),"reward_admin_invalid"));return 0;}
        database.rewardOperation(id).thenCombine(database.rewardResumable(id),(found,safe)->safe?found:java.util.Optional.<br.com.shirokaidex.rewards.RewardJournal.Operation>empty()).whenComplete((found,failure)->source.getServer().execute(()->{
            if(stopping||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.rewards"))return;
            if(failure!=null||found.isEmpty()){source.sendFailure(messages.text(language(),"reward_resume_blocked"));return;}
            var operation=found.get();var target=source.getServer().getPlayerList().getPlayer(operation.player());
            var definition=config.current().rewards().stream().filter(value->value.id().equals(operation.reward())).findFirst();
            if(target==null||definition.isEmpty()){source.sendFailure(messages.text(language(),"reward_resume_blocked"));return;}
            try{
                var plan=definition.get().reuse(operation.plan());
                if(!validReward(target,plan)){source.sendFailure(messages.text(language(),"reward_resume_blocked"));return;}
                MainMenu.confirmRecovery(actor,config.current(),messages,plan,operation.id().toString()+" → "+target.getGameProfile().getName(),reason,()->{
                    actor.closeContainer();
                    if(!resumeAllowed(actor,target,plan)){source.sendFailure(messages.text(language(),"reward_resume_blocked"));return;}
                    deliverReward(source,target,plan,operation.id(),reason);
                },()->actor.closeContainer());
            }catch(RuntimeException invalid){source.sendFailure(messages.text(language(),"reward_resume_blocked"));}
        }));return 1;
    }
    private boolean resumeAllowed(ServerPlayer actor,ServerPlayer target,br.com.shirokaidex.rewards.RewardPlan plan){
        return !actor.hasDisconnected()&&Permissions.has(actor,"shirokaidex.admin.rewards")&&!target.hasDisconnected()&&target.getServer().getPlayerList().getPlayer(target.getUUID())==target&&validReward(target,plan);
    }
    private int rewardAdminList(CommandSourceStack source,int page){
        var actor=source.getPlayer();if(!ready||actor==null||!Permissions.has(actor,"shirokaidex.admin.rewards"))return 0;
        database.incompleteRewards(page).whenComplete((rows,failure)->source.getServer().execute(()->{
            if(stopping||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.rewards"))return;
            if(failure!=null){source.sendFailure(messages.text(language(),"reward_error"));return;}
            String text="["+(page+1)+"] "+(rows.isEmpty()?"—":rows.stream().map(op->op.id()+" | "+op.player()+" | "+op.reward()+" | "+op.state()).collect(java.util.stream.Collectors.joining("; ")));
            source.sendSuccess(()->messages.text(language(),"reward_admin_result",text),false);
        }));return 1;
    }
    private int rewardAdmin(CommandSourceStack source, String raw, String action, String reason) {
        var player = source.getPlayer();
        if (!ready || player == null || !Permissions.has(player, "shirokaidex.admin.rewards")) return 0;
        java.util.UUID id;
        try { id = java.util.UUID.fromString(raw); }
        catch (IllegalArgumentException failure) { source.sendFailure(messages.text(language(), "reward_admin_invalid")); return 0; }
        if (reason.length() > 2048 || reason.codePoints().anyMatch(Character::isISOControl)) { source.sendFailure(messages.text(language(), "reward_admin_invalid")); return 0; }
        CompletableFuture<String> result;
        if (action.equals("status")) result = database.rewardOperation(id).thenApply(value -> value.map(op -> op.id()+" | "+op.player()+" | "+op.reward()+" | "+op.state()+" | "+op.plan()).orElse("—"));
        else {
            String actor = player.getUUID().toString();
            var change = action.equals("cancel") ? database.cancelReward(id, actor, reason) : database.resolveReward(id, actor, reason);
            result = change.thenApply(changed -> changed ? "OK" : "BLOCKED");
        }
        result.whenComplete((value, failure) -> source.getServer().execute(() -> {
            if (stopping || player.hasDisconnected() || !Permissions.has(player,"shirokaidex.admin.rewards")) return;
            if (failure != null) { LOG.error("Reward administrative operation failed", failure); source.sendFailure(messages.text(language(), "reward_error")); }
            else source.sendSuccess(() -> messages.text(language(), "reward_admin_result", value), false);
        }));
        return 1;
    }

    private int rewards(CommandSourceStack source,String id,int page){
        if(!ready||source.getPlayer()==null)return 0;
        var player=source.getPlayer();var settings=config.current();long ticket=++rewardViewSequence;rewardViewRequests.put(player.getUUID(),ticket);
        var previousMenu=player.containerMenu;
        if(id==null){
            var visible=settings.rewards().stream().filter(plan->canAccess(player,plan.collection())).toList();
            database.rewardStates(player.getUUID(),visible.stream().map(br.com.shirokaidex.rewards.RewardPlan::id).toList()).whenComplete((states,error)->source.getServer().execute(()->{
                if(!validRewardView(source,player,ticket,previousMenu))return;
                var current=config.current();var nowVisible=current.rewards().stream().filter(plan->canAccess(player,plan.collection())).toList();
                if(current!=settings||!nowVisible.equals(visible)){rewards(source,null,page);return;}
                if(error!=null)LOG.error("Reward display query failed for {}",player.getUUID(),error);
                br.com.shirokaidex.menus.HomeMenu.rewards(player,current,messages,visible,rewardUnlockCache.getOrDefault(player.getUUID(),java.util.Set.of()),error==null?states:java.util.Map.of(),error!=null,page,
                        reward->rewards(source,reward,0),next->rewards(source,null,next),()->home(source));
            }));return 1;
        }
        var plan=settings.rewards().stream().filter(value->value.id().equals(id)).findFirst().orElse(null);
        if(plan==null||!canAccess(player,plan.collection())){source.sendFailure(messages.text(settings.language(),"collection_denied"));return 0;}
        database.rewardForPlayer(player.getUUID(),id).whenComplete((operation,error)->source.getServer().execute(()->{
            if(!validRewardView(source,player,ticket,previousMenu))return;
            if(config.current()!=settings){rewards(source,id,page);return;}
            if(!canAccess(player,plan.collection())){source.sendFailure(messages.text(language(),"collection_denied"));return;}
            boolean unlocked=rewardUnlockCache.getOrDefault(player.getUUID(),java.util.Set.of()).contains(plan.id());
            var view=error==null?br.com.shirokaidex.menus.layout.RewardView.preview(unlocked,plan,operation):br.com.shirokaidex.menus.layout.RewardView.unavailable();
            if(error!=null)LOG.error("Reward preview query failed for {}",player.getUUID(),error);
            MainMenu.openReward(player,settings,messages,plan,view,page,next->rewards(source,id,next),()->{player.closeContainer();rewards(source,null,0);},()->confirmReward(source,plan));
        }));return 1;
    }
    private boolean validRewardView(CommandSourceStack source,ServerPlayer player,long ticket,net.minecraft.world.inventory.AbstractContainerMenu previous){
        return ready&&!stopping&&!player.hasDisconnected()&&source.getServer().getPlayerList().getPlayer(player.getUUID())==player
                &&java.util.Objects.equals(rewardViewRequests.get(player.getUUID()),ticket)&&player.containerMenu==previous;
    }

    private void observeRewards(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty() || config.current().rewards().isEmpty()) return;
        // Round robin: at most one player considered per tick; no unbounded request queue.
        rewardPlayerCursor = Math.floorMod(rewardPlayerCursor, players.size());
        var player = players.get(rewardPlayerCursor++); var uuid = player.getUUID(); long now = System.nanoTime();
        Long previous = rewardObservedAt.get(uuid);
        if (rewardObserving.contains(uuid) || (previous != null && now - previous < 5_000_000_000L)) return;
        rewardObservedAt.put(uuid, now);
        var settings = config.current(); var known = rewardUnlockCache.getOrDefault(uuid, java.util.Set.of());
        java.util.List<br.com.shirokaidex.rewards.RewardUnlock> observed = new java.util.ArrayList<>();
        try {
            var progress = cobblemon.collections(player, settings.collections());
            for (var plan : settings.rewards()) if (!known.contains(plan.id())) {
                var counts = progress.get(plan.collection()).progress();
                if (plan.unlocked(counts)) observed.add(new br.com.shirokaidex.rewards.RewardUnlock(plan, counts.registered().orElseThrow(), counts.total()));
            }
        } catch (RuntimeException failure) { LOG.error("Reward milestone observation failed for {}", uuid, failure); return; }
        if (observed.isEmpty() && rewardUnlockCache.containsKey(uuid)) return;
        rewardObserving.add(uuid);
        database.rewardUnlocks(uuid, observed).whenComplete((ids, failure) -> server.execute(() -> {
            rewardObserving.remove(uuid);
            if (stopping || server.getPlayerList().getPlayer(uuid) != player) return;
            if (failure != null) LOG.error("Reward unlock persistence failed for {}; retry deferred", uuid, failure);
            else rewardUnlockCache.put(uuid, ids);
        }));
    }

    private boolean validReward(ServerPlayer player, br.com.shirokaidex.rewards.RewardPlan plan) {
        if (!ready || stopping || !canAccess(player, plan.collection())) return false;
        var settings = config.current();
        if (!settings.rewards().contains(plan.definition())) return false;
        return rewardUnlockCache.getOrDefault(player.getUUID(), java.util.Set.of()).contains(plan.id());
    }
    private void confirmReward(CommandSourceStack source, br.com.shirokaidex.rewards.RewardPlan plan) {
        var player = source.getPlayer();
        if (player == null || !validReward(player, plan)) { source.sendFailure(messages.text(language(), "reward_not_eligible")); return; }
        // A second menu, with explicit confirmation, is separate from the read-only preview.
        MainMenu.confirmReward(player, config.current(), messages, plan,
                () -> claimReward(source, plan), () -> rewards(source, plan.id(), 0));
    }
    private void claimReward(CommandSourceStack source, br.com.shirokaidex.rewards.RewardPlan plan) {
        var player = source.getPlayer();
        if (player == null || !validReward(player, plan)) { source.sendFailure(messages.text(language(), "reward_not_eligible")); return; }
        player.closeContainer();
        if(!plan.pokemon().isEmpty()||!plan.commands().isEmpty()){
            if(!plan.pokemon().isEmpty()&&!br.com.shirokaidex.rewards.PokemonRewardAdapter.supported()){source.sendFailure(messages.text(language(),"pokemon_backend_unavailable"));return;}
            database.rewardForPlayer(player.getUUID(),plan.id()).whenComplete((reserved,error)->source.getServer().execute(()->{
                if(stopping||player.hasDisconnected()||!validReward(player,plan))return;
                if(error!=null){source.sendFailure(messages.text(language(),"reward_error"));return;}
                if(reserved.isPresent()&&reserved.get().state()!=br.com.shirokaidex.rewards.RewardJournal.State.PENDING){source.sendFailure(messages.text(language(),"reward_result.blocked"));source.sendSuccess(()->messages.text(language(),"saved_item_operation",reserved.get().id()),false);return;}
                try{
                    var prepared=reserved.isPresent()?plan.reuse(reserved.get().plan()):plan.withPokemon(plan.pokemon().stream().map(request->br.com.shirokaidex.rewards.PokemonPayload.freeze(source.getServer(),player,request)).toList()).withCommands(plan.commands().stream().map(request->request.freeze(player.getGameProfile().getName(),player.getUUID())).toList());
                    prepared.json();deliverReward(source,prepared);
                }catch(RuntimeException failure){LOG.error("Reward preparation failed for {}",player.getUUID(),failure);source.sendFailure(messages.text(language(),"reward_error"));}
            }));return;
        }
        deliverReward(source,plan);
    }
    private void deliverReward(CommandSourceStack source,br.com.shirokaidex.rewards.RewardPlan plan){
        var player=source.getPlayer();if(player==null)return;deliverReward(source,player,plan,null,"");
    }
    private void deliverReward(CommandSourceStack source,ServerPlayer player,br.com.shirokaidex.rewards.RewardPlan plan,java.util.UUID recovery,String recoveryReason){
        var actor=source.getPlayer();if(actor==null||!(recovery==null?validReward(player,plan):resumeAllowed(actor,player,plan)))return;
        java.util.function.Predicate<br.com.shirokaidex.rewards.RewardPlan> permitted=current->recovery==null?validReward(player,current):resumeAllowed(actor,player,current);
        if(!plan.currencies().isEmpty()&&(!FabricLoader.getInstance().isModLoaded("impactor")||!br.com.shirokaidex.rewards.ImpactorCurrencyPort.supported())){source.sendFailure(messages.text(language(),"currency_backend_unavailable"));return;}
        br.com.shirokaidex.rewards.PokemonRewardAdapter pokemonAdapter=plan.pokemon().isEmpty()?null:new br.com.shirokaidex.rewards.PokemonRewardAdapter(source.getServer(),player,permitted);
        if(pokemonAdapter!=null){pokemonDeliveries.add(pokemonAdapter);source.sendSuccess(()->messages.text(language(),"pokemon_processing"),false);}
        br.com.shirokaidex.rewards.RewardDelivery.Adapter adapter=pokemonAdapter==null?new br.com.shirokaidex.rewards.ItemRewardAdapter(source.getServer(),player,permitted):pokemonAdapter;
        if(!plan.currencies().isEmpty())adapter=new br.com.shirokaidex.rewards.CurrencyRewardAdapter(adapter,new br.com.shirokaidex.rewards.ImpactorCurrencyPort(source.getServer(),player,permitted));
        if(!plan.commands().isEmpty())adapter=new br.com.shirokaidex.rewards.CommandRewardAdapter(source.getServer(),player,permitted,()->config.current().commandPolicy());
        (recovery==null?rewardDelivery.claim(player.getUUID(),plan,adapter):rewardDelivery.resume(recovery,player.getUUID(),plan,adapter,actor.getUUID().toString(),recoveryReason))
                .whenComplete((result, failure) -> source.getServer().execute(() -> {
                    if(pokemonAdapter!=null){pokemonDeliveries.remove(pokemonAdapter);pokemonAdapter.close();}
                    if (stopping || actor.hasDisconnected()) return;
                    if (failure != null) { LOG.error("Reward delivery failed for {}", player.getUUID(), failure); source.sendFailure(messages.text(language(), "reward_error")); }
                    else {
                        source.sendSuccess(() -> messages.text(language(), "reward_result." + result.name().toLowerCase(java.util.Locale.ROOT)), false);
                        database.rewardForPlayer(player.getUUID(),plan.id()).thenAccept(value->value.ifPresent(op->source.getServer().execute(()->{
                            if(!stopping&&!actor.hasDisconnected())source.sendSuccess(()->messages.text(language(),"saved_item_operation",op.id()),false);
                        })));
                    }
                }));
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
                    cobblemon.clear(); placeholderCache.clear(); rewardObservedAt.clear();
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
