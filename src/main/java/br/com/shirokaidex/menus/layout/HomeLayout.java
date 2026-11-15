package br.com.shirokaidex.menus.layout;

import java.util.*;

/** Initial visual layout scope: hub only. Configurable placement and an optional player command for the close button. */
public record HomeLayout(int rows,Palette palette,Map<String,Integer> slots,String closeCommand) {
    public HomeLayout(int rows,Palette palette,Map<String,Integer> slots){this(rows,palette,slots,"");}
    public enum Palette {
        VIOLET(0xD7B8FF,0x7966F2),OCEAN(0x90F5E8,0x4883ED),EMBER(0xFFD195,0xEC6877);
        public final int start,end;Palette(int start,int end){this.start=start;this.end=end;}
    }
    public static final Map<String,Integer> DEFAULT_SLOTS=Map.of("profile",4,"dex",20,"rewards",22,"wiki",24,"competitions",29,"achievements",31,"history",33,"settings",40,"back",48,"close",50);
    public HomeLayout {
        Objects.requireNonNull(closeCommand);
        if(closeCommand.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("invalid home close command");
        closeCommand=closeCommand.strip();
        if(closeCommand.equals("/"))throw new IllegalArgumentException("empty home close command");
        if(closeCommand.startsWith("/"))closeCommand=closeCommand.substring(1);
        if(closeCommand.length()>256||closeCommand.chars().anyMatch(Character::isISOControl)||(!closeCommand.isEmpty()&&!closeCommand.matches("[a-z0-9_.:-]+(?: +.+)?")))throw new IllegalArgumentException("invalid home close command");
        Objects.requireNonNull(palette);slots=Map.copyOf(slots);
        if(rows<3||rows>6||!slots.keySet().equals(DEFAULT_SLOTS.keySet()))throw new IllegalArgumentException("home requires 3..6 rows and every known button");
        if(new HashSet<>(slots.values()).size()!=slots.size())throw new IllegalArgumentException("home slot conflict");
        for(int slot:slots.values())if(slot<0||slot>=rows*9)throw new IllegalArgumentException("home slot outside configured rows");
    }
    public static HomeLayout defaults(){return new HomeLayout(6,Palette.VIOLET,DEFAULT_SLOTS);}
}
