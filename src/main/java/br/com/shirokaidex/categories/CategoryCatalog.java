package br.com.shirokaidex.categories;

import br.com.shirokaidex.pokedex.CollectionDefinition.Criteria;
import br.com.shirokaidex.pokedex.ProgressCalculator;
import java.util.*;

/** Immutable classification rules. Does not classify historical captures or modify official records. */
public final class CategoryCatalog {
    public record Definition(String id, Criteria criteria, Set<String> allOf, Set<String> anyOf, Set<String> noneOf) {
        public Definition {
            identifier(id); Objects.requireNonNull(criteria);
            allOf = Set.copyOf(allOf); anyOf = Set.copyOf(anyOf); noneOf = Set.copyOf(noneOf);
            for (var refs : List.of(allOf, anyOf, noneOf)) refs.forEach(CategoryCatalog::identifier);
        }
        Set<String> references() {
            Set<String> result = new HashSet<>(allOf); result.addAll(anyOf); result.addAll(noneOf); return result;
        }
    }
    private final Map<String, Definition> definitions;
    public CategoryCatalog(List<Definition> entries) {
        if (entries.size() > 64) throw new IllegalArgumentException("maximum 64 categories");
        Map<String, Definition> candidate = new LinkedHashMap<>();
        for (var entry : entries) if (candidate.putIfAbsent(entry.id(), entry) != null)
            throw new IllegalArgumentException("duplicate category " + entry.id());
        Set<String> done = new HashSet<>();
        for (String id : candidate.keySet()) visit(id, candidate, new LinkedHashSet<>(), done);
        definitions = Collections.unmodifiableMap(candidate);
    }
    private static void visit(String id, Map<String, Definition> entries, Set<String> active, Set<String> done) {
        if (done.contains(id)) return;
        Definition entry = entries.get(id);
        if (entry == null) throw new IllegalArgumentException("unknown category reference " + id);
        if (!active.add(id)) throw new IllegalArgumentException("category cycle " + active + " -> " + id);
        for (String ref : entry.references()) visit(ref, entries, active, done);
        active.remove(id); done.add(id);
    }
    public Map<String, Definition> definitions() { return definitions; }
    @Override public boolean equals(Object other) {
        return other instanceof CategoryCatalog catalog && definitions.equals(catalog.definitions);
    }
    @Override public int hashCode() { return definitions.hashCode(); }
    /** Every referenced condition is evaluated on this exact same form. */
    public boolean matches(String category, String species, ProgressCalculator.Form form) {
        if (!definitions.containsKey(category)) throw new IllegalArgumentException("unknown category " + category);
        return evaluate(category, species, Objects.requireNonNull(form), new HashMap<>());
    }
    private boolean evaluate(String id, String species, ProgressCalculator.Form form, Map<String, Boolean> memo) {
        Boolean cached = memo.get(id); if (cached != null) return cached;
        Definition d = definitions.get(id);
        boolean result = d.criteria().acceptsSpecies(species) && d.criteria().acceptsForm(form)
                && d.allOf().stream().allMatch(ref -> evaluate(ref, species, form, memo))
                && (d.anyOf().isEmpty() || d.anyOf().stream().anyMatch(ref -> evaluate(ref, species, form, memo)))
                && d.noneOf().stream().noneMatch(ref -> evaluate(ref, species, form, memo));
        memo.put(id, result); return result;
    }
    /** A union of matching forms still counts each species only once. */
    public Set<String> species(String category, Collection<ProgressCalculator.Species> catalog) {
        if (!definitions.containsKey(category)) throw new IllegalArgumentException("unknown category " + category);
        Set<String> result = new TreeSet<>();
        for (var entry : catalog) if (entry.forms().stream().anyMatch(form -> matches(category, entry.id(), form))) result.add(entry.id());
        return Collections.unmodifiableSet(result);
    }
    private static void identifier(String id) {
        if (id == null || !id.matches("[a-z][a-z0-9_]{0,47}")) throw new IllegalArgumentException("invalid category id");
    }
}
