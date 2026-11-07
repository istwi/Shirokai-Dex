package br.com.shirokaidex.pokedex;

import java.util.*;
import br.com.shirokaidex.categories.CategoryCatalog;

/** Immutable user configuration; no executable actions or command templates. */
public record CollectionDefinition(String id, String name, String description, String icon,
                                   Mode mode, Criteria criteria, String permission, CategorySelection category) {
    public record CategorySelection(String id, CategoryCatalog catalog) {
        public CategorySelection {
            if (id == null || !id.matches("[a-z][a-z0-9_]{0,47}")) throw new IllegalArgumentException("invalid category reference");
        }
    }
    public CollectionDefinition(String id, String name, String description, String icon, Mode mode, Criteria criteria, String permission) {
        this(id, name, description, icon, mode, criteria, permission, null);
    }
    public CollectionDefinition bind(CategoryCatalog catalog) {
        if (category == null) return this;
        if (!catalog.definitions().containsKey(category.id())) throw new IllegalArgumentException("unknown category " + category.id() + " in collection " + id);
        return new CollectionDefinition(id, name, description, icon, mode, criteria, permission, new CategorySelection(category.id(), catalog));
    }
    public boolean filtersForms() { return criteria.filtersForms() || category != null; }
    public boolean acceptsForm(String species, ProgressCalculator.Form form) {
        return criteria.acceptsForm(form) && (category == null || Objects.requireNonNull(category.catalog(), "unbound category").matches(category.id(), species, form));
    }
    public enum Mode { OWNED, SEEN, SHINY_SEEN, SHINY_CAPTURED }
    private static final Set<String> RESERVED = Set.of("admin", "status", "help", "rewards", "top", "achievements", "profile", "settings", "history", "language");
    public record Criteria(Set<String> includeSpecies, Set<String> excludeSpecies, Set<String> labels,
                           Set<String> types, Set<String> forms) {
        public Criteria {
            includeSpecies = Set.copyOf(includeSpecies); excludeSpecies = Set.copyOf(excludeSpecies);
            labels = Set.copyOf(labels); types = Set.copyOf(types); forms = Set.copyOf(forms);
            for (String id : includeSpecies) identifier(id);
            for (String id : excludeSpecies) identifier(id);
            for (var values : List.of(labels, types, forms)) for (String value : values) {
                text(value, 64, false, "criterion");
                if (!value.equals(value.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("criteria values must be lowercase");
            }
        }
        public static Criteria all() { return new Criteria(Set.of(), Set.of(), Set.of(), Set.of(), Set.of()); }
        public boolean acceptsSpecies(String id) { return !excludeSpecies.contains(id) && (includeSpecies.isEmpty() || includeSpecies.contains(id)); }
        public boolean filtersForms() { return !labels.isEmpty() || !types.isEmpty() || !forms.isEmpty(); }
        public boolean acceptsForm(ProgressCalculator.Form form) {
            return (labels.isEmpty() || !Collections.disjoint(labels, form.labels()))
                    && (types.isEmpty() || !Collections.disjoint(types, form.types()))
                    && (forms.isEmpty() || forms.contains(form.name()));
        }
    }
    public CollectionDefinition {
        if (id == null || !id.matches("[a-z][a-z0-9_]{0,47}") || RESERVED.contains(id)) throw new IllegalArgumentException("invalid or reserved collection id");
        boolean builtin = Set.of("national", "shiny", "legendary").contains(id);
        text(name, 64, builtin, "name"); text(description, 512, true, "description");
        identifier(icon);
        if (icon.equals("minecraft:air")) throw new IllegalArgumentException("icon cannot be air");
        Objects.requireNonNull(mode, "progress_mode"); Objects.requireNonNull(criteria, "criteria");
        if (permission == null || permission.length() > 128 || (!permission.isEmpty() && !permission.matches("[a-z0-9_.-]+"))) throw new IllegalArgumentException("invalid permission");
    }
    public static List<CollectionDefinition> defaults() {
        return List.of(
                new CollectionDefinition("national", "", "", "minecraft:book", Mode.OWNED, Criteria.all(), ""),
                new CollectionDefinition("shiny", "", "", "minecraft:amethyst_shard", Mode.SHINY_CAPTURED, Criteria.all(), ""),
                new CollectionDefinition("legendary", "", "", "minecraft:nether_star", Mode.OWNED,
                        new Criteria(Set.of(), Set.of(), Set.of("legendary"), Set.of(), Set.of()), ""));
    }
    public static void identifier(String id) {
        if (id == null || id.length() > 256 || !id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw new IllegalArgumentException("expected namespaced identifier");
    }
    private static void text(String value, int length, boolean empty, String field) {
        if (value == null || value.length() > length || (!empty && value.isBlank()) || value.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("invalid " + field);
    }
}
