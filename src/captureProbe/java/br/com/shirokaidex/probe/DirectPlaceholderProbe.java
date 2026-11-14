package br.com.shirokaidex.probe;
/** Isolated optional API fixture, invoked only with its audited runtime installed. */
final class DirectPlaceholderProbe {
    static String parse(net.minecraft.server.level.ServerPlayer player,String input){
        return eu.pb4.placeholders.api.Placeholders.parseText(net.minecraft.network.chat.Component.literal(input),eu.pb4.placeholders.api.PlaceholderContext.of(player)).getString();
    }
}
