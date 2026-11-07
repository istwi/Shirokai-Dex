package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.menus.layout.HomeLayout;
import br.com.shirokaidex.menus.layout.MenuNavigation;
import br.com.shirokaidex.menus.layout.MenuAppearance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
    public static Component title(String label,Settings settings){return title(label,settings.home().palette());}
    public static Component title(String label,HomeLayout.Palette palette){return Component.literal("✦ ").withStyle(style->style.withColor(palette.start).withBold(true)).append(Component.literal(label.startsWith("✦ ")?label.substring(2):label).withStyle(style->style.withColor(palette.end).withBold(true)));}
    public static String titleLabel(MenuNavigation.Layout layout,String original,String player){return layout.title().isEmpty()?original:MenuAppearance.render(layout.title(),"title",original,player);}
    public static SimpleContainer appearance(SimpleContainer original,MenuNavigation.Layout layout,String player){
        var contents=new SimpleContainer(layout.rows()*9);
        for(int slot=0;slot<original.getContainerSize();slot++)contents.setItem(slot,original.getItem(slot));
        layout.buttons().forEach((role,view)->{
            int slot=layout.screen().defaults.get(role);var old=contents.getItem(slot);if(old.isEmpty())return;
            var label=old.getHoverName();String real=label.getString();if(real.startsWith("✦ "))real=real.substring(2);
            var item=view.icon().isEmpty()?old:new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(view.icon())));
            item.set(DataComponents.CUSTOM_NAME,label);
            var lore=new ArrayList<>(old.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines());
            for(String line:view.lore())lore.add(text(Component.literal(MenuAppearance.render(line,"name",real,player))));
            item.set(DataComponents.LORE,new ItemLore(lore));
            if(!view.name().isEmpty())item.set(DataComponents.CUSTOM_NAME,name(Component.literal(MenuAppearance.render(view.name(),"name",real,player)),label.getStyle().getColor()==null?TEXT:label.getStyle().getColor().getValue()));
            item.set(DataComponents.ATTRIBUTE_MODIFIERS,item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).withTooltip(false));contents.setItem(slot,item);
        });return contents;
    }
    public static Component text(Component line){return line.copy().withStyle(style->style.withColor(TEXT).withItalic(false));}
    public static void card(ItemStack item,Component label,List<Component> lore,int color){
        item.set(DataComponents.CUSTOM_NAME,name(label,color));item.set(DataComponents.LORE,new ItemLore(lore.stream().map(MenuStyle::text).toList()));
        item.set(DataComponents.ATTRIBUTE_MODIFIERS,item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).withTooltip(false));
    }
    public static void navigation(SimpleContainer contents,Map<Integer,Runnable> actions,br.com.shirokaidex.menus.layout.MenuNavigation.Layout layout){
        Map<Integer,ItemStack> icons=new HashMap<>();Map<Integer,Runnable> callbacks=new HashMap<>();
        var moves=layout.moves();
        for(int source:moves.keySet()){
            icons.put(source,contents.getItem(source));var action=actions.remove(source);if(action!=null)callbacks.put(source,action);
            contents.setItem(source,ItemStack.EMPTY);
        }
        moves.forEach((source,destination)->{
            if(!contents.getItem(destination).isEmpty())throw new IllegalStateException("navigation overlaps content");
            contents.setItem(destination,icons.get(source));if(callbacks.containsKey(source))actions.put(destination,callbacks.get(source));
        });
    }
    public static void footer(SimpleContainer contents,Settings settings){footer(contents,settings.home().palette());}
    public static void footer(SimpleContainer contents,HomeLayout.Palette palette){
        List<Integer> slots=new ArrayList<>(List.of(21,23,25));for(int slot=28;slot<contents.getContainerSize();slot++)slots.add(slot);
        for(int slot:slots)if(contents.getItem(slot).isEmpty()){
            var pane=new ItemStack(switch(palette){case VIOLET->Items.PURPLE_STAINED_GLASS_PANE;case OCEAN->Items.CYAN_STAINED_GLASS_PANE;case EMBER->Items.ORANGE_STAINED_GLASS_PANE;});pane.set(DataComponents.CUSTOM_NAME,Component.literal(" "));contents.setItem(slot,pane);
        }
    }
    public static void polish(ItemStack item,int color){
        if(item.isEmpty())return;var label=item.get(DataComponents.CUSTOM_NAME);if(label==null||label.getString().isBlank()||label.getString().startsWith("✦ "))return;
        card(item,label,item.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines(),color);
    }
}
