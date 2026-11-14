package br.com.shirokaidex.menus;

import br.com.shirokaidex.achievements.*;
import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.wiki.Wiki;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

public final class AchievementsMenu {
    private AchievementsMenu() { }
    public static void open(ServerPlayer viewer,Settings settings,Messages messages,List<AchievementStore.Medal> medals,int requested,boolean own,Consumer<String> select,IntConsumer page,Runnable back){
        var all=new LinkedHashMap<String,AchievementStore.Medal>();
        if(own)for(var definition:settings.achievements())all.put(definition.id(),new AchievementStore.Medal(definition,false,null,null));for(var medal:medals){
            if(!own&&(!medal.active()||!medal.definition().publicMedal()))continue;var current=all.get(medal.definition().id());if(current==null&&!medal.active())continue;
            all.put(medal.definition().id(),current==null?medal:new AchievementStore.Medal(current.definition(),medal.active(),medal.unlockedAt(),medal.favoriteSlot()));
        }
        var slice=Wiki.paginate(new ArrayList<>(all.values()),requested,18);var c=new SimpleContainer(27);var actions=new HashMap<Integer,Runnable>();
        for(int i=0;i<slice.items().size();i++){
            var medal=slice.items().get(i);var definition=medal.definition();boolean secret=definition.secret()&&!medal.active();Item icon=medal.active()?BuiltInRegistries.ITEM.get(ResourceLocation.parse(definition.icon())):Items.GRAY_DYE;if(icon==Items.AIR)icon=Items.PAPER;
            var lore=new ArrayList<Component>();if(!secret)lore.add(Component.literal(definition.description()));if(!medal.active()&&!definition.hint().isBlank())lore.add(Component.literal(definition.hint()));lore.add(messages.text(settings.language(),medal.active()?"achievement_unlocked":"achievement_locked"));if(medal.favoriteSlot()!=null)lore.add(messages.text(settings.language(),"achievement_favorite",medal.favoriteSlot()+1));
            ProfileMenu.card(c,i,icon,secret?messages.text(settings.language(),"achievement_secret"):Component.literal(definition.name()),lore);if(own&&medal.active())actions.put(i,()->select.accept(definition.id()));
        }
        if(slice.number()>0)ProfileMenu.button(c,actions,18,Items.ARROW,messages.text(settings.language(),"page_previous"),()->page.accept(slice.number()-1));if(slice.number()+1<slice.pages())ProfileMenu.button(c,actions,26,Items.ARROW,messages.text(settings.language(),"page_next"),()->page.accept(slice.number()+1));ProfileMenu.button(c,actions,22,Items.ARROW,messages.text(settings.language(),"home.return_home"),back);MainMenu.show(viewer,HomeMenu.gradient(messages.text(settings.language(),"home.achievements").getString(),settings.home().palette()),c,actions);
    }
    public static void favorite(ServerPlayer player,Settings settings,Messages messages,AchievementStore.Medal medal,IntConsumer select,Runnable back){
        var c=new SimpleContainer(27);var actions=new HashMap<Integer,Runnable>();ProfileMenu.card(c,4,Items.NETHER_STAR,Component.literal(medal.definition().name()),List.of(Component.literal(medal.definition().description())));
        for(int slot=0;slot<3;slot++){int chosen=slot;ProfileMenu.button(c,actions,11+slot*2,Items.GOLD_NUGGET,messages.text(settings.language(),"achievement_favorite",slot+1),()->select.accept(chosen));}
        ProfileMenu.button(c,actions,18,Items.BARRIER,messages.text(settings.language(),"achievement_unfavorite"),()->select.accept(-1));ProfileMenu.button(c,actions,22,Items.ARROW,messages.text(settings.language(),"home.return_home"),back);MainMenu.show(player,HomeMenu.gradient(messages.text(settings.language(),"achievement_favorites").getString(),settings.home().palette()),c,actions);
    }
}
