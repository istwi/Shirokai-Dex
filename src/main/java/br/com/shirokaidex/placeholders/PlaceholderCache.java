package br.com.shirokaidex.placeholders;

import br.com.shirokaidex.pokedex.ProgressCalculator;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Server-thread only. Expansions queue coalesced work; they never build a progress snapshot. */
public final class PlaceholderCache {
    private record Snapshot(Object revision, long created, Map<String, ProgressCalculator.CollectionProgress> values) { }
    private final Map<UUID, Snapshot> values = new HashMap<>();
    private final LinkedHashSet<UUID> pending = new LinkedHashSet<>();
    public Map<String, ProgressCalculator.CollectionProgress> read(UUID player, Object revision, long now) {
        var snapshot = values.get(player);
        if (snapshot == null || snapshot.revision() != revision) { pending.add(player); return Map.of(); }
        if (now - snapshot.created() >= TimeUnit.SECONDS.toNanos(5)) pending.add(player);
        // A refresh runs at the end of a server tick; use the previous valid revision meanwhile.
        return snapshot.values();
    }
    public void publish(UUID player, Object revision, long now, Map<String, ProgressCalculator.CollectionProgress> result) {
        values.put(player, new Snapshot(revision, now, Map.copyOf(result)));
    }
    public void invalidate(UUID player) { values.remove(player); pending.add(player); }
    public void remove(UUID player) { values.remove(player); pending.remove(player); }
    public UUID poll() {
        if (pending.isEmpty()) return null;
        var iterator = pending.iterator(); var player = iterator.next(); iterator.remove(); return player;
    }
    public void clear() { values.clear(); pending.clear(); }
}
