package br.com.shirokaidex.rewards;
/** Selectable Dex tag permission, never group membership or an arbitrary console command. */
public record PermissionReward(String permission) {
    public PermissionReward {if(permission==null||!permission.matches("shirokai\\.dex\\.[a-z0-9_.-]{1,112}"))throw new IllegalArgumentException("Invalid Dex tag permission");}
}
