package br.com.shirokaidex.probe;
/** Read-only LuckPerms API fixture, isolated from servers without the optional provider. */
final class DirectPermissionProbe {
    static boolean granted(net.minecraft.server.level.ServerPlayer player){
        return granted(player,"shirokai.dex.national.005");
    }
    static boolean granted(net.minecraft.server.level.ServerPlayer player,String permission){
        var user=net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(player.getUUID());
        return user!=null&&user.getNodes().stream().anyMatch(node->node.getKey().equals(permission)&&node.getValue()&&!node.hasExpiry()&&node.getContexts().isEmpty());
    }
}
