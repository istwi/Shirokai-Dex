package br.com.shirokaidex.localization;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Chat-only branding; menu labels and other notification channels keep their own styles. */
public final class ChatMessages {
    private ChatMessages() { }
    public static Component format(Component body) {
        return Component.empty().withStyle(style->style.withColor(ChatFormatting.GRAY).withBold(false).withItalic(false))
                .append(Component.literal("✦ SHIROKAI").withStyle(style->style.withColor(ChatFormatting.WHITE).withBold(true).withItalic(false)))
                .append(" » ")
                .append(gray(body));
    }
    private static MutableComponent gray(Component body) {
        var result=body.plainCopy().setStyle(body.getStyle().withColor(ChatFormatting.GRAY).withBold(false).withItalic(false));
        for(var sibling:body.getSiblings())result.append(gray(sibling));
        return result;
    }
}
