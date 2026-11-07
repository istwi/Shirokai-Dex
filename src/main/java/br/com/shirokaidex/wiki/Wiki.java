package br.com.shirokaidex.wiki;

import br.com.shirokaidex.pokedex.CollectionDefinition;
import br.com.shirokaidex.pokedex.ProgressCalculator;
import java.util.*;

/** Read-only query over the same immutable official progress snapshot used by collections. */
public final class Wiki {
    public enum Filter { ALL, SEEN, OWNED, UNSEEN, SHINY_SEEN;
        public Filter next() { return values()[(ordinal() + 1) % values().length]; }
    }
    public enum Order { NAME, ID;
        public Order next() { return this == NAME ? ID : NAME; }
    }
    public record Entry(String name, ProgressCalculator.Species species) { }
    public record Row(String name, String id, List<ProgressCalculator.Form> forms, boolean seen, boolean owned, boolean shinySeen) {
        public Row { forms = List.copyOf(forms); }
    }
    public record Request(String query, Filter filter, Order order, int page, String detail) {
        public static Request initial(String query) { return new Request(query, Filter.ALL, Order.NAME, 0, null); }
        public Request page(int value) { return new Request(query, filter, order, value, null); }
        public Request detail(String id) { return new Request(query, filter, order, page, id); }
    }
    public record Page(List<Row> rows, int total, int number, int pages, Optional<Row> detail) {
        public Page { rows = List.copyOf(rows); }
    }
    private Wiki() { }
    public static Page select(List<Entry> entries, CollectionDefinition definition, Request request, int size) {
        if (size < 1 || size > 54 || request.query().length() > 128) throw new IllegalArgumentException("Invalid wiki query/page size");
        String query = request.query().strip().toLowerCase(Locale.ROOT);
        var criteria = definition.criteria();
        Map<String, Row> unique = new HashMap<>();
        for (var entry : entries) {
            var species = entry.species();
            if (!criteria.acceptsSpecies(species.id())) continue;
            var forms = species.forms().stream().filter(criteria::acceptsForm).toList();
            if (criteria.filtersForms() && forms.isEmpty()) continue;
            boolean seen = criteria.filtersForms() ? forms.stream().anyMatch(ProgressCalculator.Form::seen) : species.seen();
            boolean owned = criteria.filtersForms() ? forms.stream().anyMatch(ProgressCalculator.Form::owned) : species.owned();
            boolean shinySeen = forms.stream().anyMatch(ProgressCalculator.Form::shinySeen);
            boolean matches = switch (request.filter()) {
                case ALL -> true;
                case SEEN -> seen;
                case OWNED -> owned;
                case UNSEEN -> !seen;
                case SHINY_SEEN -> shinySeen;
            };
            if (!matches || !(entry.name().toLowerCase(Locale.ROOT).contains(query) || species.id().toLowerCase(Locale.ROOT).contains(query))) continue;
            unique.put(species.id(), new Row(entry.name(), species.id(), forms, seen, owned, shinySeen));
        }
        Comparator<Row> order = request.order() == Order.NAME
                ? Comparator.comparing(Row::name, String.CASE_INSENSITIVE_ORDER).thenComparing(Row::id)
                : Comparator.comparing(Row::id);
        var sorted = unique.values().stream().sorted(order).toList();
        int pages = Math.max(1, (sorted.size() + size - 1) / size);
        int page = Math.max(0, Math.min(request.page(), pages - 1));
        int first = page * size;
        Optional<Row> detail = sorted.stream().filter(row -> row.id().equals(request.detail())).findFirst();
        return new Page(sorted.subList(first, Math.min(first + size, sorted.size())), sorted.size(), page, pages, detail);
    }
}
