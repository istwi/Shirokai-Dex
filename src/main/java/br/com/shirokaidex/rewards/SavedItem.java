package br.com.shirokaidex.rewards;

import br.com.shirokaidex.pokedex.CollectionDefinition;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

/** Immutable serialized template, safe to pass to the database worker. No Minecraft object is retained. */
public record SavedItem(String id, String item, int count, String data, String checksum) {
    public SavedItem {
        if(id==null||!id.matches("[a-z][a-z0-9_]{0,47}"))throw new IllegalArgumentException("invalid saved item id");
        CollectionDefinition.identifier(item);
        if(count<1||count>2304||data==null||data.isBlank()||data.getBytes(StandardCharsets.UTF_8).length>16384)
            throw new IllegalArgumentException("invalid saved item count or payload (maximum 16 KiB)");
        if(!digest(item,count,data).equals(checksum))throw new IllegalArgumentException("saved item checksum mismatch");
    }
    public static SavedItem create(String id,String item,int count,String data){return new SavedItem(id,item,count,data,digest(item,count,data));}
    public RewardPlan.Item snapshot(Integer override){return new RewardPlan.Item(item,override==null?count:override,data);}
    private static String digest(String item,int count,String data){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((item+"\n"+count+"\n"+data).getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
