package br.com.shirokaidex.config;

import br.com.shirokaidex.pokedex.*;
import java.io.IOException;
import java.util.*;

/** Immutable registry snapshot captured on the server thread; validation itself is safe off-thread. */
public final class CollectionValidator {
    private final Set<String> species, labels, types, forms, items;
    public CollectionValidator(Collection<ProgressCalculator.Species> catalog, Set<String> items) {
        Set<String> ids = new HashSet<>(), labels = new HashSet<>(), types = new HashSet<>(), forms = new HashSet<>();
        // These are verified Cobblemon labels even if the current datapacks contain no matching species.
        labels.addAll(Set.of("legendary", "mythical", "ultra_beast"));
        for (var entry : catalog) {
            ids.add(entry.id());
            for (var form : entry.forms()) { labels.addAll(form.labels()); types.addAll(form.types()); forms.add(form.name()); }
        }
        this.species = Set.copyOf(ids); this.labels = Set.copyOf(labels); this.types = Set.copyOf(types);
        this.forms = Set.copyOf(forms); this.items = Set.copyOf(items);
    }
    public void validate(Settings settings) throws IOException {
        for (var definition : settings.collections()) {
            if (!items.contains(definition.icon())) throw new IOException("config.json collections." + definition.id() + ".icon: unknown item " + definition.icon());
            check(definition, "include_species", definition.criteria().includeSpecies(), species);
            check(definition, "exclude_species", definition.criteria().excludeSpecies(), species);
            check(definition, "labels", definition.criteria().labels(), labels);
            check(definition, "types", definition.criteria().types(), types);
            check(definition, "forms", definition.criteria().forms(), forms);
        }
    }
    private static void check(CollectionDefinition definition, String key, Set<String> values, Set<String> known) throws IOException {
        for (String value : values) if (!known.contains(value)) throw new IOException("config.json collections." + definition.id() + ".criteria." + key + ": unknown reference " + value);
    }
}
