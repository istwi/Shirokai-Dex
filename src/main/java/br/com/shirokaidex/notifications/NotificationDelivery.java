package br.com.shirokaidex.notifications;

import br.com.shirokaidex.localization.Messages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import java.util.Locale;

/** Server-thread-only presentation. No commands, reward effects, disk access or background Minecraft calls. */
public final class NotificationDelivery {
    private NotificationDelivery() { }
    public static void send(ServerPlayer player,NotificationOutbox.Notice notice,NotificationPolicy.Rule rule,String language,Messages messages){
        var text=messages.text(language,notice.event()==NotificationPolicy.Event.ACHIEVEMENT?"achievement_notification":"notification."+notice.event().name().toLowerCase(Locale.ROOT),notice.value());
        for(var channel:rule.channels())switch(channel){
            case CHAT -> player.sendSystemMessage(text);
            case ACTIONBAR -> player.displayClientMessage(text,true);
            case TITLE -> {player.connection.send(new ClientboundSetTitlesAnimationPacket(10,50,10));player.connection.send(new ClientboundSetTitleTextPacket(text));}
            case SOUND -> player.playNotifySound(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(rule.sound())),SoundSource.PLAYERS,.6f,1f);
        }
    }
}
