package br.com.shirokaidex.permissions;

import java.util.UUID;
import net.luckperms.api.LuckPermsProvider;

final class LuckPermsAccess {
    private LuckPermsAccess() { }
    static boolean check(UUID player, String permission) {
        var user = LuckPermsProvider.get().getUserManager().getUser(player);
        return user != null && user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }
}
