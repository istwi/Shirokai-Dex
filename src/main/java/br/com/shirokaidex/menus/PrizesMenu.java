package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.menus.layout.RewardView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import java.time.Instant;
import java.util.*;
import java.util.function.*;

public final class PrizesMenu {
    private PrizesMenu() { }
    public static void open(ServerPlayer player,Settings settings,Messages messages,List<Database.AwardView> awards,int page,Consumer<String> navigate,IntConsumer change,Runnable back){
        var contents=new SimpleContainer(27);var actions=new HashMap<Integer,Runnable>();int slot=0;
        for(var view:awards){var award=view.award();var status=RewardView.preview(award.available(Instant.now()),award.plan(),view.operation());var item=new ItemStack(status.canClaim()?Items.CHEST:Items.GRAY_DYE);
            MenuStyle.card(item,messages.text(settings.language(),"competition_prize_band",award.band()),List.of(Component.literal(award.edition()),messages.text(settings.language(),"layout.reward_status."+status.status().name()),messages.text(settings.language(),"competition_prize_expiry",award.expiresAt()==null?messages.text(settings.language(),"competition_prize_permanent").getString():award.expiresAt())),MenuStyle.NAVIGATION);contents.setItem(slot,item);String id=award.id();actions.put(slot++,()->navigate.accept(id));
        }
        if(page>0)button(contents,actions,18,messages.text(settings.language(),"page_previous"),()->change.accept(page-1));
        if(awards.size()==10)button(contents,actions,26,messages.text(settings.language(),"page_next"),()->change.accept(page+1));
        button(contents,actions,22,messages.text(settings.language(),"home.return_home"),back);
        MainMenu.show(player,HomeMenu.gradient(messages.text(settings.language(),"competition_prizes").getString(),settings.home().palette()),contents,actions);
    }
    private static void button(SimpleContainer c,Map<Integer,Runnable> actions,int slot,Component label,Runnable action){var item=new ItemStack(Items.ARROW);MenuStyle.card(item,label,List.of(),MenuStyle.NAVIGATION);c.setItem(slot,item);actions.put(slot,action);}
}
