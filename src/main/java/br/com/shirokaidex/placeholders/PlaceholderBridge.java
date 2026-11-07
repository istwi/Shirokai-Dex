package br.com.shirokaidex.placeholders;

import net.minecraft.server.level.ServerPlayer;

/** Keeps optional API types out of the main initializer's field/method signatures. */
public interface PlaceholderBridge extends AutoCloseable {
    String preview(ServerPlayer player, String metric, String collection);
    @Override void close();
}
