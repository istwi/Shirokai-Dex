package br.com.shirokaidex.permissions;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

public final class Permissions {
    private Permissions() { }
    public static boolean mayReload(ServerPlayer player) {
        return has(player, "shirokaidex.admin.reload");
    }
    public static boolean has(ServerPlayer player, String permission) {
        if (permission.isEmpty()) return player != null;
        if (player == null || !FabricLoader.getInstance().isModLoaded("luckperms")) return false;
        // The optional API is referenced only in a class loaded when its provider is present.
        try { return LuckPermsAccess.check(player.getUUID(), permission); }
        catch (IllegalStateException | LinkageError unavailable) { return false; }
    }
}
