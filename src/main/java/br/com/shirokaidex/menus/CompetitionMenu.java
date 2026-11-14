package br.com.shirokaidex.menus;

import br.com.shirokaidex.competitions.*;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.wiki.Wiki;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

public final class CompetitionMenu {
    private CompetitionMenu() { }
    public static void selector(ServerPlayer player,Settings settings,Messages messages,List<CompetitionStore.Edition> editions,int page,boolean history,Consumer<String> navigate,IntConsumer change,Runnable home,Runnable prizes){
        var slice=Wiki.paginate(editions,page,18);renderSelector(player,settings,messages,slice.items(),slice.number(),slice.number()>0,slice.number()+1<slice.pages(),history,navigate,change,home,prizes);
    }
    public static void history(ServerPlayer player,Settings settings,Messages messages,List<CompetitionStore.Edition> editions,int page,boolean more,Consumer<String> navigate,IntConsumer change,Runnable home,Runnable prizes){
        renderSelector(player,settings,messages,editions,page,page>0,more,true,navigate,change,home,prizes);
    }
    private static void renderSelector(ServerPlayer player,Settings settings,Messages messages,List<CompetitionStore.Edition> editions,int page,boolean previous,boolean next,boolean history,Consumer<String> navigate,IntConsumer change,Runnable home,Runnable prizes){
        var contents=new SimpleContainer(27);var actions=new HashMap<Integer,Runnable>();
        for(int i=0;i<editions.size();i++){var e=editions.get(i);put(contents,actions,i,Items.GOLDEN_SWORD,Component.literal(e.definition().name()),List.of(Component.literal(e.id()),messages.text(settings.language(),"competition_state",e.state()),messages.text(settings.language(),"competition_period",e.start(),e.end())),()->navigate.accept(e.id()));}
        if(previous)put(contents,actions,18,Items.ARROW,messages.text(settings.language(),"page_previous"),List.of(),()->change.accept(page-1));
        if(next)put(contents,actions,26,Items.ARROW,messages.text(settings.language(),"page_next"),List.of(),()->change.accept(page+1));
        put(contents,actions,22,Items.ARROW,messages.text(settings.language(),"home.return_home"),List.of(),home);
        put(contents,actions,24,Items.CHEST,messages.text(settings.language(),"competition_prizes"),List.of(),prizes);
        MainMenu.show(player,HomeMenu.gradient(messages.text(settings.language(),history?"competition_history":"competition_title").getString(),settings.home().palette()),contents,actions);
    }
    public static void edition(ServerPlayer player,Settings settings,Messages messages,Database.CompetitionView view,int page,IntConsumer change,Runnable join,Runnable cancel,Runnable back,Runnable prizes){
        var e=view.edition();var contents=new SimpleContainer(36);var actions=new HashMap<Integer,Runnable>();int slot=0;
        if(e.definition().visibility()!=CompetitionDefinition.Visibility.HIDDEN)for(var standing:view.standings()){
            if(e.definition().visibility()==CompetitionDefinition.Visibility.PARTIAL&&!standing.player().equals(player.getUUID()))continue;
            put(contents,actions,slot++,Items.PLAYER_HEAD,Component.literal(standing.position()+". "+standing.name()),List.of(messages.text(settings.language(),"ranking_score",standing.score())),()->{});
        }
        var membership=view.membership();
        put(contents,actions,22,Items.PAPER,messages.text(settings.language(),"competition_state",e.state()),List.of(messages.text(settings.language(),"competition_period",e.start(),e.end()),messages.text(settings.language(),"competition_member",membership.joined(),membership.cancelled(),membership.score()),messages.text(settings.language(),"competition_review_until",e.reviewUntil())),()->{});
        if(e.state()==CompetitionStore.State.ACTIVE){
            if(!membership.joined())put(contents,actions,20,Items.LIME_DYE,messages.text(settings.language(),"competition_join"),List.of(messages.text(settings.language(),"competition_join_rule")),join);
            else if(!membership.cancelled())put(contents,actions,24,Items.RED_DYE,messages.text(settings.language(),"competition_cancel"),List.of(messages.text(settings.language(),"competition_cancel_warning")),cancel);
        }
        if(page>0)put(contents,actions,27,Items.ARROW,messages.text(settings.language(),"page_previous"),List.of(),()->change.accept(page-1));
        if(e.definition().visibility()==CompetitionDefinition.Visibility.PUBLIC&&view.standings().size()==10)put(contents,actions,35,Items.ARROW,messages.text(settings.language(),"page_next"),List.of(),()->change.accept(page+1));
        put(contents,actions,29,Items.CHEST,messages.text(settings.language(),"competition_prizes"),List.of(),prizes);
        put(contents,actions,31,Items.ARROW,messages.text(settings.language(),"home.return_home"),List.of(),back);
        MainMenu.show(player,HomeMenu.gradient(e.definition().name()+" ["+(page+1)+"]",settings.home().palette()),contents,actions);
    }
    public static void confirm(ServerPlayer player,Settings settings,Messages messages,String description,Runnable confirm,Runnable back){
        var contents=new SimpleContainer(27);var actions=new HashMap<Integer,Runnable>();
        put(contents,actions,13,Items.RED_DYE,messages.text(settings.language(),"competition_confirm"),List.of(Component.literal(description)),confirm);
        put(contents,actions,15,Items.ARROW,messages.text(settings.language(),"home.return_home"),List.of(),back);
        MainMenu.show(player,HomeMenu.gradient(messages.text(settings.language(),"competition_confirm").getString(),settings.home().palette()),contents,actions);
    }
    private static void put(SimpleContainer contents,Map<Integer,Runnable> actions,int slot,Item icon,Component name,List<Component> lore,Runnable action){var stack=new ItemStack(icon);MenuStyle.card(stack,name,lore,MenuStyle.NAVIGATION);contents.setItem(slot,stack);actions.put(slot,action);}
}
