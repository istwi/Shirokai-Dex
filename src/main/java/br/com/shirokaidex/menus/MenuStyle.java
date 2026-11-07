package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.menus.layout.HomeLayout;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import java.util.*;

/** Shared presentation only. Never apply this to a delivered stack or an immutable reward payload. */
public final class MenuStyle {
    public static final int TEXT=0xC7CDD9,NAVIGATION=0xF1CD76,DANGER=0xF07886,SUCCESS=0x80D9AE;
    private MenuStyle() { }
    public static Component name(Component label,int color){return Component.literal("✦ ").append(label).withStyle(style->style.withColor(color).withBold(true).withItalic(false));}
    public static Component title(String label,Settings settings){return Component.literal("✦ ").withStyle(style->style.withColor(settings.home().palette().start).withBold(true)).append(Component.literal(label.startsWith("✦ ")?label.substring(2):label).withStyle(style->style.withColor(settings.home().palette().end).withBold(true)));}
    public static Component text(Component line){return line.copy().withStyle(style->style.withColor(TEXT).withItalic(false));}
    public static void card(ItemStack item,Component label,List<Component> lore,int color){
        item.set(DataComponents.CUSTOM_NAME,name(label,color));item.set(DataComponents.LORE,new ItemLore(lore.stream().map(MenuStyle::text).toList()));
        item.set(DataComponents.ATTRIBUTE_MODIFIERS,item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).withTooltip(false));
    }
    public static void footer(SimpleContainer contents,Settings settings){
        List<Integer> slots=new ArrayList<>(List.of(21,23,25));for(int slot=28;slot<contents.getContainerSize();slot++)slots.add(slot);
        for(int slot:slots)if(contents.getItem(slot).isEmpty()){
            var pane=new ItemStack(switch(settings.home().palette()){case VIOLET->Items.PURPLE_STAINED_GLASS_PANE;case OCEAN->Items.CYAN_STAINED_GLASS_PANE;case EMBER->Items.ORANGE_STAINED_GLASS_PANE;});pane.set(DataComponents.CUSTOM_NAME,Component.literal(" "));contents.setItem(slot,pane);
        }
    }
    public static void polish(ItemStack item,int color){
        if(item.isEmpty())return;var label=item.get(DataComponents.CUSTOM_NAME);if(label==null||label.getString().isBlank()||label.getString().startsWith("✦ "))return;
        card(item,label,item.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines(),color);
    }
}
