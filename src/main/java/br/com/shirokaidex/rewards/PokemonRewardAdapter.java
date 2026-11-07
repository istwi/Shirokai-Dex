package br.com.shirokaidex.rewards;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/** Native NBT backend only. Cobblemon performs its own writes; this adapter only verifies readback. */
public final class PokemonRewardAdapter implements RewardDelivery.Adapter,AutoCloseable {
    private static final ScheduledExecutorService IO=Executors.newScheduledThreadPool(2,r->{var t=new Thread(r,"shirokaidex-pokemon-readback");t.setDaemon(true);return t;});
    private final MinecraftServer server;private final ServerPlayer player;private final Predicate<RewardPlan> valid;
    private final ItemRewardAdapter items;private final Set<CompletableFuture<String>> pending=ConcurrentHashMap.newKeySet();private volatile boolean closed;private final List<Path> files;private final int saveInterval;
    public PokemonRewardAdapter(MinecraftServer server,ServerPlayer player,Predicate<RewardPlan> valid){this.server=server;this.player=player;this.valid=valid;items=new ItemRewardAdapter(server,player,valid);
        var root=server.getWorldPath(LevelResource.ROOT).resolve("pokemon");String uuid=player.getUUID().toString();
        files=List.of(root.resolve("playerpartystore").resolve(uuid.substring(0,2)).resolve(uuid+".dat"),root.resolve("pcstore").resolve(uuid.substring(0,2)).resolve(uuid+".dat"));
        saveInterval=Cobblemon.INSTANCE.getConfig().getPokemonSaveIntervalSeconds();}
    public static boolean supported(){int interval=Cobblemon.INSTANCE.getConfig().getPokemonSaveIntervalSeconds();return Cobblemon.INSTANCE.getConfig().getStorageFormat().equals("nbt")&&interval>=1&&interval<=60;}
    @Override public CompletableFuture<Boolean> preflight(RewardPlan plan,int from){
        return onServer(()->{
            if(closed||!supported()||player.hasDisconnected()||server.getPlayerList().getPlayer(player.getUUID())!=player||!valid.test(plan))return false;
            var party=Cobblemon.INSTANCE.getStorage().getParty(player);var pc=Cobblemon.INSTANCE.getStorage().getPC(player);
            if(party.getClass()!=PlayerPartyStore.class||pc.getClass()!=PCStore.class||!party.getUuid().equals(player.getUUID())||!pc.getUuid().equals(player.getUUID()))return false;
            if(party.size()<1||party.size()>6||pc.getBoxes().isEmpty()||pc.getBoxes().size()>4096)return false;
            int remaining=plan.pokemon().size()-Math.max(0,from-plan.items().size());int free=party.size()-party.occupied()+pc.getBoxes().stream().mapToInt(box->box.getUnoccupiedSlots()).sum();
            return free>=remaining;
        }).thenCompose(space->!space?CompletableFuture.completedFuture(false):from<plan.items().size()?items.preflight(plan,from):CompletableFuture.completedFuture(true));
    }
    @Override public CompletableFuture<String> deliver(RewardPlan plan,int step){
        if(step<plan.items().size())return items.deliver(plan,step);
        var spec=plan.pokemon().get(step-plan.items().size());
        return preflight(plan,step).thenCompose(allowed->{
            if(!allowed)return CompletableFuture.failedFuture(new IllegalStateException("Pokemon storage preconditions changed"));
            return onServer(()->{
                if(closed||!valid.test(plan)||player.hasDisconnected())throw new IllegalStateException("Player or permission changed");
                var pokemon=PokemonPayload.restore(server,spec);var party=Cobblemon.INSTANCE.getStorage().getParty(player);var pc=Cobblemon.INSTANCE.getStorage().getPC(player);
                if(party.get(pokemon.getUuid())!=null||pc.get(pokemon.getUuid())!=null)throw new IllegalStateException("Prepared UUID already present; review required");
                if(!party.add(pokemon))throw new IllegalStateException("Cobblemon rejected addition; result must be reviewed");
                if(party.get(pokemon.getUuid())==null&&pc.get(pokemon.getUuid())==null)throw new IllegalStateException("Added Pokemon not observable in destination stores");
                return pokemon.getUuid();
            }).thenCompose(uuid->{
                var result=new CompletableFuture<String>();pending.add(result);result.whenComplete((v,e)->pending.remove(result));
                if(closed)result.completeExceptionally(new IllegalStateException("Server stopping"));
                else poll(result,spec,uuid,System.nanoTime()+TimeUnit.SECONDS.toNanos(Math.min(180,3L*saveInterval+15)));
                return result;
            });
        });
    }
    private void poll(CompletableFuture<String> result,PokemonReward spec,UUID uuid,long deadline){
        IO.schedule(()->{
            if(result.isDone()||closed)return;
            String stored=null;
            try{
                for(int fileIndex=0;fileIndex<files.size();fileIndex++){
                    var path=files.get(fileIndex);
                    if(!Files.isRegularFile(path))continue;
                    var nbt=NbtIo.readCompressed(path,NbtAccounter.create(32L*1024*1024));
                    if(fileIndex==0)stored=find(nbt,uuid);
                    else{int boxes=nbt.getInt("BoxCount");if(boxes<0||boxes>4096)throw new IllegalStateException("unsupported PC size");for(int i=0;i<boxes&&stored==null;i++)stored=find(nbt.getCompound("Box"+i),uuid);}
                    if(stored!=null)break;
                }
            }catch(Exception reading){/* Native writer is asynchronous; retry a partial file until the bounded deadline. */}
            if(stored==null){
                if(System.nanoTime()>=deadline)result.completeExceptionally(new IllegalStateException("Pokemon save could not be verified; no automatic retry"));
                else poll(result,spec,uuid,deadline);
            }else{
                String snapshot=stored;
                onServer(()->{
                    if(result.isDone()||closed)return "";
                    var persisted=PokemonPayload.restore(server,spec.withSnapshot(snapshot));var prepared=PokemonPayload.restore(server,spec);
                    if(!persisted.getUuid().equals(uuid)||!PokemonPayload.signature(prepared).equals(PokemonPayload.signature(persisted)))throw new IllegalStateException("Persisted Pokemon differs from prepared payload");
                    return "Pokemon UUID="+uuid+"; native NBT readback verified; obtained event only, not a capture";
                }).whenComplete((evidence,error)->{if(error!=null)result.completeExceptionally(error);else if(!evidence.isEmpty())result.complete(evidence);});
            }
        },500,TimeUnit.MILLISECONDS);
    }
    private static String find(CompoundTag root,UUID uuid){for(int i=0;i<30;i++){var child=root.getCompound("Slot"+i);if(child.hasUUID("UUID")&&child.getUUID("UUID").equals(uuid))return child.toString();if(child.contains("UUID",8)){try{if(UUID.fromString(child.getString("UUID")).equals(uuid))return child.toString();}catch(IllegalArgumentException invalid){/* not a valid UUID */}}}return null;}
    private <T> CompletableFuture<T> onServer(Callable<T> action){var result=new CompletableFuture<T>();server.execute(()->{try{result.complete(action.call());}catch(Exception error){result.completeExceptionally(error);}});return result;}
    @Override public void close(){closed=true;pending.forEach(result->result.completeExceptionally(new IllegalStateException("Pokemon verification interrupted by shutdown")));}
}
