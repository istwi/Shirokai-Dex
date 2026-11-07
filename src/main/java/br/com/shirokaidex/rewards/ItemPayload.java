package br.com.shirokaidex.rewards;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/** Registry-aware serialization is only called on the game thread. */
public final class ItemPayload {
    private ItemPayload() { }
    public static SavedItem capture(MinecraftServer server,String id,ItemStack held){
        checkThread(server);if(held.isEmpty())throw new IllegalArgumentException("cannot save an empty item");
        var prototype=held.copy();prototype.setCount(1);
        String item=BuiltInRegistries.ITEM.getKey(prototype.getItem()).toString();
        var saved=SavedItem.create(id,item,held.getCount(),prototype.save(server.registryAccess()).toString());
        if(!ItemStack.matches(prototype,restore(server,saved.snapshot(1))))throw new IllegalArgumentException("item components did not round trip");
        return saved;
    }
    public static ItemStack restore(MinecraftServer server,RewardPlan.Item entry){
        checkThread(server);
        if(entry.data().isEmpty())return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.id())));
        try{
            var tag=TagParser.parseTag(entry.data());
            ItemStack stack=ItemStack.STRICT_SINGLE_ITEM_CODEC.parse(RegistryOps.create(NbtOps.INSTANCE,server.registryAccess()),tag).getOrThrow();
            if(stack.isEmpty()||stack.getCount()!=1||!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(entry.id()))
                throw new IllegalArgumentException("payload does not match its declared item");
            return stack;
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new IllegalArgumentException("invalid item payload",failure);}
    }
    private static void checkThread(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Item codec requires server thread");}
}
