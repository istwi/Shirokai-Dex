package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.rankings.*;
import br.com.shirokaidex.wiki.Wiki;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

public final class RankingsMenu {
    private RankingsMenu() { }
    public static void selector(ServerPlayer player,Settings settings,Messages messages,List<CaptureRanking> definitions,int page,Consumer<String> navigate,IntConsumer change,Runnable home,Runnable competitions) {
        var contents=new SimpleContainer(27);var actions=new HashMap<Integer,Runnable>();var slice=Wiki.paginate(definitions,page,18);
        for(int i=0;i<slice.items().size();i++){var definition=slice.items().get(i);put(contents,actions,i,Items.GOLD_INGOT,Component.literal(definition.name()),List.of(messages.text(settings.language(),"ranking_metric",definition.metric(),definition.key())),()->navigate.accept(definition.id()));}
        if(slice.number()>0)put(contents,actions,18,Items.ARROW,messages.text(settings.language(),"page_previous"),List.of(),()->change.accept(slice.number()-1));
        if(slice.number()+1<slice.pages())put(contents,actions,26,Items.ARROW,messages.text(settings.language(),"page_next"),List.of(),()->change.accept(slice.number()+1));
        put(contents,actions,22,Items.ARROW,messages.text(settings.language(),"home.return_home"),List.of(),home);
        put(contents,actions,24,Items.GOLDEN_SWORD,messages.text(settings.language(),"competition_title"),List.of(),competitions);
        MainMenu.show(player,HomeMenu.gradient(messages.text(settings.language(),"ranking_title").getString(),settings.home().palette()),contents,actions);
    }
    public static void ranking(ServerPlayer player,Settings settings,Messages messages,CaptureRanking definition,CaptureRankings.Page page,int number,IntConsumer change,Runnable back,Consumer<UUID> profile) {
        var contents=new SimpleContainer(27);var actions=new HashMap<Integer,Runnable>();
        for(int i=0;i<page.entries().size();i++){
            var entry=page.entries().get(i);var item=new ItemStack(Items.PLAYER_HEAD);
            var lore=new ArrayList<Component>();lore.add(Component.literal(entry.player().toString()));
            lore.add(entry.total()==null?messages.text(settings.language(),"ranking_score",entry.score()):messages.text(settings.language(),"profile_collection_score",entry.registered(),entry.total(),br.com.shirokaidex.pokedex.DexProgress.known(entry.total(),entry.registered()).formattedPercentage(settings.language()).orElse("N/D")));
            lore.add(messages.text(settings.language(),"ranking_reached",entry.reachedAt()==null?"N/D":entry.reachedAt()));if(entry.observedAt()!=null)lore.add(messages.text(settings.language(),"profile_observed",entry.observedAt()));
            MenuStyle.card(item,Component.literal((number*10+i+1)+". "+entry.name()),lore,MenuStyle.SUCCESS);contents.setItem(i,item);actions.put(i,()->profile.accept(entry.player()));
        }
        if(page.entries().isEmpty())put(contents,actions,13,Items.PAPER,messages.text(settings.language(),"ranking_empty"),List.of(),()->{});
        if(number>0)put(contents,actions,18,Items.ARROW,messages.text(settings.language(),"page_previous"),List.of(),()->change.accept(number-1));
        if(page.hasMore())put(contents,actions,26,Items.ARROW,messages.text(settings.language(),"page_next"),List.of(),()->change.accept(number+1));
        put(contents,actions,22,Items.ARROW,messages.text(settings.language(),"home.return_home"),List.of(),back);
        MainMenu.show(player,HomeMenu.gradient(definition.name()+" ["+(number+1)+"]",settings.home().palette()),contents,actions);
    }
    private static void put(SimpleContainer contents,Map<Integer,Runnable> actions,int slot,Item item,Component name,List<Component> lore,Runnable action){var stack=new ItemStack(item);MenuStyle.card(stack,name,lore,MenuStyle.NAVIGATION);contents.setItem(slot,stack);actions.put(slot,action);}
}
