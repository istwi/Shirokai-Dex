package br.com.shirokaidex.menus;

import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.localization.Messages;
import br.com.shirokaidex.profiles.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Items;
import java.util.*;
import java.util.function.*;

public final class SettingsMenu {
    private SettingsMenu() { }
    public static void open(ServerPlayer player,Settings settings,Messages messages,Map<StatisticsPrivacy.Key,Boolean> overrides,boolean hidden,boolean notifications,BiConsumer<StatisticsPrivacy.Key,Boolean> change,Consumer<Boolean> visibility,Consumer<Boolean> notify,Runnable back){
        var c=new SimpleContainer(27);var actions=new HashMap<Integer,Runnable>();int slot=9;
        for(var key:StatisticsPrivacy.Key.values()){
            boolean visible=settings.statisticsPrivacy().visible(key,overrides);ProfileMenu.card(c,slot,visible?Items.LIME_DYE:Items.GRAY_DYE,messages.text(settings.language(),"privacy_stat."+key.id()),List.of(messages.text(settings.language(),"settings_visible",visible),messages.text(settings.language(),settings.statisticsPrivacy().enabled()&&settings.statisticsPrivacy().allowPlayerOverride()?"settings_click_toggle":"settings_fixed")));
            if(settings.statisticsPrivacy().enabled()&&settings.statisticsPrivacy().allowPlayerOverride())actions.put(slot,()->change.accept(key,!visible));slot++;
        }
        ProfileMenu.card(c,16,hidden?Items.GRAY_DYE:Items.LIME_DYE,messages.text(settings.language(),"settings_rankings"),List.of(messages.text(settings.language(),"ranking_privacy",hidden)));actions.put(16,()->visibility.accept(!hidden));
        ProfileMenu.card(c,17,notifications?Items.LIME_DYE:Items.GRAY_DYE,messages.text(settings.language(),"settings_notifications"),List.of(messages.text(settings.language(),"settings_enabled",notifications)));actions.put(17,()->notify.accept(!notifications));
        ProfileMenu.button(c,actions,22,Items.ARROW,messages.text(settings.language(),"home.return_home"),back);MainMenu.show(player,HomeMenu.gradient(messages.text(settings.language(),"home.settings").getString(),settings.home().palette()),c,actions);
    }
}
