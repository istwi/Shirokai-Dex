package br.com.shirokaidex.pokedex;

import java.util.*;

/** Pure calculation over immutable snapshots, without Minecraft calls or side effects. */
public final class ProgressCalculator {
    public record Form(String name, Set<String> labels, Set<String> types, boolean seen, boolean owned, boolean shinySeen) {
        public Form {
            name = name.toLowerCase(Locale.ROOT);
            labels = lower(labels); types = lower(types);
            if (owned && !seen) throw new IllegalArgumentException("Owned implies seen");
            if (shinySeen && !seen) throw new IllegalArgumentException("Shiny seen implies seen");
        }
        public Form(boolean legendary, boolean seen, boolean owned) {
            this("normal", legendary ? Set.of("legendary") : Set.of(), Set.of(), seen, owned, false);
        }
        public boolean legendary() { return labels.contains("legendary"); }
        private static Set<String> lower(Set<String> values) {
            Set<String> result = new HashSet<>(); values.forEach(value -> result.add(value.toLowerCase(Locale.ROOT))); return Set.copyOf(result);
        }
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
        var values = calculate(entries, CollectionDefinition.defaults());
        return Map.of(DexCollection.NATIONAL, values.get("national"), DexCollection.LEGENDARY, values.get("legendary"), DexCollection.SHINY, values.get("shiny"));
    }
    public static Map<String, CollectionProgress> calculate(Collection<Species> entries, List<CollectionDefinition> definitions) {
        Map<String, Species> unique = new HashMap<>();
        for (Species species : entries) unique.merge(species.id(), species, (left, right) -> {
            List<Form> combined = new ArrayList<>(left.forms()); combined.addAll(right.forms());
            return new Species(left.id(), left.seen() || right.seen(), left.owned() || right.owned(), combined);
        });
        Map<String, CollectionProgress> result = new LinkedHashMap<>();
        for (var definition : definitions) {
            int total = 0, seen = 0, registered = 0;
            var criteria = definition.criteria();
            for (var species : unique.values()) {
                if (!criteria.acceptsSpecies(species.id())) continue;
                var eligible = species.forms().stream().filter(form -> definition.acceptsForm(species.id(), form)).toList();
                if (definition.filtersForms() && eligible.isEmpty()) continue;
                total++;
                boolean hasSeen = definition.filtersForms() ? eligible.stream().anyMatch(Form::seen) : species.seen();
                boolean hasOwned = definition.filtersForms() ? eligible.stream().anyMatch(Form::owned) : species.owned();
                boolean hasShinySeen = eligible.stream().anyMatch(Form::shinySeen);
                if (definition.mode() == CollectionDefinition.Mode.SHINY_SEEN || definition.mode() == CollectionDefinition.Mode.SHINY_CAPTURED) {
                    if (hasShinySeen) seen++;
                } else if (hasSeen) seen++;
                boolean registeredSpecies = switch (definition.mode()) {
                    case OWNED -> hasOwned;
                    case SEEN -> hasSeen;
                    case SHINY_SEEN -> hasShinySeen;
                    case SHINY_CAPTURED -> false; // No verified capture-history provider yet.
                };
                if (registeredSpecies) registered++;
            }
            DexProgress progress = definition.mode() == CollectionDefinition.Mode.SHINY_CAPTURED
                    ? DexProgress.unavailable(total) : DexProgress.known(total, registered);
            if (result.put(definition.id(), new CollectionProgress(progress, seen)) != null) throw new IllegalArgumentException("Duplicate collection id: " + definition.id());
        }
        return Collections.unmodifiableMap(result);
    }
}
