package br.com.shirokaidex.pokedex;

import java.util.*;

/** Pure calculation over immutable snapshots, without Minecraft calls or side effects. */
public final class ProgressCalculator {
    public record Form(boolean legendary, boolean seen, boolean owned) {
        public Form { if (owned && !seen) throw new IllegalArgumentException("Owned implies seen"); }
    }
    public record Species(String id, boolean seen, boolean owned, List<Form> forms) {
        public Species {
            Objects.requireNonNull(id); forms = List.copyOf(forms);
            if (owned && !seen) throw new IllegalArgumentException("Owned implies seen");
        }
    }
    public record CollectionProgress(DexProgress progress, int seen) {
        public CollectionProgress {
            if (seen < 0 || seen > progress.total()) throw new IllegalArgumentException("Inconsistent seen count");
        }
    }
    private ProgressCalculator() { }

    public static Map<DexCollection, CollectionProgress> calculate(Collection<Species> entries) {
        Map<String, Species> unique = new HashMap<>();
        for (Species species : entries) unique.merge(species.id(), species, (left, right) -> {
            List<Form> combined = new ArrayList<>(left.forms()); combined.addAll(right.forms());
            return new Species(left.id(), left.seen() || right.seen(), left.owned() || right.owned(), combined);
        });
        int seen = 0, owned = 0, legendaryTotal = 0, legendarySeen = 0, legendaryOwned = 0;
        for (var species : unique.values()) {
            var forms = species.forms();
            if (species.seen()) seen++;
            if (species.owned()) owned++;
            if (forms.stream().anyMatch(Form::legendary)) {
                legendaryTotal++;
                if (forms.stream().anyMatch(form -> form.legendary() && form.seen())) legendarySeen++;
                if (forms.stream().anyMatch(form -> form.legendary() && form.owned())) legendaryOwned++;
            }
        }
        return Map.of(
                DexCollection.NATIONAL, new CollectionProgress(DexProgress.known(unique.size(), owned), seen),
                DexCollection.LEGENDARY, new CollectionProgress(DexProgress.known(legendaryTotal, legendaryOwned), legendarySeen),
                // The official seen-shiny set cannot prove captured-shiny progress.
                DexCollection.SHINY, new CollectionProgress(DexProgress.unavailable(unique.size()), 0));
    }
}
