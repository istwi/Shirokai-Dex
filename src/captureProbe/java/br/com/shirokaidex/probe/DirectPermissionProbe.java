package br.com.shirokaidex.probe;
/** Read-only LuckPerms API fixture, isolated from servers without the optional provider. */
final class DirectPermissionProbe {
    static boolean granted(net.minecraft.server.level.ServerPlayer player){
        var user=net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(player.getUUID());
        return user!=null&&user.getNodes().stream().anyMatch(node->node.getKey().equals("shirokai.dex.national.005")&&node.getValue()&&!node.hasExpiry()&&node.getContexts().isEmpty());
    }
}
