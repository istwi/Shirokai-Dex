package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.pokedex.DexProgress;
import br.com.shirokaidex.rewards.RewardPlan;
import br.com.shirokaidex.menus.layout.HomeLayout;
import br.com.shirokaidex.wiki.Wiki;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import java.util.*;
import java.util.function.*;

/** First layout pass: a clear hub, real summaries and explicitly unavailable future systems. */
public final class HomeMenu {
    private HomeMenu() { }
    public static void open(ServerPlayer player,Settings settings,Messages messages,DexProgress progress,int collections,int rewards,Map<String,Runnable> available){
        var layout=settings.home();var contents=new SimpleContainer(layout.rows()*9);Map<Integer,Runnable> actions=new HashMap<>();
        for(int slot=0;slot<contents.getContainerSize();slot++){
            boolean border=slot<9||slot>=contents.getContainerSize()-9;
            Item glass=border?switch(layout.palette()){case VIOLET->Items.PURPLE_STAINED_GLASS_PANE;case OCEAN->Items.CYAN_STAINED_GLASS_PANE;case EMBER->Items.ORANGE_STAINED_GLASS_PANE;}:Items.BLACK_STAINED_GLASS_PANE;
            var pane=new ItemStack(glass);pane.set(DataComponents.CUSTOM_NAME,Component.literal(" "));contents.setItem(slot,pane);
        }
        var profile=new ItemStack(Items.PLAYER_HEAD);profile.set(DataComponents.CUSTOM_NAME,name(messages.text(settings.language(),"home.profile",player.getGameProfile().getName()),layout.palette().start));
        profile.set(DataComponents.LORE,new ItemLore(List.of(messages.text(settings.language(),"home.profile_progress",progress.registered().isPresent()?progress.registered().getAsInt():"N/D",progress.total()),messages.text(settings.language(),"home.profile_percent",progress.formattedPercentage(settings.language()).orElse("N/D")))));contents.setItem(layout.slots().get("profile"),profile);
        Map<String,Item> icons=Map.of("dex",Items.BOOK,"rewards",Items.CHEST,"wiki",Items.SPYGLASS,"competitions",Items.GOLDEN_SWORD,"achievements",Items.NETHER_STAR,"history",Items.WRITABLE_BOOK,"settings",Items.COMPARATOR,"back",Items.ARROW,"close",Items.BARRIER);
        for(String id:icons.keySet()){
            int slot=layout.slots().get(id);boolean enabled=available.containsKey(id);var item=new ItemStack(icons.get(id));
            item.set(DataComponents.CUSTOM_NAME,name(messages.text(settings.language(),"home."+id),enabled?(id.equals("back")?0xF1CD76:id.equals("close")?0xF07886:layout.palette().start):0x999999));
            List<Component> lore=new ArrayList<>();
            if(Set.of("competitions","achievements","history","settings").contains(id))lore.add(messages.text(settings.language(),"home.in_development"));
            else if(id.equals("dex"))lore.add(messages.text(settings.language(),"home.collections",collections));
            else if(id.equals("rewards"))lore.add(messages.text(settings.language(),"home.reward_count",rewards));
            else if(id.equals("back"))lore.add(messages.text(settings.language(),enabled?"home.external_back":"home.external_missing"));
            else if(id.equals("wiki"))lore.add(messages.text(settings.language(),enabled?"home.wiki_description":"home.wiki_locked"));
            if(enabled)lore.add(messages.text(settings.language(),id.equals("close")?"home.click_close":id.equals("back")?"home.click_back":"home.click_open"));
            item.set(DataComponents.LORE,new ItemLore(lore.stream().map(line->(Component)line.copy().withStyle(style->style.withColor(0xAAAAAA).withItalic(false))).toList()));contents.setItem(slot,item);if(enabled)actions.put(slot,available.get(id));
        }
        MainMenu.show(player,gradient(settings.menuTitle(),layout.palette()),contents,actions);
    }
    public static void rewards(ServerPlayer player,Settings settings,Messages messages,List<RewardPlan> plans,Set<String> unlocked,Map<String,br.com.shirokaidex.rewards.RewardJournal.State> states,boolean unavailable,int requested,Consumer<String> navigate,IntConsumer page,Runnable home){
        var contents=new SimpleContainer(27);Map<Integer,Runnable> actions=new HashMap<>();var slice=Wiki.paginate(plans,requested,18);
        for(int i=0;i<slice.items().size();i++){
            var plan=slice.items().get(i);var item=new ItemStack(!plan.pokemon().isEmpty()?Items.DRAGON_EGG:!plan.currencies().isEmpty()?Items.GOLD_INGOT:Items.CHEST);
            var view=unavailable?br.com.shirokaidex.menus.layout.RewardView.unavailable():br.com.shirokaidex.menus.layout.RewardView.state(unlocked.contains(plan.id()),states.get(plan.id()));
            int color=switch(view.status()){case COMPLETED,AVAILABLE->MenuStyle.SUCCESS;case PROCESSING,RESERVED->MenuStyle.NAVIGATION;case NEEDS_REVIEW,PLAN_CHANGED,UNAVAILABLE->MenuStyle.DANGER;case LOCKED,CANCELLED->0x999999;};
            MenuStyle.card(item,Component.literal(plan.id()),List.of(messages.text(settings.language(),"layout.reward_status."+view.status().name()),messages.text(settings.language(),"home.reward_actions",plan.steps()),messages.text(settings.language(),"home.click_preview")),color);contents.setItem(i,item);actions.put(i,()->navigate.accept(plan.id()));
        }
        if(plans.isEmpty()){var empty=new ItemStack(Items.PAPER);empty.set(DataComponents.CUSTOM_NAME,messages.text(settings.language(),"home.no_rewards"));contents.setItem(13,empty);}
        button(contents,actions,23,Items.ARROW,messages.text(settings.language(),"home.return_home"),home);
        button(contents,actions,22,Items.BARRIER,messages.text(settings.language(),"home.close"),player::closeContainer);
        if(slice.number()>0)button(contents,actions,18,Items.ARROW,messages.text(settings.language(),"page_previous"),()->page.accept(slice.number()-1));
        if(slice.number()+1<slice.pages())button(contents,actions,26,Items.ARROW,messages.text(settings.language(),"page_next"),()->page.accept(slice.number()+1));
        var layout=settings.menus().layout(br.com.shirokaidex.menus.layout.MenuNavigation.Screen.REWARDS);
        contents=MenuStyle.appearance(contents,layout,player.getGameProfile().getName());MenuStyle.navigation(contents,actions,layout);var palette=layout.palette().orElse(settings.home().palette());if(layout.footer())MenuStyle.footer(contents,palette);
        MainMenu.show(player,gradient(MenuStyle.titleLabel(layout,messages.text(settings.language(),"home.rewards").getString()+" ["+(slice.number()+1)+"/"+slice.pages()+"]",player.getGameProfile().getName()),palette),contents,actions);
    }
    private static void button(SimpleContainer contents,Map<Integer,Runnable> actions,int slot,Item icon,Component label,Runnable action){var item=new ItemStack(icon);item.set(DataComponents.CUSTOM_NAME,name(label,0xF1CD76));contents.setItem(slot,item);actions.put(slot,action);}
    private static Component name(Component label,int rgb){return Component.literal("✦ ").append(label).withStyle(style->style.withColor(rgb).withBold(true).withItalic(false));}
    public static Component gradient(String text,HomeLayout.Palette palette){
        String title=text.startsWith("✦")?text:"✦ "+text;int[] chars=title.codePoints().toArray();var result=Component.empty();
        for(int i=0;i<chars.length;i++){double t=chars.length<2?0:(double)i/(chars.length-1);int rgb=0;for(int shift:new int[]{16,8,0})rgb|=((int)Math.round(((palette.start>>shift)&255)*(1-t)+((palette.end>>shift)&255)*t))<<shift;
            int color=rgb;result.append(Component.literal(new String(Character.toChars(chars[i]))).withStyle(style->style.withColor(color).withBold(true).withItalic(false)));}
        return result;
    }
}
