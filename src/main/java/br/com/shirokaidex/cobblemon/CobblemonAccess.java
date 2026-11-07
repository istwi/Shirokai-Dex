package br.com.shirokaidex.cobblemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import net.minecraft.server.level.ServerPlayer;
import br.com.shirokaidex.pokedex.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Read-only access to the real registry and official records. Invoke on the server thread. */
public final class CobblemonAccess {
    private record Cached(long created, Map<DexCollection, ProgressCalculator.CollectionProgress> values) { }
    private final Map<UUID, Cached> cache = new HashMap<>();
    public record Progress(int total, int seen, int owned) { }
    public Progress progress(ServerPlayer player) {
        var national = collections(player).get(DexCollection.NATIONAL);
        return new Progress(national.progress().total(), national.seen(), national.progress().registered().orElseThrow());
    }
    /** Invalidation is also confined to the server thread. */
    public void invalidate(UUID player) { cache.remove(player); }
    public void clear() { cache.clear(); }
    public Map<DexCollection, ProgressCalculator.CollectionProgress> collections(ServerPlayer player) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Pokédex access requires the server thread");
        long now = System.nanoTime();
        Cached previous = cache.get(player.getUUID());
        if (previous != null && now - previous.created() < TimeUnit.SECONDS.toNanos(5)) return previous.values();
        var dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
        List<ProgressCalculator.Species> entries = new ArrayList<>();
        for (var species : PokemonSpecies.INSTANCE.getImplemented()) {
            var record = dex.getSpeciesRecord(species.getResourceIdentifier());
            List<ProgressCalculator.Form> forms = new ArrayList<>();
            // Species without alternate forms may have an empty forms list in 1.8.3.
            var availableForms = new ArrayList<>(species.getForms());
            if (availableForms.stream().noneMatch(form -> form.getName().equalsIgnoreCase(species.getStandardForm().getName()))) {
                availableForms.add(species.getStandardForm());
            }
            for (var form : availableForms) {
                var formRecord = record == null ? null : record.getFormRecord(form.getName());
                var knowledge = formRecord == null ? PokedexEntryProgress.UNREGISTERED : formRecord.getKnowledge();
                forms.add(new ProgressCalculator.Form(
                        form.getLabels().stream().anyMatch(label -> label.equalsIgnoreCase("legendary")),
                        knowledge.ordinal() >= PokedexEntryProgress.SEEN.ordinal(),
                        knowledge.ordinal() >= PokedexEntryProgress.OWNED.ordinal()));
            }
            entries.add(new ProgressCalculator.Species(species.getResourceIdentifier().toString(),
                    record != null && record.hasAtLeast(PokedexEntryProgress.SEEN),
                    record != null && record.hasAtLeast(PokedexEntryProgress.OWNED), forms));
        }
        var values = ProgressCalculator.calculate(entries);
        cache.put(player.getUUID(), new Cached(now, values));
        return values;
    }
}
