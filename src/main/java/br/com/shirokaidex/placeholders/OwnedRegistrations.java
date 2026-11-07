package br.com.shirokaidex.placeholders;

import java.util.*;
import java.util.function.*;

/** Never replaces an existing handler or removes a handler subsequently installed by another mod. */
public final class OwnedRegistrations<K, V> implements AutoCloseable {
    private final Function<K,V> lookup;
    private final BiConsumer<K,V> register;
    private final Consumer<K> remove;
    private final Map<K,V> owned = new HashMap<>();
    public OwnedRegistrations(Function<K,V> lookup, BiConsumer<K,V> register, Consumer<K> remove) {
        this.lookup = lookup; this.register = register; this.remove = remove;
    }
    public boolean install(K key, V handler) {
        if (lookup.apply(key) != null) return false;
        owned.put(key, handler); register.accept(key, handler); return true;
    }
    public boolean owns(K key) { return owned.containsKey(key) && lookup.apply(key) == owned.get(key); }
    @Override public void close() {
        for (var entry : owned.entrySet()) if (lookup.apply(entry.getKey()) == entry.getValue()) remove.accept(entry.getKey());
        owned.clear();
    }
}
