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
    private final br.com.shirokaidex.captures.CaptureAccess captures=new br.com.shirokaidex.captures.CaptureAccess();
    private final br.com.shirokaidex.defeats.DefeatAccess defeats=new br.com.shirokaidex.defeats.DefeatAccess();
    private final Messages messages = new Messages();
    private record PublicMask(Settings settings,Database.PublicPrivacy privacy) { }
    private final java.util.Map<java.util.UUID,PublicMask> publicMasks=new java.util.HashMap<>();
    private final java.util.LinkedHashSet<java.util.UUID> privacyQueue=new java.util.LinkedHashSet<>();
    private final java.util.Set<java.util.UUID> privacyRefreshing=new java.util.HashSet<>();
    private final PlaceholderCache placeholderCache = new PlaceholderCache();
    private final PublicMetricCache publicMetricCache=new PublicMetricCache();
    private PlaceholderBridge placeholderBridge;
    private SpawnCatalog spawnCatalog = SpawnCatalog.unavailable();
    private boolean spawnRefreshPending;
    private final java.util.Map<java.util.UUID,net.minecraft.world.inventory.AbstractContainerMenu> publicViews=new java.util.HashMap<>();
    private record PendingSearch(ServerPlayer player,String collection,Wiki.Request request,long expires) { }
    private final java.util.concurrent.ConcurrentHashMap<java.util.UUID,PendingSearch> searches=new java.util.concurrent.ConcurrentHashMap<>();
    private int notificationCursor;
    private final java.util.Map<String,Long> notificationObserved=new java.util.HashMap<>();
    private final java.util.Set<String> notificationPending=new java.util.HashSet<>();
    private int profileCursor;
    private final java.util.Map<java.util.UUID,Long> profileObservedAt=new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> profileObserving=new java.util.HashSet<>();
    private long lastCompetitionSync;
    private boolean competitionSyncPending;
    private long lastHistoryCleanup;
    private boolean historyCleanupPending;
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
        config = new ConfigManager(directory,true);
        database = new Database(directory.resolve("data/shirokaidex.sqlite"));
        rewardDelivery = new br.com.shirokaidex.rewards.RewardDelivery(database);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            if (dispatcher.getRoot().getChild("dex") != null) {
                LOG.error("Command /dex already exists; refusing to overwrite it");
                return;
            }
            var root=literal("dex").executes(context->0)
                .then(literal("admin").requires(source->Permissions.mayReload(source.getPlayer())||Permissions.has(source.getPlayer(),"shirokaidex.admin.rewards")||Permissions.has(source.getPlayer(),"shirokaidex.admin.items")||Permissions.has(source.getPlayer(),"shirokaidex.admin.captures"))
                    .then(literal("reload").requires(source->Permissions.mayReload(source.getPlayer())).executes(context->reload(context.getSource())))
                    .then(rewardAdminCommands()).then(savedItemCommands()).then(captureHistoryCommands()));
            for(String name:List.of("nacional","special","shiny","forms"))root.then(literal(name).executes(context->directDex(context.getSource(),br.com.shirokaidex.config.DirectMenuCatalog.collection(name),0)));
            dispatcher.register(root);
            if(dispatcher.getRoot().getChild("dexrank")==null){var rank=literal("dexrank").executes(context->0);for(String kind:List.of("captura","derrotados","dex"))rank.then(literal(kind).executes(context->directRanking(context.getSource(),kind)));dispatcher.register(rank);}else LOG.error("Command /dexrank already exists; preserving its provider");
            if(dispatcher.getRoot().getChild("wiki")==null)dispatcher.register(literal("wiki").executes(context->0).then(argument("collection",StringArgumentType.word()).suggests((context,builder)->{for(String name:List.of("nacional","special","shiny","forms"))if(name.startsWith(builder.getRemaining()))builder.suggest(name);return builder.buildFuture();}).executes(context->directWiki(context.getSource(),br.com.shirokaidex.config.DirectMenuCatalog.collection(StringArgumentType.getString(context,"collection")),Wiki.Request.initial(""))).then(argument("query",StringArgumentType.greedyString()).executes(context->directWiki(context.getSource(),br.com.shirokaidex.config.DirectMenuCatalog.collection(StringArgumentType.getString(context,"collection")),Wiki.Request.initial(StringArgumentType.getString(context,"query")))))));else LOG.error("Command /wiki already exists; preserving its provider");
        });
        // Consume private wiki input before default-phase chat routers broadcast/cancel it.
        // Fabric still validates the signed message normally before this event.
        var wikiSearchPhase=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("shirokaidex","wiki_search");
        net.fabricmc.fabric.api.message.v1.ServerMessageEvents.ALLOW_CHAT_MESSAGE.addPhaseOrdering(wikiSearchPhase,net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);
        net.fabricmc.fabric.api.message.v1.ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(wikiSearchPhase,(message,sender,params)->{
            var pending=searches.remove(sender.getUUID());if(pending==null)return true;
            String input=message.signedContent().strip();sender.server.execute(()->{
                if(!ready||stopping||sender.server!=activeServer||pending.player()!=sender||sender.hasDisconnected())return;
                if(System.nanoTime()>pending.expires()){directMessage(sender.createCommandSourceStack(),"search_expired");return;}
                if(input.equalsIgnoreCase("cancelar")||input.equalsIgnoreCase("cancel")){directMessage(sender.createCommandSourceStack(),"search_cancelled");return;}
                if(input.length()>128){directMessage(sender.createCommandSourceStack(),"unavailable");return;}
                var old=pending.request();directWiki(sender.createCommandSourceStack(),pending.collection(),new Wiki.Request(input,old.filter(),old.order(),0,"",old.type(),old.category(),-1,Wiki.Facet.NONE,0,-1));
            });return false;
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{
            if(!ready||stopping||!world.getServer().isSameThread()||!(entity instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity pokemon))return;
            try{var proof=captures.observe(pokemon,br.com.shirokaidex.captures.NativeSpawnProof.Source.ENTITY_LOAD);if(proof!=null)database.recordNativeSpawnProof(proof).exceptionally(failure->{LOG.error("Native spawn proof persistence failed",failure);return false;});}
            catch(RuntimeException failure){LOG.error("Native spawn evidence unavailable",failure);}
        });
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe((Consumer<com.cobblemon.mod.common.api.events.pokeball.ThrownPokeballHitEvent>) event->{
            if(ready&&!stopping&&activeServer!=null&&activeServer.isSameThread()){
                try{var proof=captures.hit(event);if(proof!=null)database.recordNativeSpawnProof(proof).exceptionally(failure->{LOG.error("Native spawn proof persistence failed",failure);return false;});}
                catch(RuntimeException failure){LOG.error("Native pre-capture evidence unavailable",failure);}
            }
        });
        CobblemonEvents.BATTLE_STARTED_POST.subscribe((Consumer<com.cobblemon.mod.common.api.events.battles.BattleStartedEvent.Post>)event->{if(ready&&!stopping&&activeServer!=null&&activeServer.isSameThread())defeats.started(event,captures);});
        CobblemonEvents.BATTLE_FAINTED.subscribe((Consumer<com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent>)event->{if(!ready||stopping||activeServer==null||!activeServer.isSameThread())return;var record=defeats.fainted(event);if(record!=null)database.recordDefeat(record).exceptionally(failure->{LOG.error("Defeat persistence failed",failure);return false;});});
        CobblemonEvents.BATTLE_ENDED.subscribe((Consumer<com.cobblemon.mod.common.api.events.battles.BattleEndedEvent>)event->{if(activeServer!=null&&activeServer.isSameThread())defeats.ended(event.getBattle().getBattleId());});
        CobblemonEvents.POKE_BALL_CAPTURE_CALCULATED.subscribe((Consumer<com.cobblemon.mod.common.api.events.pokeball.PokeBallCaptureCalculatedEvent>)event->{if(!ready||stopping||activeServer==null||!activeServer.isSameThread())return;try{var proof=captures.calculated(event);if(proof!=null)database.recordNativeSpawnProof(proof).exceptionally(error->{LOG.error("Pre-calculation capture proof failed",error);return false;});}catch(RuntimeException error){LOG.error("Capture calculation evidence unavailable",error);}});
        CobblemonEvents.POKEMON_CAPTURED.subscribe((Consumer<com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent>) event->{
            if(!ready||stopping||activeServer==null||!activeServer.isSameThread())return;
            try{var observation=captures.captured(event,config.current().categories());var capturePlayer=event.getPlayer();var captureServer=capturePlayer.server;database.recordObservedCapture(observation,config.current().captureHistory().enabled()).whenComplete((inserted,failure)->{if(failure!=null)LOG.error("Capture persistence failed for {}; statistics may be incomplete",observation.capture().player(),failure);else if(Boolean.TRUE.equals(inserted))captureServer.execute(()->{if(ready&&!stopping&&activeServer==captureServer&&captureServer.getPlayerList().getPlayer(observation.capture().player())==capturePlayer){profileObservedAt.remove(capturePlayer.getUUID());refreshCapturedShiny(capturePlayer,true);}});});}
            catch(RuntimeException failure){LOG.error("Capture evidence unavailable; no legitimate capture inferred",failure);}
        });
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe((Consumer<PokedexDataChangedEvent.Post>) event -> {
            MinecraftServer server = activeServer;
            if (server != null) server.execute(() -> { rewardObservedAt.remove(event.getPlayerUUID()); profileObservedAt.remove(event.getPlayerUUID()); cobblemon.invalidate(event.getPlayerUUID()); if (placeholderBridge != null) placeholderCache.invalidate(event.getPlayerUUID()); });
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            activeServer = server; stopping = false;notificationObserved.clear();notificationPending.clear();searches.clear(); profileObservedAt.clear();profileObserving.clear();publicViews.clear();publicMasks.clear();privacyQueue.clear();privacyRefreshing.clear();lastCompetitionSync=System.nanoTime()-java.util.concurrent.TimeUnit.SECONDS.toNanos(1);competitionSyncPending=false;lastHistoryCleanup=System.nanoTime()-java.util.concurrent.TimeUnit.SECONDS.toNanos(60); historyCleanupPending=false; cobblemon.clear(); captures.clear();defeats.clear();
            var registry = cobblemon.validationSnapshot();
            config.setMenuValidator(registry::validateMenus);
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
                    LOG.info("Shirokai Dex ready: configuration validated, SQLite migrations applied/verified");
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
            if(ready&&!stopping&&!historyCleanupPending&&System.nanoTime()-lastHistoryCleanup>=java.util.concurrent.TimeUnit.SECONDS.toNanos(60)){
                lastHistoryCleanup=System.nanoTime();historyCleanupPending=true;
                database.cleanupCaptureHistory(config.current().captureHistory(),Instant.now()).whenComplete((removed,failure)->server.execute(()->{
                    if(activeServer!=server)return;historyCleanupPending=false;
                    if(failure!=null)LOG.error("Capture history cleanup failed; retained data will be retried later",failure);
                    else if(removed>0)LOG.info("Removed {} expired verified capture history rows; permanent records preserved",removed);
                }));
            }
            if(ready){long now=System.nanoTime();searches.entrySet().removeIf(entry->now>entry.getValue().expires());}
            if (ready) {var online=server.getPlayerList().getPlayers();if(!online.isEmpty())refreshCapturedShiny(online.get(Math.floorMod(shinyCursor++,online.size())),false);observeRewards(server);observeProfiles(server);observeNotifications(server);}
            if (!ready || placeholderBridge == null) return;
            refreshPublicPrivacy(server);
            refreshPublicMetrics(server);
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
            if (placeholderBridge != null){placeholderCache.invalidate(handler.player.getUUID());privacyQueue.add(handler.player.getUUID());}
            var uuid = handler.player.getUUID();
            String name = handler.player.getGameProfile().getName();
            Instant time = Instant.now();
            if (initialization != null) initialization.thenCompose(unused -> database.recordLogin(uuid, name, time))
                    .exceptionally(failure -> { LOG.error("Could not persist player login {}", uuid, failure); return null; });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { searches.remove(handler.player.getUUID());notificationObserved.remove(handler.player.getUUID().toString());rewardViewRequests.remove(handler.player.getUUID());publicViews.remove(handler.player.getUUID());publicMasks.remove(handler.player.getUUID());privacyQueue.remove(handler.player.getUUID());profileObservedAt.remove(handler.player.getUUID()); rewardUnlockCache.remove(handler.player.getUUID()); rewardObservedAt.remove(handler.player.getUUID()); cobblemon.forget(handler.player.getUUID());shinyRequestedAt.remove(handler.player.getUUID()); placeholderCache.remove(handler.player.getUUID());publicMetricCache.remove(handler.player.getUUID()); });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { stopping = true; captures.clear();defeats.clear(); java.util.List.copyOf(pokemonDeliveries).forEach(br.com.shirokaidex.rewards.PokemonRewardAdapter::close); pokemonDeliveries.clear(); payloadValidations.forEach(future -> future.completeExceptionally(new IllegalStateException("Server stopping"))); ready = false; activeServer = null;notificationObserved.clear();notificationPending.clear();searches.clear(); rewardViewRequests.clear(); rewardUnlockCache.clear(); rewardObservedAt.clear();profileObservedAt.clear();profileObserving.clear();publicViews.clear();publicMasks.clear();privacyQueue.clear();privacyRefreshing.clear();shinyRequestedAt.clear(); cobblemon.clear(); placeholderCache.clear();publicMetricCache.clear(); spawnCatalog = SpawnCatalog.unavailable(); spawnRefreshPending = false;
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
    private void directMessage(CommandSourceStack source,String key){var state=config.snapshot();if(state==null||state.menus()==null)return;String template=state.menus().messages().getOrDefault(key,key);source.sendSuccess(()->br.com.shirokaidex.menus.direct.RichText.render(template,java.util.Map.of("player",source.getPlayer()==null?"":source.getPlayer().getGameProfile().getName())),false);}
    private int directDex(CommandSourceStack source,String id,int page){
        var player=source.getPlayer();if(!ready||stopping||player==null||id==null)return 0;searches.remove(player.getUUID());var state=config.snapshot();
        if(id.equals("forms")){br.com.shirokaidex.menus.direct.DirectMenus.future(player,state.menus().menus().get("dex_forms"));return 1;}
        if(!canAccess(player,id))return 0;var definition=state.settings().collections().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow();var plans=state.settings().rewards().stream().filter(p->p.collection().equals(id)).toList();var previous=player.containerMenu;long ticket=beginView(player);
        database.rewardStates(player.getUUID(),plans.stream().map(br.com.shirokaidex.rewards.RewardPlan::id).toList()).whenComplete((states,error)->source.getServer().execute(()->{
            if(!validRewardView(source,player,ticket,previous)||!canAccess(player,id))return;if(state!=config.snapshot()){directDex(source,id,page);return;}if(error!=null){directMessage(source,"unavailable");return;}
            br.com.shirokaidex.menus.direct.DirectMenus.dex(player,state.menus(),definition,cobblemon.collections(player,state.settings().collections()).get(id).progress(),plans,rewardUnlockCache.getOrDefault(player.getUUID(),java.util.Set.of()),states,page,plan->{if(validReward(player,plan))claimReward(source,plan);},next->directDex(source,id,next),()->directWiki(source,id,Wiki.Request.initial("")));
        }));return 1;
    }
    private int directWiki(CommandSourceStack source,String id,Wiki.Request request){
        var player=source.getPlayer();if(!ready||stopping||player==null||id==null)return 0;searches.remove(player.getUUID());var state=config.snapshot();if(id.equals("forms")){br.com.shirokaidex.menus.direct.DirectMenus.future(player,state.menus().menus().get("wiki_forms"));return 1;}
        if(!canAccess(player,id)||request.query().length()>128)return 0;var definition=state.settings().collections().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow();var spec=state.menus().menus().get("wiki_"+br.com.shirokaidex.config.DirectMenuCatalog.command(id));var page=Wiki.select(cobblemon.wiki(player),definition,request,spec.contentSlots().size());var previous=player.containerMenu;long ticket=beginView(player);
        database.capturedSpecies(player.getUUID(),definition.mode()==CollectionDefinition.Mode.SHINY_CAPTURED).whenComplete((captured,error)->source.getServer().execute(()->{
            if(!validRewardView(source,player,ticket,previous)||!canAccess(player,id))return;if(state!=config.snapshot()){directWiki(source,id,request);return;}
            br.com.shirokaidex.menus.direct.DirectMenus.wiki(player,spec,definition,request,page,spawnCatalog,error==null?captured:null,next->directWiki(source,id,next),()->{if(!ready||!canAccess(player,id))return;beginView(player);player.closeContainer();searches.put(player.getUUID(),new PendingSearch(player,id,request,System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(60)));directMessage(source,"search_prompt");});
        }));return 1;
    }
    private int directRanking(CommandSourceStack source,String kind){
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;searches.remove(player.getUUID());var state=config.snapshot();var previous=player.containerMenu;long ticket=beginView(player);var spec=state.menus().menus().get("rank_"+kind);
        if(kind.equals("derrotados")){database.defeatRanking().whenComplete((result,error)->source.getServer().execute(()->{if(!validRewardView(source,player,ticket,previous))return;if(error!=null){directMessage(source,"unavailable");return;}if(state!=config.snapshot()||result.privacyRevision()!=database.privacyRevision()){directRanking(source,kind);return;}var lines=new java.util.ArrayList<br.com.shirokaidex.menus.direct.DirectMenus.RankingLine>();int rank=1;for(var e:result.entries())lines.add(new br.com.shirokaidex.menus.direct.DirectMenus.RankingLine(rank++,e.name(),Long.toString(e.total())));br.com.shirokaidex.menus.direct.DirectMenus.ranking(player,spec,java.util.Map.of("defeats",lines),state.menus().messages().get("no_data"));markPublicView(player,true);}));return 1;}
        boolean dex=kind.equals("dex");var definitions=visibleRankings(player,state.settings()).stream().filter(r->dex?r.metric()==br.com.shirokaidex.rankings.CaptureRanking.Metric.COLLECTION:java.util.Set.of(br.com.shirokaidex.rankings.CaptureRanking.Metric.TOTAL,br.com.shirokaidex.rankings.CaptureRanking.Metric.SHINY,br.com.shirokaidex.rankings.CaptureRanking.Metric.LEGENDARY).contains(r.metric())).limit(3).toList();var versions=cobblemon.collectionVersions(state.settings().collections());
        var jobs=definitions.stream().map(r->database.captureRankingView(r,0,state.settings().statisticsPrivacy(),versions.get(r.key()),r.key().equals("shiny")?br.com.shirokaidex.profiles.StatisticsPrivacy.Key.SHINY_CAPTURES:null)).toList();CompletableFuture.allOf(jobs.toArray(CompletableFuture[]::new)).whenComplete((unused,error)->source.getServer().execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;if(error!=null){directMessage(source,"unavailable");return;}if(state!=config.snapshot()||jobs.stream().anyMatch(j->j.join().privacyRevision()!=database.privacyRevision())||definitions.stream().anyMatch(r->!Permissions.has(player,r.permission())||dex&&!canAccess(player,r.key()))){directRanking(source,kind);return;}
            var boards=new java.util.HashMap<String,java.util.List<br.com.shirokaidex.menus.direct.DirectMenus.RankingLine>>();for(int i=0;i<definitions.size();i++){var r=definitions.get(i);var lines=new java.util.ArrayList<br.com.shirokaidex.menus.direct.DirectMenus.RankingLine>();int rank=1;for(var entry:jobs.get(i).join().page().entries()){String score=entry.total()==null?Long.toString(entry.score()):DexProgress.known(entry.total(),entry.registered()).formattedPercentage(state.settings().language()).orElse("N/D");lines.add(new br.com.shirokaidex.menus.direct.DirectMenus.RankingLine(rank++,entry.name(),score));}boards.put(dex?r.key():r.metric().name().toLowerCase(java.util.Locale.ROOT),lines);}br.com.shirokaidex.menus.direct.DirectMenus.ranking(player,spec,boards,state.menus().messages().get("no_data"));markPublicView(player,true);
        }));return 1;
    }
    private int home(CommandSourceStack source){
        if(!ready||source.getPlayer()==null)return 0;if(config.current().home().focused())return focusedHome(source);var player=source.getPlayer();var settings=config.current();
        java.util.Map<String,Runnable> actions=new java.util.HashMap<>();actions.put("dex",()->selector(source,0));actions.put("rewards",()->rewards(source,null,0));actions.put("close",player::closeContainer);
        if(canAccess(player,"national"))actions.put("wiki",()->wiki(source,"national",Wiki.Request.initial("")));
        if(source.getServer().getCommands().getDispatcher().getRoot().getChild("menu")!=null)actions.put("back",()->{
            if(!ready)return;player.closeContainer();source.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(),"menu");
        });
        var progress=cobblemon.collections(player).get(DexCollection.NATIONAL).progress();
        actions.put("profile",()->profile(source,null,null,0));
        actions.put("settings",()->settings(source));
        actions.put("achievements",()->achievements(source,null,0));
        actions.put("competitions",()->competitions(source,null,0,false));
        actions.put("history",()->competitions(source,null,0,true));
        br.com.shirokaidex.menus.HomeMenu.open(player,settings,messages,progress,visible(player,settings).size(),(int)settings.rewards().stream().filter(plan->canAccess(player,plan.collection())).count(),actions);return 1;
    }
    private int focusedHome(CommandSourceStack source){
        var player=source.getPlayer();var settings=config.current();var previous=player.containerMenu;long ticket=beginView(player);
        database.competitionEditions(false).whenComplete((editions,error)->source.getServer().execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;if(settings!=config.current()){home(source);return;}
            boolean active=error==null&&editions.stream().anyMatch(e->e.state()==br.com.shirokaidex.competitions.CompetitionStore.State.ACTIVE&&Instant.now().isBefore(e.end())&&e.definition().metric()==br.com.shirokaidex.rankings.CaptureRanking.Metric.TOTAL&&Permissions.has(player,e.definition().permission()));
            br.com.shirokaidex.menus.FocusedMenu.home(player,settings,active,()->selector(source,0),()->focusedRankingRoot(source),()->focusedEvents(source));
        }));return 1;
    }
    private int focusedCollection(CommandSourceStack source,String id){
        var player=source.getPlayer();if(player==null||!canAccess(player,id))return 0;var settings=config.current();var definition=settings.collections().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow();
        var plans=settings.rewards().stream().filter(r->r.collection().equals(id)&&r.minimumPercent()!=null).toList();var previous=player.containerMenu;long ticket=beginView(player);
        database.rewardStates(player.getUUID(),plans.stream().map(br.com.shirokaidex.rewards.RewardPlan::id).toList()).whenComplete((states,error)->source.getServer().execute(()->{
            if(!validRewardView(source,player,ticket,previous)||!canAccess(player,id))return;if(settings!=config.current()){focusedCollection(source,id);return;}if(error!=null){source.sendFailure(messages.text(language(),"reward_error"));return;}
            br.com.shirokaidex.menus.FocusedMenu.collection(player,settings,definition,cobblemon.collections(player,settings.collections()).get(id).progress(),plans,rewardUnlockCache.getOrDefault(player.getUUID(),java.util.Set.of()),states,plan->{if(validReward(player,plan))claimReward(source,plan);else source.sendFailure(messages.text(language(),"reward_not_eligible"));},()->wiki(source,id,Wiki.Request.initial("")),()->selector(source,0));
        }));return 1;
    }
    private int focusedRankingRoot(CommandSourceStack source){var p=source.getPlayer();if(!ready||p==null)return 0;br.com.shirokaidex.menus.FocusedMenu.rankingRoot(p,config.current(),()->focusedBoards(source,true),()->focusedBoards(source,false),()->defeatedRanking(source),()->home(source));return 1;}
    private int focusedBoards(CommandSourceStack source,boolean dex){
        var player=source.getPlayer();if(!ready||player==null)return 0;var settings=config.current();var versions=cobblemon.collectionVersions(settings.collections());var previous=player.containerMenu;long ticket=beginView(player);
        var definitions=visibleRankings(player,settings).stream().filter(r->dex?r.metric()==br.com.shirokaidex.rankings.CaptureRanking.Metric.COLLECTION&&java.util.Set.of("national","special","shiny").contains(r.key()):java.util.Set.of(br.com.shirokaidex.rankings.CaptureRanking.Metric.TOTAL,br.com.shirokaidex.rankings.CaptureRanking.Metric.SHINY,br.com.shirokaidex.rankings.CaptureRanking.Metric.LEGENDARY).contains(r.metric())).limit(3).toList();
        var jobs=definitions.stream().map(r->database.captureRankingView(r,0,settings.statisticsPrivacy(),versions.get(r.key()),r.key().equals("shiny")?br.com.shirokaidex.profiles.StatisticsPrivacy.Key.SHINY_CAPTURES:null)).toList();
        CompletableFuture.allOf(jobs.toArray(CompletableFuture[]::new)).whenComplete((unused,error)->source.getServer().execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;if(error!=null){source.sendFailure(messages.text(language(),"capture_stats_unavailable"));return;}
            if(settings!=config.current()||jobs.stream().anyMatch(j->j.join().privacyRevision()!=database.privacyRevision())||!definitions.stream().allMatch(r->Permissions.has(player,r.permission())&&(r.metric()!=br.com.shirokaidex.rankings.CaptureRanking.Metric.COLLECTION||canAccess(player,r.key())))){focusedBoards(source,dex);return;}
            var pages=new java.util.LinkedHashMap<String,br.com.shirokaidex.rankings.CaptureRankings.Page>();for(int i=0;i<definitions.size();i++)pages.put(definitions.get(i).id(),jobs.get(i).join().page());br.com.shirokaidex.menus.FocusedMenu.boards(player,settings,dex,definitions,pages,()->focusedRankingRoot(source));markPublicView(player,true);
        }));return 1;
    }
    private int defeatedRanking(CommandSourceStack source){var p=source.getPlayer();if(!ready||p==null)return 0;var previous=p.containerMenu;long ticket=beginView(p);database.defeatRanking().whenComplete((ranking,error)->source.getServer().execute(()->{if(!validRewardView(source,p,ticket,previous))return;if(error!=null)return;if(ranking.privacyRevision()!=database.privacyRevision()){defeatedRanking(source);return;}br.com.shirokaidex.menus.FocusedMenu.defeated(p,config.current(),ranking.entries(),()->focusedRankingRoot(source));markPublicView(p,true);}));return 1;}
    private void focusedEnterEvent(CommandSourceStack source,String id){var p=source.getPlayer();if(!ready||p==null)return;var previous=p.containerMenu;long ticket=beginView(p);database.competitionEdition(id).whenComplete((found,error)->source.getServer().execute(()->{if(!validRewardView(source,p,ticket,previous)||error!=null||found.isEmpty())return;var edition=found.get();if(edition.state()!=br.com.shirokaidex.competitions.CompetitionStore.State.ACTIVE||!Instant.now().isBefore(edition.end())||edition.definition().metric()!=br.com.shirokaidex.rankings.CaptureRanking.Metric.TOTAL||!Permissions.has(p,edition.definition().permission()))return;database.enrollCompetition(id,p.getUUID(),Instant.now()).whenComplete((joined,failure)->source.getServer().execute(()->{if(validRewardView(source,p,ticket,previous)&&failure==null)competition(source,id,0,false);}));}));}
    private int focusedEvents(CommandSourceStack source){
        var p=source.getPlayer();if(!ready||p==null)return 0;var previous=p.containerMenu;long ticket=beginView(p);
        database.competitionEditions(false).whenComplete((editions,error)->source.getServer().execute(()->{if(!validRewardView(source,p,ticket,previous))return;if(error!=null)return;var visible=editions.stream().filter(e->e.definition().metric()==br.com.shirokaidex.rankings.CaptureRanking.Metric.TOTAL&&Permissions.has(p,e.definition().permission())).toList();br.com.shirokaidex.menus.FocusedMenu.events(p,config.current(),visible,id->focusedEnterEvent(source,id),()->home(source));}));return 1;
    }
    private int selector(CommandSourceStack source, int page) {
        if (!ready || source.getPlayer() == null) return 0;
        ServerPlayer player = source.getPlayer();
        Settings settings = config.current();
        if(settings.home().focused()){br.com.shirokaidex.menus.FocusedMenu.selector(player,settings,cobblemon.collections(player,settings.collections()),id->collection(source,id,true),()->home(source));return 1;}
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
        if(ready&&menu&&config.current().home().focused())return focusedCollection(source,id);
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
            var previous=player.containerMenu;long ticket=beginView(player);
            database.capturedSpecies(player.getUUID(),definition.mode()==CollectionDefinition.Mode.SHINY_CAPTURED).whenComplete((captured,error)->source.getServer().execute(()->{
                if(!validRewardView(source,player,ticket,previous)||!canAccess(player,id))return;if(settings!=config.current()){wiki(source,id,request);return;}
                MainMenu.openWiki(player,settings,messages,definition,request,page,spawnCatalog,next->wiki(source,id,next),()->collection(source,id,true),()->{player.closeContainer();source.sendSuccess(()->messages.text(language(),"wiki_search_hint",id),false);},error==null?captured:null);
            }));
            return 1;
        } catch (RuntimeException failure) {
            LOG.error("Wiki query failed for {}", player.getUUID(), failure);
            source.sendFailure(messages.text(language(), "query_failed")); return 0;
        }
    }

    private boolean publiclyVisible(ServerPlayer player,Settings settings,br.com.shirokaidex.profiles.StatisticsPrivacy.Key key){
        if(!settings.statisticsPrivacy().enabled())return true;if(!settings.statisticsPrivacy().allowPlayerOverride())return settings.statisticsPrivacy().defaults().get(key);
        var mask=publicMasks.get(player.getUUID());if(mask==null||mask.settings()!=settings||mask.privacy().privacyRevision()!=database.privacyRevision()){privacyQueue.add(player.getUUID());return false;}
        return Boolean.TRUE.equals(mask.privacy().visible().get(key));
    }
    private void refreshPublicPrivacy(MinecraftServer server){
        for(int i=0;i<4&&privacyRefreshing.size()<4;i++){
            if(privacyQueue.isEmpty())return;var iterator=privacyQueue.iterator();var uuid=iterator.next();iterator.remove();if(privacyRefreshing.contains(uuid)){privacyQueue.add(uuid);continue;}var player=server.getPlayerList().getPlayer(uuid);if(player==null)continue;var settings=config.current();privacyRefreshing.add(uuid);
            database.publicPrivacy(uuid,settings.statisticsPrivacy()).whenComplete((value,failure)->server.execute(()->{if(activeServer!=server)return;privacyRefreshing.remove(uuid);if(!ready||stopping||server.getPlayerList().getPlayer(uuid)!=player)return;if(failure!=null){LOG.error("Public placeholder privacy unavailable",failure);return;}if(config.current()!=settings||value.privacyRevision()!=database.privacyRevision()){privacyQueue.add(uuid);return;}publicMasks.put(uuid,new PublicMask(settings,value));}));
        }
    }
    private void refreshPublicMetrics(MinecraftServer server) {
        for(int i=0;i<4;i++) {
            var request=publicMetricCache.poll();if(request==null)return;
            var player=server.getPlayerList().getPlayer(request.key().player());
            if(player==null){publicMetricCache.remove(request.key().player());continue;}
            var settings=config.current();
            if(request.settings()!=settings||request.privacy()!=database.privacyRevision()){publicMetricCache.complete(request,System.nanoTime(),PlaceholderValues.UNKNOWN);continue;}
            if(request.key().metric().equals("available_rewards")) {
                var plans=settings.rewards().stream().filter(plan->canAccess(player,plan.collection())).toList();
                boolean protectedShiny=plans.stream().anyMatch(plan->settings.collections().stream().anyMatch(c->c.id().equals(plan.collection())&&c.mode()==CollectionDefinition.Mode.SHINY_CAPTURED));
                if(protectedShiny&&!publiclyVisible(player,settings,br.com.shirokaidex.profiles.StatisticsPrivacy.Key.SHINY_CAPTURES)){publicMetricCache.complete(request,System.nanoTime(),PlaceholderValues.UNKNOWN);continue;}
                database.publicRewardAvailability(player.getUUID(),plans,Instant.now()).whenComplete((result,failure)->server.execute(()->{
                    if(activeServer!=server)return;String text=PlaceholderValues.UNKNOWN;
                    if(failure!=null)LOG.error("Reward availability placeholder failed",failure);
                    else if(ready&&!stopping&&server.getPlayerList().getPlayer(player.getUUID())==player&&settings==config.current()&&result.privacyRevision()==database.privacyRevision())text=Long.toString(result.count());
                    publicMetricCache.complete(request,System.nanoTime(),text);
                }));continue;
            }
            var collections=visible(player,settings).stream().map(CollectionDefinition::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
            var rankings=visibleRankings(player,settings).stream().filter(r->request.key().metric().equals("rank")&&r.id().equals(request.key().argument())).toList();
            // null viewer deliberately requests the public view, even for the player's own TAB context.
            database.profile(player.getUUID().toString(),null,settings,cobblemon.collectionVersions(settings.collections()),collections,rankings,Instant.now()).whenComplete((result,failure)->server.execute(()->{
                if(activeServer!=server)return;
                String text=PlaceholderValues.UNKNOWN;
                if(failure!=null)LOG.error("Public placeholder snapshot failed",failure);
                else if(ready&&!stopping&&server.getPlayerList().getPlayer(player.getUUID())==player&&settings==config.current()&&result.privacyRevision()==database.privacyRevision())text=PublicMetricValues.format(result.data().orElse(null),request.key().metric(),request.key().argument());
                publicMetricCache.complete(request,System.nanoTime(),text);
            }));
        }
    }
    private String placeholderValue(ServerPlayer player, String metric, String argument) {
        if (!ready || player == null || activeServer == null || !activeServer.isSameThread()
                || activeServer.getPlayerList().getPlayer(player.getUUID()) != player) return PlaceholderValues.UNKNOWN;
        Settings settings = config.current();
        if(metric.equals("custom")) {
            var custom=settings.customPlaceholders().stream().filter(c->c.id().equals(argument)).findFirst().orElse(null);
            return custom==null?PlaceholderValues.UNKNOWN:custom.render(placeholderValue(player,custom.metric(),custom.argument()));
        }
        if(PublicMetricValues.METRICS.contains(metric)) {
            String arg=PublicMetricValues.argument(metric,argument);if(arg==null)return PlaceholderValues.UNKNOWN;
            if(metric.equals("rank")&&visibleRankings(player,settings).stream().noneMatch(r->r.id().equals(arg)))return PlaceholderValues.UNKNOWN;
            String access="";
            if(metric.equals("medals"))access=visible(player,settings).stream().map(CollectionDefinition::id).sorted().collect(java.util.stream.Collectors.joining(","));
            if(metric.equals("available_rewards"))access=settings.rewards().stream().filter(plan->canAccess(player,plan.collection())).map(br.com.shirokaidex.rewards.RewardPlan::id).sorted().collect(java.util.stream.Collectors.joining(","));
            return publicMetricCache.read(new PublicMetricCache.Key(player.getUUID(),metric,arg,access),settings,database.privacyRevision(),System.nanoTime());
        }
        String id = PlaceholderValues.collection(argument);
        if (id == null) return PlaceholderValues.UNKNOWN;
        var definition = settings.collections().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        if (definition == null || !Permissions.has(player, definition.permission())) return PlaceholderValues.UNKNOWN;
        if(definition.mode()==CollectionDefinition.Mode.SHINY_CAPTURED&&!metric.equals("total")&&!publiclyVisible(player,settings,br.com.shirokaidex.profiles.StatisticsPrivacy.Key.SHINY_CAPTURES))return PlaceholderValues.UNKNOWN;
        var values = placeholderCache.read(player.getUUID(), settings, System.nanoTime());
        return PlaceholderValues.format(metric, values.get(id), settings.language());
    }
    private int publicPlaceholder(CommandSourceStack source,String metric,String argument) {
        if(!ready){source.sendFailure(messages.text(language(),"not_ready"));return 0;}
        if(source.getPlayer()==null){source.sendFailure(messages.text(language(),"players_only"));return 0;}
        if(placeholderBridge==null){source.sendFailure(messages.text(language(),"placeholder_unavailable"));return 0;}
        if(PublicMetricValues.argument(metric,argument)==null){source.sendFailure(messages.text(language(),"placeholder_metric_invalid"));return 0;}
        String value=placeholderBridge.preview(source.getPlayer(),metric,argument);
        source.sendSuccess(()->messages.text(language(),"placeholder_preview",metric,argument,value),false);return 1;
    }
    private int placeholder(CommandSourceStack source, String id, String metric) {
        if (!ready) { source.sendFailure(messages.text(language(), "not_ready")); return 0; }
        if (source.getPlayer() == null) { source.sendFailure(messages.text(language(), "players_only")); return 0; }
        if (!canAccess(source.getPlayer(), id)) { source.sendFailure(messages.text(language(), "collection_denied")); return 0; }
        if (placeholderBridge == null) { source.sendFailure(messages.text(language(), "placeholder_unavailable")); return 0; }
        if (!PlaceholderValues.METRICS.contains(metric)&&!metric.equals("legacy")) { source.sendFailure(messages.text(language(), "placeholder_metric_invalid")); return 0; }
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
    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> captureHistoryCommands() {
        return literal("captures").requires(source -> Permissions.has(source.getPlayer(), "shirokaidex.admin.captures"))
                .then(argument("player_uuid", StringArgumentType.word())
                        .executes(context -> captureHistory(context.getSource(), StringArgumentType.getString(context, "player_uuid"), 0))
                        .then(argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100001))
                                .executes(context -> captureHistory(context.getSource(), StringArgumentType.getString(context, "player_uuid"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "page") - 1))));
    }
    private int captureHistory(CommandSourceStack source, String rawPlayer, int page) {
        var actor = source.getPlayer();
        if (!ready || stopping || !Permissions.has(actor, "shirokaidex.admin.captures")) return 0;
        final java.util.UUID target;
        try { target = java.util.UUID.fromString(rawPlayer); }
        catch (IllegalArgumentException invalid) { source.sendFailure(messages.text(language(), "capture_history_invalid")); return 0; }
        var server = source.getServer();
        database.captureHistory(target, page).whenComplete((history, failure) -> server.execute(() -> {
            if (!ready || stopping || activeServer != server || actor.hasDisconnected()
                    || server.getPlayerList().getPlayer(actor.getUUID()) != actor
                    || !Permissions.has(actor, "shirokaidex.admin.captures")) return;
            if (failure != null) { source.sendFailure(messages.text(language(), "capture_stats_unavailable")); return; }
            source.sendSuccess(() -> messages.text(language(), "capture_history_header", target, page + 1, history.hasMore()), false);
            if (history.entries().isEmpty()) source.sendSuccess(() -> messages.text(language(), "capture_history_empty"), false);
            for (var entry : history.entries()) source.sendSuccess(() -> messages.text(language(), "capture_history_entry",
                    entry.capturedAt(), entry.species(), entry.form(), entry.shiny(), entry.origin(), entry.source(), entry.dimension(), entry.eventKey()), false);
        }));
        return 1;
    }

    private void markPublicView(ServerPlayer player,boolean publicView){if(publicView)publicViews.put(player.getUUID(),player.containerMenu);else publicViews.remove(player.getUUID());}
    private void invalidatePublicViews(){privacyQueue.addAll(publicMasks.keySet());publicMasks.clear();var server=activeServer;if(server==null){publicViews.clear();return;}for(var entry:java.util.Map.copyOf(publicViews).entrySet()){var player=server.getPlayerList().getPlayer(entry.getKey());if(player!=null&&player.containerMenu==entry.getValue())player.closeContainer();}publicViews.clear();}
    private long beginView(ServerPlayer player){long ticket=++rewardViewSequence;rewardViewRequests.put(player.getUUID(),ticket);return ticket;}
    private java.util.List<br.com.shirokaidex.rankings.CaptureRanking> visibleRankings(ServerPlayer player,Settings settings){
        return settings.rankings().stream().filter(definition->Permissions.has(player,definition.permission())).filter(definition->definition.metric()!=br.com.shirokaidex.rankings.CaptureRanking.Metric.COLLECTION||settings.collections().stream().anyMatch(c->c.id().equals(definition.key())&&Permissions.has(player,c.permission()))).toList();
    }
    private int profile(CommandSourceStack source,String requested,String section,int page){
        if(ready&&config.current().home().focused()){source.sendFailure(messages.text(language(),"focused.feature_disabled"));return 0;}
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;var server=source.getServer();String target=requested==null?player.getUUID().toString():requested;var settings=config.current();var previous=player.containerMenu;long ticket=beginView(player);
        var versions=cobblemon.collectionVersions(settings.collections());var collections=visible(player,settings).stream().map(CollectionDefinition::id).collect(java.util.stream.Collectors.toUnmodifiableSet());var rankings=visibleRankings(player,settings);
        database.profile(target,player.getUUID(),settings,versions,collections,rankings,Instant.now()).whenComplete((result,failure)->server.execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;
            if(settings!=config.current()||!collections.equals(visible(player,settings).stream().map(CollectionDefinition::id).collect(java.util.stream.Collectors.toUnmodifiableSet()))||!rankings.equals(visibleRankings(player,settings))||result!=null&&result.privacyRevision()!=database.privacyRevision()){profile(source,target,section,page);return;}
            if(failure!=null||result.data().isEmpty()){source.sendFailure(messages.text(language(),"profile_missing"));return;}var data=result.data().get();String id=data.identity().player().toString();
            if(section==null){br.com.shirokaidex.menus.ProfileMenu.open(player,settings,messages,data,()->profile(source,id,"species",0),()->profile(source,id,"categories",0),()->profile(source,id,"records",0),()->{if(player.getUUID().equals(data.identity().player()))achievements(source,null,0);else profile(source,id,"medals",0);},()->home(source));markPublicView(player,!player.getUUID().equals(data.identity().player()));return;}
            if(section.equals("medals")){br.com.shirokaidex.menus.AchievementsMenu.open(player,settings,messages,data.medals(),page,false,ignored->{},next->profile(source,id,"medals",next),()->profile(source,id,null,0));markPublicView(player,!player.getUUID().equals(data.identity().player()));return;}
            var required=section.equals("species")?br.com.shirokaidex.profiles.StatisticsPrivacy.Key.SPECIES_CAPTURES:section.equals("categories")?br.com.shirokaidex.profiles.StatisticsPrivacy.Key.CATEGORY_CAPTURES:br.com.shirokaidex.profiles.StatisticsPrivacy.Key.PERSONAL_RECORDS;
            if(!data.visibleStatistics().contains(required)){source.sendFailure(messages.text(language(),"profile_private"));profile(source,id,null,0);return;}
            java.util.List<net.minecraft.network.chat.Component> rows;
            if(section.equals("records"))rows=data.records().stream().map(record->messages.text(settings.language(),"capture_record",record.period(),record.total(),record.bucket().isEmpty()?"N/D":record.bucket(),data.timezone().getId())).toList();
            else {var counts=section.equals("species")?data.species():data.categories();rows=counts.entrySet().stream().sorted(java.util.Comparator.<java.util.Map.Entry<String,Long>>comparingLong(java.util.Map.Entry::getValue).reversed().thenComparing(java.util.Map.Entry::getKey)).map(entry->(net.minecraft.network.chat.Component)net.minecraft.network.chat.Component.literal(entry.getKey()+": "+entry.getValue())).toList();}
            br.com.shirokaidex.menus.ProfileMenu.detail(player,settings,messages,messages.text(settings.language(),"privacy_stat."+required.id()).getString(),rows,page,next->profile(source,id,section,next),()->profile(source,id,null,0));markPublicView(player,!player.getUUID().equals(data.identity().player()));
        }));return 1;
    }
    private int settings(CommandSourceStack source){
        if(ready&&config.current().home().focused()){source.sendFailure(messages.text(language(),"focused.feature_disabled"));return 0;}
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;var server=source.getServer();var current=config.current();var previous=player.containerMenu;long ticket=beginView(player);
        database.personalSettings(player.getUUID()).whenComplete((value,failure)->server.execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;if(current!=config.current()){settings(source);return;}if(failure!=null){source.sendFailure(messages.text(language(),"capture_stats_unavailable"));return;}
            br.com.shirokaidex.menus.SettingsMenu.open(player,current,messages,value.privacy(),value.rankingHidden(),value.preferences().notifications(),(key,visible)->changePrivacy(source,key.id(),visible),hidden->{rankingPrivacy(source,hidden);settings(source);},enabled->changeNotifications(source,enabled),()->home(source));
        }));return 1;
    }
    private int changePrivacy(CommandSourceStack source,String statistic,boolean visible){
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;var policy=config.current().statisticsPrivacy();if(!policy.enabled()||!policy.allowPlayerOverride()){source.sendFailure(messages.text(language(),"settings_fixed"));return 0;}
        br.com.shirokaidex.profiles.StatisticsPrivacy.Key key;try{key=br.com.shirokaidex.profiles.StatisticsPrivacy.Key.parse(statistic);}catch(IllegalArgumentException invalid){source.sendFailure(messages.text(language(),"reward_admin_invalid"));return 0;}
        var server=source.getServer();var previous=player.containerMenu;long ticket=beginView(player);database.statisticsPrivacy(player.getUUID(),key,visible).whenComplete((unused,failure)->server.execute(()->{if(ready&&!stopping&&activeServer==server&&failure==null)invalidatePublicViews();if(!validRewardView(source,player,ticket,previous))return;if(failure!=null)source.sendFailure(messages.text(language(),"capture_stats_unavailable"));else{source.sendSuccess(()->messages.text(language(),"settings_saved"),false);settings(source);}}));return 1;
    }
    private int changeNotifications(CommandSourceStack source,boolean enabled){
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;var server=source.getServer();var previous=player.containerMenu;long ticket=beginView(player);database.notificationPreference(player.getUUID(),enabled).whenComplete((unused,failure)->server.execute(()->{if(!validRewardView(source,player,ticket,previous))return;if(failure!=null)source.sendFailure(messages.text(language(),"capture_stats_unavailable"));else{source.sendSuccess(()->messages.text(language(),"settings_saved"),false);settings(source);}}));return 1;
    }
    private int achievements(CommandSourceStack source,String selected,int page){
        if(ready&&config.current().home().focused()){source.sendFailure(messages.text(language(),"focused.feature_disabled"));return 0;}
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;var server=source.getServer();var current=config.current();var previous=player.containerMenu;long ticket=beginView(player);
        database.medals(player.getUUID()).whenComplete((medals,failure)->server.execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;if(current!=config.current()){achievements(source,selected,page);return;}if(failure!=null){source.sendFailure(messages.text(language(),"capture_stats_unavailable"));return;}
            var medal=selected==null?null:medals.stream().filter(m->m.definition().id().equals(selected)&&m.active()).findFirst().orElse(null);
            if(medal==null){br.com.shirokaidex.menus.AchievementsMenu.open(player,current,messages,medals,page,true,id->achievements(source,id,0),next->achievements(source,null,next),()->home(source));return;}
            br.com.shirokaidex.menus.AchievementsMenu.favorite(player,current,messages,medal,slot->{
                database.favoriteMedal(player.getUUID(),medal.definition().id(),slot).whenComplete((changed,error)->server.execute(()->{if(!ready||stopping||activeServer!=server||server.getPlayerList().getPlayer(player.getUUID())!=player)return;if(error!=null||!Boolean.TRUE.equals(changed))source.sendFailure(messages.text(language(),"reward_not_eligible"));achievements(source,null,page);}));
            },()->achievements(source,null,page));
        }));return 1;
    }
    private void observeProfiles(MinecraftServer server){
        var online=server.getPlayerList().getPlayers();if(online.isEmpty()||profileObserving.size()>=4)return;var player=online.get(Math.floorMod(profileCursor++,online.size()));var uuid=player.getUUID();long now=System.nanoTime();if(profileObserving.contains(uuid)||now-profileObservedAt.getOrDefault(uuid,Long.MIN_VALUE/2)<java.util.concurrent.TimeUnit.SECONDS.toNanos(5))return;
        var settings=config.current();java.util.List<br.com.shirokaidex.profiles.ProfileProgress.Proof> proofs;
        try{var values=cobblemon.collections(player,settings.collections());var versions=cobblemon.collectionVersions(settings.collections());var observed=Instant.now();proofs=values.entrySet().stream().map(entry->new br.com.shirokaidex.profiles.ProfileProgress.Proof(entry.getKey(),versions.get(entry.getKey()),entry.getValue().progress().registered().isPresent()?entry.getValue().progress().registered().getAsInt():null,entry.getValue().progress().total(),observed)).toList();}
        catch(RuntimeException invalid){profileObservedAt.put(uuid,now);LOG.error("Profile snapshot unavailable",invalid);return;}
        profileObservedAt.put(uuid,now);profileObserving.add(uuid);database.observeProfile(uuid,proofs,settings,Instant.now()).whenComplete((unlocked,failure)->server.execute(()->{
            if(activeServer!=server)return;profileObserving.remove(uuid);if(!ready||stopping||server.getPlayerList().getPlayer(uuid)!=player)return;if(failure!=null){LOG.error("Profile/medal persistence failed; retry scheduled",failure);return;}

        }));
    }

    private void observeNotifications(MinecraftServer server){
        requestNotifications(server,br.com.shirokaidex.notifications.NotificationOutbox.GLOBAL,null);
        var players=server.getPlayerList().getPlayers();if(players.isEmpty())return;
        var player=players.get(Math.floorMod(notificationCursor++,players.size()));requestNotifications(server,player.getUUID().toString(),player);
    }
    private void requestNotifications(MinecraftServer server,String target,ServerPlayer player){
        long now=System.nanoTime();if(notificationPending.size()>=4||notificationPending.contains(target)||now-notificationObserved.getOrDefault(target,0L)<2_000_000_000L)return;
        notificationObserved.put(target,now);notificationPending.add(target);
        database.claimNotifications(target,Instant.now()).whenComplete((notices,failure)->server.execute(()->{
            if(activeServer!=server)return;notificationPending.remove(target);if(!ready||stopping)return;
            if(failure!=null){LOG.error("Notification delivery claim failed; retry deferred",failure);return;}
            if(player!=null){if(server.getPlayerList().getPlayer(player.getUUID())==player)for(var notice:notices){if(notice.event()!=br.com.shirokaidex.notifications.NotificationPolicy.Event.REWARD_UNLOCKED&&notice.event()!=br.com.shirokaidex.notifications.NotificationPolicy.Event.REWARD_CLAIMED)continue;var rule=config.current().notifications().rules().get(notice.event());if(rule.enabled()&&!(rule.global()&&notice.broadcastEligible()))br.com.shirokaidex.notifications.NotificationDelivery.send(player,notice,rule,language(),messages);}return;}
            var global=notices.stream().filter(n->{var rule=config.current().notifications().rules().get(n.event());return false;}).toList();if(global.isEmpty())return;
            var online=java.util.List.copyOf(server.getPlayerList().getPlayers());database.notificationAudience(online.stream().map(ServerPlayer::getUUID).toList()).whenComplete((audience,error)->server.execute(()->{
                if(error!=null||!ready||stopping||activeServer!=server)return;
                for(var receiver:online)if(audience.contains(receiver.getUUID())&&server.getPlayerList().getPlayer(receiver.getUUID())==receiver)for(var notice:global){var rule=config.current().notifications().rules().get(notice.event());if(rule.enabled()&&rule.global())br.com.shirokaidex.notifications.NotificationDelivery.send(receiver,notice,rule,language(),messages);}
            }));
        }));
    }
    private boolean validPrize(ServerPlayer player,br.com.shirokaidex.competitions.CompetitionAwards.Award award,br.com.shirokaidex.rewards.RewardPlan plan){
        return ready&&!stopping&&!player.hasDisconnected()&&activeServer==player.server&&player.server.getPlayerList().getPlayer(player.getUUID())==player&&award.player().equals(player.getUUID())&&award.available(Instant.now())&&award.plan().definition().equals(plan.definition());
    }
    private int prizes(CommandSourceStack source,String id,int page){
        if(ready&&config.current().home().focused()){source.sendFailure(messages.text(language(),"focused.feature_disabled"));return 0;}
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;var server=source.getServer();var previous=player.containerMenu;long ticket=beginView(player);
        if(id==null){database.competitionAwardViews(player.getUUID(),page).whenComplete((awards,failure)->server.execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;
            if(failure!=null){source.sendFailure(messages.text(language(),"reward_error"));return;}
            br.com.shirokaidex.menus.PrizesMenu.open(player,config.current(),messages,awards,page,award->prizes(source,award,0),next->prizes(source,null,next),()->home(source));
        }));return 1;}
        database.competitionAward(id,player.getUUID()).whenComplete((found,failure)->server.execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;
            if(failure!=null||found.isEmpty()){source.sendFailure(messages.text(language(),"reward_not_eligible"));return;}var award=found.get();
            database.rewardForPlayer(player.getUUID(),id).whenComplete((operation,error)->server.execute(()->{
                if(!validRewardView(source,player,ticket,previous))return;
                var view=error==null?br.com.shirokaidex.menus.layout.RewardView.preview(award.available(Instant.now()),award.plan(),operation):br.com.shirokaidex.menus.layout.RewardView.unavailable();
                MainMenu.openReward(player,config.current(),messages,award.plan(),view,page,next->prizes(source,id,next),()->prizes(source,null,0),()->{
                    if(!validPrize(player,award,award.plan())){source.sendFailure(messages.text(language(),"reward_not_eligible"));return;}
                    MainMenu.confirmReward(player,config.current(),messages,award.plan(),messages.text(language(),"competition_prize_band",award.band()).getString(),()->claimPrize(source,award),()->prizes(source,id,0));
                },messages.text(language(),"competition_prize_requirement",award.expiresAt()==null?messages.text(language(),"competition_prize_permanent").getString():award.expiresAt()).getString(),messages.text(language(),"competition_prize_band",award.band()).getString());
            }));
        }));return 1;
    }
    private void claimPrize(CommandSourceStack source,br.com.shirokaidex.competitions.CompetitionAwards.Award award){
        var player=source.getPlayer();if(player==null||!validPrize(player,award,award.plan()))return;player.closeContainer();
        var plan=award.plan();if(!plan.pokemon().isEmpty()&&!br.com.shirokaidex.rewards.PokemonRewardAdapter.supported()){source.sendFailure(messages.text(language(),"pokemon_backend_unavailable"));return;}
        database.rewardForPlayer(player.getUUID(),award.id()).whenComplete((operation,failure)->source.getServer().execute(()->{
            if(failure!=null||!validPrize(player,award,plan)){source.sendFailure(messages.text(language(),"reward_not_eligible"));return;}
            if(operation.isPresent()&&operation.get().state()!=br.com.shirokaidex.rewards.RewardJournal.State.PENDING){source.sendFailure(messages.text(language(),"reward_result.blocked"));return;}
            try{
                var prepared=operation.isPresent()?plan.reuse(operation.get().plan()):plan.withPokemon(plan.pokemon().stream().map(request->br.com.shirokaidex.rewards.PokemonPayload.freeze(source.getServer(),player,request)).toList()).withCommands(plan.commands().stream().map(request->request.freeze(player.getGameProfile().getName(),player.getUUID())).toList());
                prepared.json();deliverPlan(source,player,prepared,null,"",current->validPrize(player,award,current));
            }catch(RuntimeException invalid){LOG.error("Competition prize preparation failed",invalid);source.sendFailure(messages.text(language(),"reward_error"));}
        }));
    }

    private int competitionHistoryPeriod(CommandSourceStack source,String from,String until){
        try{return competitionHistory(source,new br.com.shirokaidex.competitions.CompetitionStore.HistoryFilter("","",Instant.parse(from),Instant.parse(until)),0);}
        catch(RuntimeException invalid){source.sendFailure(messages.text(language(),"reward_admin_invalid"));return 0;}
    }
    private int competitionHistory(CommandSourceStack source,br.com.shirokaidex.competitions.CompetitionStore.HistoryFilter filter,int page){
        if(ready&&config.current().home().focused()){source.sendFailure(messages.text(language(),"focused.feature_disabled"));return 0;}
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;var server=source.getServer();var previous=player.containerMenu;long ticket=beginView(player);
        database.competitionHistory(filter,page).whenComplete((result,failure)->server.execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;
            if(failure!=null){source.sendFailure(messages.text(language(),"capture_stats_unavailable"));return;}
            var visible=result.editions().stream().filter(e->Permissions.has(player,e.definition().permission())).toList();
            br.com.shirokaidex.menus.CompetitionMenu.history(player,config.current(),messages,visible,page,result.hasMore(),id->competition(source,id,0,true),next->competitionHistory(source,filter,next),()->home(source),()->prizes(source,null,0));
        }));return 1;
    }
    private int competitions(CommandSourceStack source,String requested,int page,boolean history) {
        if(ready&&config.current().home().focused())return history?0:focusedEvents(source);
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;var server=source.getServer();var previous=player.containerMenu;long ticket=beginView(player);
        if(history){if(requested!=null&&requested.contains(":"))return competition(source,requested,page,true);return competitionHistory(source,new br.com.shirokaidex.competitions.CompetitionStore.HistoryFilter(requested,"",null,null),page);}
        database.competitionEditions(false).whenComplete((editions,failure)->server.execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;
            if(failure!=null){source.sendFailure(messages.text(language(),"capture_stats_unavailable"));return;}
            var visible=editions.stream().filter(e->Permissions.has(player,e.definition().permission())).filter(e->!history||e.state()!=br.com.shirokaidex.competitions.CompetitionStore.State.ACTIVE).toList();
            if(requested!=null&&requested.contains(":")){competition(source,requested,page,history);return;}
            if(requested!=null)visible=visible.stream().filter(e->e.definition().id().equals(requested)).toList();
            br.com.shirokaidex.menus.CompetitionMenu.selector(player,config.current(),messages,visible,page,history,id->competition(source,id,0,history),next->competitions(source,requested,next,history),()->open(source,true),()->prizes(source,null,0));
        }));return 1;
    }
    private int competition(CommandSourceStack source,String id,int page,boolean history) {
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;var server=source.getServer();var previous=player.containerMenu;long ticket=beginView(player);
        var requestedSettings=config.current();database.competitionView(id,player.getUUID(),page,requestedSettings.statisticsPrivacy()).whenComplete((found,failure)->server.execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;
            if(failure!=null||found.isEmpty()){source.sendFailure(messages.text(language(),"capture_stats_unavailable"));return;}
            var view=found.get();if(requestedSettings!=config.current()||view.privacyRevision()!=database.privacyRevision()){competition(source,id,page,history);return;}if(!Permissions.has(player,view.edition().definition().permission())){source.sendFailure(messages.text(language(),"collection_denied"));return;}
            if(config.current().home().focused()){br.com.shirokaidex.menus.FocusedMenu.eventBoard(player,config.current(),view,()->focusedEvents(source));markPublicView(player,true);return;}
            br.com.shirokaidex.menus.CompetitionMenu.edition(player,config.current(),messages,view,page,next->competition(source,id,next,history),()->competitionChange(source,id,false,false),()->competitionChange(source,id,true,false),()->competitions(source,null,0,history),()->prizes(source,null,0));markPublicView(player,true);
        }));return 1;
    }
    private void competitionChange(CommandSourceStack source,String id,boolean cancel,boolean confirmed) {
        var player=source.getPlayer();if(!ready||stopping||player==null)return;var server=source.getServer();
        database.competitionView(id,player.getUUID(),0,config.current().statisticsPrivacy()).whenComplete((found,failure)->server.execute(()->{
            if(!ready||stopping||activeServer!=server||server.getPlayerList().getPlayer(player.getUUID())!=player)return;
            if(failure!=null||found.isEmpty()||!Permissions.has(player,found.get().edition().definition().permission())){source.sendFailure(messages.text(language(),"collection_denied"));return;}
            if(cancel&&!confirmed){br.com.shirokaidex.menus.CompetitionMenu.confirm(player,config.current(),messages,messages.text(language(),"competition_cancel_warning").getString(),()->competitionChange(source,id,true,true),()->competition(source,id,0,false));return;}
            var mutation=cancel?database.cancelCompetition(id,player.getUUID(),true,Instant.now()):database.enrollCompetition(id,player.getUUID(),Instant.now());
            mutation.whenComplete((changed,error)->server.execute(()->{
                if(!ready||stopping||activeServer!=server||server.getPlayerList().getPlayer(player.getUUID())!=player)return;
                if(error!=null)source.sendFailure(messages.text(language(),"capture_stats_unavailable"));
                else {source.sendSuccess(()->messages.text(language(),"competition_changed",Boolean.TRUE.equals(changed)),false);competition(source,id,0,false);}
            }));
        }));
    }
    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> competitionAdminCommands() {
        return literal("competition").requires(source->Permissions.has(source.getPlayer(),"shirokaidex.admin.competitions"))
                .then(competitionAuditCommands("points")).then(competitionAuditCommands("audit"))
                .then(literal("inspect").then(argument("edition",StringArgumentType.string()).then(argument("event",StringArgumentType.string()).executes(context->competitionAudit(context.getSource(),StringArgumentType.getString(context,"edition"),"inspect",StringArgumentType.getString(context,"event"),0)))))
                .then(literal("adjust").then(argument("edition",StringArgumentType.string()).then(argument("player_uuid",StringArgumentType.word()).then(argument("delta",com.mojang.brigadier.arguments.LongArgumentType.longArg(-1000000000L,1000000000L)).then(argument("reason",StringArgumentType.greedyString()).executes(context->adjustCompetition(context.getSource(),StringArgumentType.getString(context,"edition"),StringArgumentType.getString(context,"player_uuid"),com.mojang.brigadier.arguments.LongArgumentType.getLong(context,"delta"),null,StringArgumentType.getString(context,"reason"))))))))
                .then(literal("revert").then(argument("adjustment",StringArgumentType.word()).then(argument("reason",StringArgumentType.greedyString()).executes(context->adjustCompetition(context.getSource(),null,null,0,StringArgumentType.getString(context,"adjustment"),StringArgumentType.getString(context,"reason"))))))
                .then(literal("review").then(argument("edition",StringArgumentType.string()).then(argument("event",StringArgumentType.string()).then(argument("state",StringArgumentType.word()).then(argument("reason",StringArgumentType.greedyString()).executes(context->reviewCompetition(context.getSource(),StringArgumentType.getString(context,"edition"),StringArgumentType.getString(context,"event"),StringArgumentType.getString(context,"state"),StringArgumentType.getString(context,"reason"))))))))
                .then(literal("finalize").then(argument("edition",StringArgumentType.string()).then(argument("reason",StringArgumentType.greedyString()).executes(context->reviewCompetition(context.getSource(),StringArgumentType.getString(context,"edition"),null,null,StringArgumentType.getString(context,"reason"))))));
    }
    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> competitionAuditCommands(String mode){
        return literal(mode).then(argument("edition",StringArgumentType.string()).executes(context->competitionAudit(context.getSource(),StringArgumentType.getString(context,"edition"),mode,null,0))
                .then(argument("page",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1,100001)).executes(context->competitionAudit(context.getSource(),StringArgumentType.getString(context,"edition"),mode,null,com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,"page")-1))));
    }
    private int competitionAudit(CommandSourceStack source,String edition,String mode,String event,int page){
        var actor=source.getPlayer();if(!ready||stopping||actor==null||!Permissions.has(actor,"shirokaidex.admin.competitions"))return 0;var server=source.getServer();
        database.competitionAdminRows(edition,mode,event,page).whenComplete((rows,failure)->server.execute(()->{
            if(!ready||stopping||activeServer!=server||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.competitions"))return;
            if(failure!=null)source.sendFailure(messages.text(language(),"capture_stats_unavailable"));
            else{if(rows.isEmpty())source.sendSuccess(()->messages.text(language(),"capture_history_empty"),false);for(String row:rows)source.sendSuccess(()->net.minecraft.network.chat.Component.literal(row),false);}
        }));return 1;
    }
    private int adjustCompetition(CommandSourceStack source,String edition,String playerId,long delta,String reversal,String reason){
        var actor=source.getPlayer();if(!ready||stopping||actor==null||!Permissions.has(actor,"shirokaidex.admin.competitions"))return 0;
        java.util.UUID target;
        try{target=playerId==null?null:java.util.UUID.fromString(playerId);if(reversal!=null)java.util.UUID.fromString(reversal);if(reversal==null&&delta==0)throw new IllegalArgumentException();}
        catch(IllegalArgumentException invalid){source.sendFailure(messages.text(language(),"reward_admin_invalid"));return 0;}
        if(reason.strip().length()<8||reason.length()>2048||reason.codePoints().anyMatch(Character::isISOControl)){source.sendFailure(messages.text(language(),"reward_admin_invalid"));return 0;}
        var server=source.getServer();
        br.com.shirokaidex.menus.CompetitionMenu.confirm(actor,config.current(),messages,(reversal==null?edition+" | "+target+" | "+delta:reversal)+" | "+reason,()->{
            if(!ready||stopping||activeServer!=server||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.competitions"))return;actor.closeContainer();
            var action=reversal==null?database.adjustCompetition(edition,target,delta,actor.getUUID().toString(),reason,Instant.now()):database.revertCompetitionAdjustment(reversal,actor.getUUID().toString(),reason,Instant.now());
            action.whenComplete((result,failure)->server.execute(()->{if(!ready||stopping||activeServer!=server||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.competitions"))return;if(failure!=null)source.sendFailure(messages.text(language(),"capture_stats_unavailable"));else source.sendSuccess(()->messages.text(language(),"competition_adjusted",result.orElse("BLOCKED")),false);}));
        },actor::closeContainer);return 1;
    }
    private int reviewCompetition(CommandSourceStack source,String edition,String event,String state,String reason) {
        var actor=source.getPlayer();if(!ready||stopping||actor==null||!Permissions.has(actor,"shirokaidex.admin.competitions"))return 0;
        if(reason.strip().length()<8||reason.length()>2048||reason.codePoints().anyMatch(Character::isISOControl)||(event!=null&&!java.util.Set.of("COUNTED","HELD","REJECTED").contains(state))){source.sendFailure(messages.text(language(),"reward_admin_invalid"));return 0;}
        var server=source.getServer();
        br.com.shirokaidex.menus.CompetitionMenu.confirm(actor,config.current(),messages,edition+" | "+(event==null?"FINALIZE":state)+" | "+reason,()->{
            if(!ready||stopping||activeServer!=server||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.competitions"))return;
            actor.closeContainer();var action=event==null?database.finalizeCompetition(edition,actor.getUUID().toString(),reason,Instant.now()):database.reviewCompetitionPoint(edition,event,state,actor.getUUID().toString(),reason,Instant.now());
            action.whenComplete((changed,failure)->server.execute(()->{if(!ready||stopping||activeServer!=server||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.competitions"))return;if(failure!=null)source.sendFailure(messages.text(language(),"capture_stats_unavailable"));else source.sendSuccess(()->messages.text(language(),"competition_changed",Boolean.TRUE.equals(changed)),false);}));
        },actor::closeContainer);return 1;
    }

    private int rankings(CommandSourceStack source,String id,int page) {
        if(ready&&config.current().home().focused())return focusedRankingRoot(source);
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;
        var settings=config.current();var previous=player.containerMenu;long ticket=beginView(player);
        if(id==null){
            var definitions=visibleRankings(player,settings);
            br.com.shirokaidex.menus.RankingsMenu.selector(player,settings,messages,definitions,page,value->rankings(source,value,0),next->rankings(source,null,next),()->open(source,true),()->competitions(source,null,0,false));return 1;
        }
        var definition=settings.rankings().stream().filter(value->value.id().equals(id)).findFirst().orElse(null);
        if(definition==null||!visibleRankings(player,settings).contains(definition)){source.sendFailure(messages.text(language(),"collection_denied"));return 0;}
        var server=source.getServer();var collection=settings.collections().stream().filter(c->c.id().equals(definition.key())).findFirst().orElse(null);var sensitivity=collection!=null&&collection.mode()==CollectionDefinition.Mode.SHINY_CAPTURED?br.com.shirokaidex.profiles.StatisticsPrivacy.Key.SHINY_CAPTURES:null;String version=definition.metric()==br.com.shirokaidex.rankings.CaptureRanking.Metric.COLLECTION?cobblemon.collectionVersions(settings.collections()).get(definition.key()):null;database.captureRankingView(definition,page,settings.statisticsPrivacy(),version,sensitivity).whenComplete((result,failure)->server.execute(()->{
            if(!validRewardView(source,player,ticket,previous))return;
            if(result!=null&&(result.privacyRevision()!=database.privacyRevision()||settings!=config.current())){rankings(source,id,page);return;}
            if(!visibleRankings(player,config.current()).contains(definition)){source.sendFailure(messages.text(language(),"collection_denied"));return;}
            if(failure!=null){source.sendFailure(messages.text(language(),"capture_stats_unavailable"));return;}
            br.com.shirokaidex.menus.RankingsMenu.ranking(player,config.current(),messages,definition,result.page(),page,next->rankings(source,id,next),()->rankings(source,null,0),target->profile(source,target.toString(),null,0));markPublicView(player,true);
        }));return 1;
    }
    private int personalRecords(CommandSourceStack source) {
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;var server=source.getServer();var zone=config.current().statisticsTimezone();
        database.captureRecords(player.getUUID(),zone).whenComplete((records,failure)->server.execute(()->{
            if(!ready||stopping||activeServer!=server||server.getPlayerList().getPlayer(player.getUUID())!=player)return;
            if(failure!=null){source.sendFailure(messages.text(language(),"capture_stats_unavailable"));return;}
            for(var record:records)source.sendSuccess(()->messages.text(language(),"capture_record",record.period(),record.total(),record.bucket().isEmpty()?"N/D":record.bucket(),zone.getId()),false);
        }));return 1;
    }
    private int rankingPrivacy(CommandSourceStack source,boolean hidden) {
        var player=source.getPlayer();if(!ready||stopping||player==null)return 0;var server=source.getServer();
        database.rankingPrivacy(player.getUUID(),hidden).whenComplete((unused,failure)->server.execute(()->{
            if(ready&&!stopping&&activeServer==server&&failure==null)invalidatePublicViews();
            if(!ready||stopping||activeServer!=server||server.getPlayerList().getPlayer(player.getUUID())!=player)return;
            if(failure!=null)source.sendFailure(messages.text(language(),"capture_stats_unavailable"));
            else{invalidatePublicViews();source.sendSuccess(()->messages.text(language(),"ranking_privacy",hidden),false);}
        }));return 1;
    }

    private int captureStats(CommandSourceStack source,String kind,String raw){
        var player=source.getPlayer();if(!ready||player==null)return 0;
        if(kind==null){var zone=config.current().statisticsTimezone();database.captureSummary(player.getUUID(),java.time.Instant.now(),zone).whenComplete((value,failure)->source.getServer().execute(()->{
            if(stopping||player.hasDisconnected())return;
            if(failure!=null){source.sendFailure(messages.text(language(),"capture_stats_unavailable"));return;}
            source.sendSuccess(()->messages.text(language(),"capture_stats_summary",value.total(),value.shiny(),value.legendary(),value.mythical(),value.distinctSpecies(),value.today(),value.week(),value.month(),zone.getId(),value.unverified()),false);
        }));return 1;}
        String key=kind.equals("SPECIES")?br.com.shirokaidex.spawning.SpawnCatalog.speciesId(raw):raw;
        if(key==null||!key.matches(kind.equals("SPECIES")?"[a-z0-9_.-]+:[a-z0-9/._-]+":"[a-z][a-z0-9_]{0,47}")){source.sendFailure(messages.text(language(),"capture_stats_unavailable"));return 0;}
        database.captureCount(player.getUUID(),kind,key).whenComplete((value,failure)->source.getServer().execute(()->{
            if(stopping||player.hasDisconnected())return;
            if(failure!=null)source.sendFailure(messages.text(language(),"capture_stats_unavailable"));
            else source.sendSuccess(()->messages.text(language(),"capture_stats_count",key,value),false);
        }));return 1;
    }
    private int previewRewardResume(CommandSourceStack source,String raw,String reason){
        var actor=source.getPlayer();if(!ready||actor==null||!Permissions.has(actor,"shirokaidex.admin.rewards"))return 0;
        java.util.UUID id;try{id=java.util.UUID.fromString(raw);}catch(IllegalArgumentException failure){source.sendFailure(messages.text(language(),"reward_admin_invalid"));return 0;}
        if(reason.strip().length()<8||reason.length()>2048||reason.codePoints().anyMatch(Character::isISOControl)){source.sendFailure(messages.text(language(),"reward_admin_invalid"));return 0;}
        database.rewardOperation(id).thenCombine(database.rewardResumable(id),(found,safe)->safe?found:java.util.Optional.<br.com.shirokaidex.rewards.RewardJournal.Operation>empty()).whenComplete((found,failure)->source.getServer().execute(()->{
            if(stopping||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.rewards"))return;
            if(failure!=null||found.isEmpty()){source.sendFailure(messages.text(language(),"reward_resume_blocked"));return;}
            var operation=found.get();var target=source.getServer().getPlayerList().getPlayer(operation.player());
            if(operation.reward().startsWith("cp_")){previewPrizeResume(source,operation,reason);return;}
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
    private void previewPrizeResume(CommandSourceStack source,br.com.shirokaidex.rewards.RewardJournal.Operation operation,String reason){
        var actor=source.getPlayer();var server=source.getServer();if(actor==null)return;
        database.competitionAward(operation.reward(),operation.player()).whenComplete((found,failure)->server.execute(()->{
            if(!ready||stopping||activeServer!=server||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.rewards"))return;
            var player=server.getPlayerList().getPlayer(operation.player());if(failure!=null||found.isEmpty()||player==null){source.sendFailure(messages.text(language(),"reward_resume_blocked"));return;}var award=found.get();
            try{
                var plan=award.plan().reuse(operation.plan());if(!validPrize(player,award,plan)){source.sendFailure(messages.text(language(),"reward_resume_blocked"));return;}
                MainMenu.confirmRecovery(actor,config.current(),messages,plan,operation.id()+" → "+player.getGameProfile().getName(),reason,()->{
                    if(!ready||stopping||activeServer!=server||actor.hasDisconnected()||!Permissions.has(actor,"shirokaidex.admin.rewards")||!validPrize(player,award,plan))return;actor.closeContainer();
                    deliverPlan(source,player,plan,operation.id(),reason,current->!actor.hasDisconnected()&&Permissions.has(actor,"shirokaidex.admin.rewards")&&validPrize(player,award,current));
                },actor::closeContainer);
            }catch(RuntimeException invalid){source.sendFailure(messages.text(language(),"reward_resume_blocked"));}
        }));
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
        if(ready&&config.current().home().focused())return selector(source,0);
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

    private int shinyCursor;
    private final java.util.Map<java.util.UUID,Long> shinyRequestedAt=new java.util.HashMap<>();
    private void refreshCapturedShiny(ServerPlayer player,boolean force){
        var server=player.server;var uuid=player.getUUID();long now=System.nanoTime();
        if(!force&&(cobblemon.hasCapturedShiny(uuid)||now-shinyRequestedAt.getOrDefault(uuid,Long.MIN_VALUE/2)<java.util.concurrent.TimeUnit.SECONDS.toNanos(5)))return;
        shinyRequestedAt.put(uuid,now);database.capturedShinyForms(uuid).whenComplete((forms,failure)->server.execute(()->{
            if(stopping||activeServer!=server||server.getPlayerList().getPlayer(uuid)!=player)return;
            if(failure!=null){LOG.error("Verified shiny capture snapshot unavailable for {}",uuid,failure);return;}
            cobblemon.publishCapturedShiny(uuid,forms);rewardObservedAt.remove(uuid);profileObservedAt.remove(uuid);if(placeholderBridge!=null)placeholderCache.invalidate(uuid);
        }));
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
        deliverPlan(source,player,plan,recovery,recoveryReason,permitted);
    }
    private void deliverPlan(CommandSourceStack source,ServerPlayer player,br.com.shirokaidex.rewards.RewardPlan plan,java.util.UUID recovery,String recoveryReason,java.util.function.Predicate<br.com.shirokaidex.rewards.RewardPlan> permitted){
        var actor=source.getPlayer();if(actor==null||!permitted.test(plan))return;
        if(!plan.currencies().isEmpty()&&(!FabricLoader.getInstance().isModLoaded("impactor")||!br.com.shirokaidex.rewards.ImpactorCurrencyPort.supported())){source.sendFailure(messages.text(language(),"currency_backend_unavailable"));return;}
        if(!plan.permissions().isEmpty()&&!FabricLoader.getInstance().isModLoaded("luckperms")){source.sendFailure(messages.text(language(),"permission_backend_unavailable"));return;}
        br.com.shirokaidex.rewards.PokemonRewardAdapter pokemonAdapter=plan.pokemon().isEmpty()?null:new br.com.shirokaidex.rewards.PokemonRewardAdapter(source.getServer(),player,permitted);
        if(pokemonAdapter!=null){pokemonDeliveries.add(pokemonAdapter);source.sendSuccess(()->messages.text(language(),"pokemon_processing"),false);}
        br.com.shirokaidex.rewards.RewardDelivery.Adapter adapter=pokemonAdapter==null?new br.com.shirokaidex.rewards.ItemRewardAdapter(source.getServer(),player,permitted):pokemonAdapter;
        if(!plan.currencies().isEmpty())adapter=new br.com.shirokaidex.rewards.CurrencyRewardAdapter(adapter,new br.com.shirokaidex.rewards.ImpactorCurrencyPort(source.getServer(),player,permitted));
        if(!plan.permissions().isEmpty())adapter=new br.com.shirokaidex.rewards.PermissionRewardAdapter(adapter,new br.com.shirokaidex.rewards.LuckPermsRewardPort(source.getServer(),player,()->permitted.test(plan)));
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
                    invalidatePublicViews();
                    cobblemon.clear(); placeholderCache.clear();publicMetricCache.clear(); rewardObservedAt.clear();profileObservedAt.clear();
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
