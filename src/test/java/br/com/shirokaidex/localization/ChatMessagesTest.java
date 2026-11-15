package br.com.shirokaidex.localization;

import br.com.shirokaidex.menus.direct.RichText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatMessagesTest {
    @Test void rewardNotificationHasWhiteBoldBrandAndRegularGrayBody(){
        var text=ChatMessages.format(new Messages().text("pt_br","notification.reward_unlocked","Nacional 5%"));
        assertEquals("✦ SHIROKAI » Recompensa desbloqueada: Nacional 5%",text.getString());
        text.visit((style,part)->{
            if(part.isEmpty())return Optional.empty();
            boolean brand=part.equals("✦ SHIROKAI");
            assertEquals((brand?ChatFormatting.WHITE:ChatFormatting.GRAY).getColor(),style.getColor().getValue());
            assertEquals(brand,style.isBold());assertFalse(style.isItalic());return Optional.empty();
        },Style.EMPTY);
    }
    @Test void existingRichTextColorsAndBoldDoNotBleedIntoChatOrMutateMenuSource(){
        var original=RichText.render("<red><bold>Falha <yellow>aninhada</yellow></bold></red>",Map.of());
        var formatted=ChatMessages.format(original);
        formatted.getSiblings().getLast().visit((style,part)->{
            assertEquals(ChatFormatting.GRAY.getColor(),style.getColor().getValue());assertFalse(style.isBold());return Optional.empty();
        },Style.EMPTY);
        assertEquals("Falha aninhada",original.getString());
        original.visit((style,part)->{if(!part.isEmpty())assertTrue(style.isBold());return Optional.empty();},Style.EMPTY);
        assertFalse(original.getString().contains("SHIROKAI"));
    }
    @Test void formattingPreservesClickBehaviorAndTreatsPlayerTextAsLiteral(){
        var click=new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/dex nacional");
        var body=Component.literal("<red>nome</red>").withStyle(style->style.withClickEvent(click));
        var formatted=ChatMessages.format(body);var content=formatted.getSiblings().getLast();
        assertEquals(click,content.getStyle().getClickEvent());assertTrue(formatted.getString().endsWith("<red>nome</red>"));assertEquals(body.getString(),content.getString());
    }
}
