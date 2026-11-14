package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.profiles.*;
import br.com.shirokaidex.pokedex.DexProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class ProfileMenu {
    private ProfileMenu() { }
    public static void open(ServerPlayer viewer,Settings settings,Messages messages,ProfileStore.Data data,Runnable species,Runnable categories,Runnable records,Runnable medals,Runnable back){
        var c=new SimpleContainer(54);var actions=new HashMap<Integer,Runnable>();var language=settings.language();
        card(c,4,Items.PLAYER_HEAD,Component.literal(data.identity().name()),List.of(Component.literal(data.identity().player().toString())));
        int[] slots={10,12,14};var keys=List.of(StatisticsPrivacy.Key.TOTAL_CAPTURES,StatisticsPrivacy.Key.SHINY_CAPTURES,StatisticsPrivacy.Key.LEGENDARY_CAPTURES);
        for(int i=0;i<keys.size();i++){var key=keys.get(i);card(c,slots[i],Items.PAPER,messages.text(language,"privacy_stat."+key.id()),List.of(data.statistics().containsKey(key)?Component.literal(String.valueOf(data.statistics().get(key))):messages.text(language,"profile_private")));}
        card(c,16,Items.CLOCK,messages.text(language,"profile_periods"),List.of(data.today()==null?messages.text(language,"profile_private"):messages.text(language,"profile_period_values",data.today(),data.week(),data.month(),data.timezone().getId())));
        var progressLines=new ArrayList<Component>();
        for(var definition:settings.collections()){
            if(!br.com.shirokaidex.permissions.Permissions.has(viewer,definition.permission()))continue;
            var proof=data.collections().stream().filter(p->p.collection().equals(definition.id())).findFirst().orElse(null);
            String value=definition.mode()==br.com.shirokaidex.pokedex.CollectionDefinition.Mode.SHINY_CAPTURED&&!data.visibleStatistics().contains(StatisticsPrivacy.Key.SHINY_CAPTURES)?messages.text(language,"profile_private").getString():proof==null||proof.registered()==null?"N/D":proof.registered()+"/"+proof.total()+" ("+DexProgress.known(proof.total(),proof.registered()).formattedPercentage(language).orElse("N/D")+")";
            progressLines.add(Component.literal((definition.name().isBlank()?definition.id():definition.name())+": "+value));if(proof!=null)progressLines.add(messages.text(language,"profile_observed",proof.observedAt()));
        }
        card(c,22,Items.BOOK,messages.text(language,"profile_pokedex"),progressLines);
        var rankLines=new ArrayList<Component>();for(var entry:data.rankingPositions().entrySet())rankLines.add(Component.literal(entry.getKey()+": #"+entry.getValue()));card(c,20,Items.GOLD_INGOT,messages.text(language,"ranking_title"),rankLines);
        card(c,24,Items.CHEST,messages.text(language,"profile_rewards"),List.of(Component.literal(String.valueOf(data.claimedRewards()))));card(c,31,Items.GOLDEN_SWORD,messages.text(language,"profile_victories"),List.of(Component.literal(String.valueOf(data.victories()))));
        int favorites=0;for(var medal:data.medals().stream().filter(m->m.favoriteSlot()!=null).sorted(Comparator.comparing(m->m.favoriteSlot())).toList()){if(favorites>=3)break;card(c,37+favorites++,Items.NETHER_STAR,Component.literal(medal.definition().name()),List.of(Component.literal(medal.definition().description())));}
        if(data.visibleStatistics().contains(StatisticsPrivacy.Key.SPECIES_CAPTURES))button(c,actions,28,Items.SPYGLASS,messages.text(language,"privacy_stat.species_captures"),species);
        if(data.visibleStatistics().contains(StatisticsPrivacy.Key.CATEGORY_CAPTURES))button(c,actions,30,Items.NAME_TAG,messages.text(language,"privacy_stat.category_captures"),categories);
        if(data.visibleStatistics().contains(StatisticsPrivacy.Key.PERSONAL_RECORDS))button(c,actions,32,Items.CLOCK,messages.text(language,"privacy_stat.personal_records"),records);
        button(c,actions,34,Items.NETHER_STAR,messages.text(language,"home.achievements"),medals);button(c,actions,49,Items.ARROW,messages.text(language,"home.return_home"),back);
        MainMenu.show(viewer,HomeMenu.gradient(messages.text(language,"profile_title",data.identity().name()).getString(),settings.home().palette()),c,actions);
    }
    public static void detail(ServerPlayer player,Settings settings,Messages messages,String title,List<Component> rows,int requested,java.util.function.IntConsumer page,Runnable back){
        var c=new SimpleContainer(27);var actions=new HashMap<Integer,Runnable>();var slice=br.com.shirokaidex.wiki.Wiki.paginate(rows,requested,18);
        for(int i=0;i<slice.items().size();i++)card(c,i,Items.PAPER,slice.items().get(i),List.of());if(slice.number()>0)button(c,actions,18,Items.ARROW,messages.text(settings.language(),"page_previous"),()->page.accept(slice.number()-1));if(slice.number()+1<slice.pages())button(c,actions,26,Items.ARROW,messages.text(settings.language(),"page_next"),()->page.accept(slice.number()+1));button(c,actions,22,Items.ARROW,messages.text(settings.language(),"home.return_home"),back);MainMenu.show(player,HomeMenu.gradient(title,settings.home().palette()),c,actions);
    }
    static void card(SimpleContainer contents,int slot,Item icon,Component name,List<Component> lore){var item=new ItemStack(icon);MenuStyle.card(item,name,lore,MenuStyle.NAVIGATION);contents.setItem(slot,item);}
    static void button(SimpleContainer contents,Map<Integer,Runnable> actions,int slot,Item icon,Component name,Runnable action){card(contents,slot,icon,name,List.of());actions.put(slot,action);}
}
