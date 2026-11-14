package br.com.shirokaidex.cobblemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import br.com.shirokaidex.pokedex.*;
import br.com.shirokaidex.config.CollectionValidator;
import java.util.*;
import br.com.shirokaidex.wiki.Wiki;
import net.minecraft.resources.ResourceLocation;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Minecraft access stays on the server thread. Cached values contain only immutable Java data. */
public final class CobblemonAccess {
    private record Cached(long created, List<ProgressCalculator.Species> entries,
                          Map<DexCollection, ProgressCalculator.CollectionProgress> defaults,
                          List<CollectionDefinition> definitions, Map<String, ProgressCalculator.CollectionProgress> configured, List<Wiki.Entry> wiki) { }
    private List<CollectionDefinition> versionDefinitions=List.of();
    private Map<String,String> versions=Map.of();
    private final Map<UUID, Cached> cache = new HashMap<>();
    public record Progress(int total, int seen, int owned) { }
    public Progress progress(ServerPlayer player) {
        var national = collections(player).get(DexCollection.NATIONAL);
        return new Progress(national.progress().total(), national.seen(), national.progress().registered().orElseThrow());
    }
    private final Map<UUID,Set<br.com.shirokaidex.pokedex.CapturedShinyForm>> capturedShiny=new HashMap<>();
    public boolean hasCapturedShiny(UUID player){return capturedShiny.containsKey(player);}
    public void publishCapturedShiny(UUID player,Set<br.com.shirokaidex.pokedex.CapturedShinyForm> forms){capturedShiny.put(player,Set.copyOf(forms));invalidate(player);}
    public void forget(UUID player){capturedShiny.remove(player);invalidate(player);}
    public void invalidate(UUID player) { cache.remove(player); }
    public void clear() { cache.clear();capturedShiny.clear();versionDefinitions=List.of();versions=Map.of(); }
    public Map<String,String> collectionVersions(List<CollectionDefinition> definitions){
        if(!versionDefinitions.equals(definitions)||versions.isEmpty()){versions=br.com.shirokaidex.profiles.CollectionVersions.of(snapshot(PokemonSpecies.INSTANCE.getImplemented(),null),definitions);versionDefinitions=List.copyOf(definitions);}return versions;
    }
    public CollectionValidator validationSnapshot() {
        return new CollectionValidator(snapshot(PokemonSpecies.INSTANCE.getSpecies(), null),
                BuiltInRegistries.ITEM.keySet().stream().map(Object::toString).collect(Collectors.toUnmodifiableSet()));
    }
    public Map<DexCollection, ProgressCalculator.CollectionProgress> collections(ServerPlayer player) { return cached(player).defaults(); }
    public Map<String, ProgressCalculator.CollectionProgress> collections(ServerPlayer player, List<CollectionDefinition> definitions) {
        var previous = cached(player);
        if (previous.definitions().equals(definitions)) return previous.configured();
        var values = ProgressCalculator.calculate(previous.entries(), definitions,capturedShiny.get(player.getUUID()));
        cache.put(player.getUUID(), new Cached(previous.created(), previous.entries(), previous.defaults(), List.copyOf(definitions), values, previous.wiki()));
        return values;
    }
    public List<Wiki.Entry> wiki(ServerPlayer player) { return cached(player).wiki(); }
    private Cached cached(ServerPlayer player) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Pokédex access requires the server thread");
        long now = System.nanoTime();
        Cached previous = cache.get(player.getUUID());
        if (previous != null && now - previous.created() < TimeUnit.SECONDS.toNanos(5)) return previous;
        var dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
        var entries = snapshot(PokemonSpecies.INSTANCE.getImplemented(), dex);
        var wiki = entries.stream().map(entry -> new Wiki.Entry(PokemonSpecies.INSTANCE.getByIdentifier(ResourceLocation.parse(entry.id())).getName(), entry)).toList();
        var value = new Cached(now, entries, ProgressCalculator.calculate(entries,capturedShiny.get(player.getUUID())), List.of(), Map.of(), wiki);
        cache.put(player.getUUID(), value); return value;
    }
    private static List<ProgressCalculator.Species> snapshot(Collection<Species> speciesEntries, PokedexManager dex) {
        List<ProgressCalculator.Species> entries = new ArrayList<>();
        for (var species : speciesEntries) {
            var record = dex == null ? null : dex.getSpeciesRecord(species.getResourceIdentifier());
            List<ProgressCalculator.Form> forms = new ArrayList<>();
            var availableForms = new ArrayList<>(species.getForms());
            if (availableForms.stream().noneMatch(form -> form.getName().equalsIgnoreCase(species.getStandardForm().getName()))) availableForms.add(species.getStandardForm());
            for (var form : availableForms) {
                var formRecord = record == null ? null : record.getFormRecord(form.getName());
                var knowledge = formRecord == null ? PokedexEntryProgress.UNREGISTERED : formRecord.getKnowledge();
                boolean seen = knowledge.ordinal() >= PokedexEntryProgress.SEEN.ordinal();
                Set<String> types = new HashSet<>(); form.getTypes().forEach(type -> types.add(type.getName()));
                forms.add(new ProgressCalculator.Form(form.getName(), form.getLabels(), types, seen,
                        knowledge.ordinal() >= PokedexEntryProgress.OWNED.ordinal(),
                        seen && formRecord != null && formRecord.hasSeenShinyState(true)));
            }
            entries.add(new ProgressCalculator.Species(species.getResourceIdentifier().toString(),
                    record != null && record.hasAtLeast(PokedexEntryProgress.SEEN),
                    record != null && record.hasAtLeast(PokedexEntryProgress.OWNED), forms));
        }
        return List.copyOf(entries);
    }
}
