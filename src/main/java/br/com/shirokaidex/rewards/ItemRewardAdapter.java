package br.com.shirokaidex.rewards;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/** Main-inventory only; capacity is simulated on copies before any mutation. No drop fallback. */
public final class ItemRewardAdapter implements RewardDelivery.Adapter {
    private final MinecraftServer server;
    private final ServerPlayer player;
    private final Predicate<RewardPlan> valid;
    public ItemRewardAdapter(MinecraftServer server, ServerPlayer player, Predicate<RewardPlan> valid) {
        this.server=server;this.player=player;this.valid=valid;
    }
    @Override public CompletableFuture<Boolean> preflight(RewardPlan plan,int firstRemainingStep) {
        return onServer(()->available(plan)&&(firstRemainingStep>=plan.items().size()||PlayerSaveCoordinator.ready(server,player))&&simulate(plan.items().subList(firstRemainingStep,plan.items().size())).isPresent());
    }
    @Override public CompletableFuture<String> deliver(RewardPlan plan,int step) {
        return onServer(()->{
            if(!available(plan)||!PlayerSaveCoordinator.ready(server,player))throw new IllegalStateException("Player, configuration, permission or persistence changed");
            // Recheck after persisted intent, since inventory may have changed while SQLite was working.
            if(simulate(plan.items().subList(step,plan.items().size())).isEmpty())throw new IllegalStateException("Inventory capacity changed");
            var next=simulate(List.of(plan.items().get(step))).orElseThrow();
            for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,next.get(slot));
            player.getInventory().setChanged();player.containerMenu.broadcastFullState();
            // The final acknowledgement follows a vanilla player save. This is not a distributed transaction.
            String evidence="Inventory slots updated on server thread; step="+step+"; final_save_readback_verified="+(step+1==plan.items().size());
            if(step+1==plan.items().size()){
                List<ItemStack> expected=new ArrayList<>();for(int slot=0;slot<36;slot++)expected.add(player.getInventory().getItem(slot).copy());
                return PlayerSaveCoordinator.save(server,player).thenCompose(unused->onServer(()->{verifySavedInventory(expected);return evidence;}));
            }
            return CompletableFuture.completedFuture(evidence);
        }).thenCompose(future->future);
    }
    private void verifySavedInventory(List<ItemStack> expected) throws java.io.IOException {
        // Vanilla PlayerDataStorage.save logs and suppresses write exceptions. Returning from saveAll is insufficient.
        var path=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(player.getUUID()+".dat");
        var data=net.minecraft.nbt.NbtIo.readCompressed(path,net.minecraft.nbt.NbtAccounter.create(16L*1024*1024));
        var inventory=data.getList("Inventory",10);
        List<ItemStack> saved=new ArrayList<>();for(int i=0;i<36;i++)saved.add(ItemStack.EMPTY);
        for(int i=0;i<inventory.size();i++){
            var entry=inventory.getCompound(i);int slot=Byte.toUnsignedInt(entry.getByte("Slot"));
            if(slot<36)saved.set(slot,ItemStack.parseOptional(server.registryAccess(),entry));
        }
        for(int i=0;i<36;i++)if(!ItemStack.matches(saved.get(i),expected.get(i)))
            throw new java.io.IOException("Player inventory save readback differs; external result requires review");
    }
    private boolean available(RewardPlan plan) { return server.getPlayerList().getPlayer(player.getUUID())==player&&!player.hasDisconnected()&&valid.test(plan); }
    private Optional<List<ItemStack>> simulate(List<RewardPlan.Item> entries) {
        List<ItemStack> slots=new ArrayList<>();for(int i=0;i<36;i++)slots.add(player.getInventory().getItem(i).copy());
        for(var entry:entries){
            ItemStack prototype=ItemPayload.restore(server,entry);
            if(prototype.isEmpty())return Optional.empty();int remaining=entry.count();
            for(ItemStack existing:slots){
                if(!existing.isEmpty()&&ItemStack.isSameItemSameComponents(existing,prototype)){
                    int transfer=Math.min(remaining,Math.max(0,existing.getMaxStackSize()-existing.getCount()));existing.grow(transfer);remaining-=transfer;
                }
            }
            for(int i=0;i<slots.size()&&remaining>0;i++)if(slots.get(i).isEmpty()){
                ItemStack stack=prototype.copy();int transfer=Math.min(remaining,stack.getMaxStackSize());stack.setCount(transfer);slots.set(i,stack);remaining-=transfer;
            }
            if(remaining>0)return Optional.empty();
        }
        return Optional.of(slots);
    }
    private <T> CompletableFuture<T> onServer(Callable<T> action){
        var result=new CompletableFuture<T>();server.execute(()->{try{result.complete(action.call());}catch(Exception failure){result.completeExceptionally(failure);}});return result;
    }
}
