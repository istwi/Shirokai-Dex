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
    public enum Facet { NONE, TYPE, CATEGORY }
    public record Request(String query, Filter filter, Order order, int page, String detail,
                          String type, String category, int formPage, Facet facet, int optionPage, int spawnPage) {
        public Request(String query, Filter filter, Order order, int page, String detail) {
            this(query, filter, order, page, detail, "", "", -1, Facet.NONE, 0, -1);
        }
        public static Request initial(String query) { return new Request(query, Filter.ALL, Order.NAME, 0, null); }
        public Request page(int value) { return new Request(query, filter, order, value, null, type, category, -1, Facet.NONE, 0, -1); }
        public Request detail(String id) { return new Request(query, filter, order, page, id, type, category, -1, Facet.NONE, 0, -1); }
        public Request forms(int value) { return new Request(query, filter, order, page, detail, type, category, value, Facet.NONE, 0, -1); }
        public Request filtered(Filter value) { return new Request(query, value, order, 0, null, type, category, -1, Facet.NONE, 0, -1); }
        public Request ordered(Order value) { return new Request(query, filter, value, 0, null, type, category, -1, Facet.NONE, 0, -1); }
        public Request type(String value) { return new Request(query, filter, order, 0, null, value, category, -1, Facet.NONE, 0, -1); }
        public Request category(String value) { return new Request(query, filter, order, 0, null, type, value, -1, Facet.NONE, 0, -1); }
        public Request spawns(int value) { return new Request(query, filter, order, page, detail, type, category, -1, Facet.NONE, 0, value); }
        public Request pick(Facet value, int option) { return new Request(query, filter, order, page, null, type, category, -1, value, option, -1); }
    }
    public record Page(List<Row> rows, int total, int number, int pages, Optional<Row> detail,
                       List<String> types, List<String> categories) {
        public Page { rows = List.copyOf(rows); types = List.copyOf(types); categories = List.copyOf(categories); }
    }
    public record Slice<T>(List<T> items, int total, int number, int pages) {
        public Slice { items = List.copyOf(items); }
    }
    public static <T> Slice<T> paginate(List<T> items, int requestedPage, int size) {
        if (size < 1 || size > 54) throw new IllegalArgumentException("Invalid page size");
        int pages = Math.max(1, (items.size() + size - 1) / size);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        int first = page * size;
        return new Slice<>(items.subList(first, Math.min(first + size, items.size())), items.size(), page, pages);
    }
    public static Slice<ProgressCalculator.Form> forms(Row row, int page, int size) {
        return paginate(row.forms().stream().sorted(Comparator.comparing(ProgressCalculator.Form::name)).toList(), page, size);
    }
    public static Slice<String> options(List<String> values, int page, int size) {
        List<String> all = new ArrayList<>(); all.add("");
        values.stream().filter(value -> !value.isEmpty()).distinct().sorted().forEach(all::add);
        return paginate(all, page, size);
    }
    private Wiki() { }
    public static Page select(List<Entry> entries, CollectionDefinition definition, Request request, int size) {
        if (size < 1 || size > 54 || request.query().length() > 128) throw new IllegalArgumentException("Invalid wiki query/page size");
        String query = request.query().strip().toLowerCase(Locale.ROOT);
        var criteria = definition.criteria();
        Map<String, Row> unique = new HashMap<>();
        Set<String> types = new TreeSet<>(), categories = new TreeSet<>();
        for (var entry : entries) {
            var species = entry.species();
            if (!criteria.acceptsSpecies(species.id())) continue;
            var collectionForms = species.forms().stream().filter(form -> definition.acceptsForm(species.id(), form)).toList();
            if (definition.filtersForms() && collectionForms.isEmpty()) continue;
            collectionForms.forEach(form -> { types.addAll(form.types()); categories.addAll(form.labels()); });
            // Secondary facets must match the same form, together with the collection criteria.
            var forms = collectionForms.stream().filter(form -> (request.type().isEmpty() || form.types().contains(request.type()))
                    && (request.category().isEmpty() || form.labels().contains(request.category()))).toList();
            boolean scoped = definition.filtersForms() || !request.type().isEmpty() || !request.category().isEmpty();
            if (scoped && forms.isEmpty()) continue;
            boolean seen = scoped ? forms.stream().anyMatch(ProgressCalculator.Form::seen) : species.seen();
            boolean owned = scoped ? forms.stream().anyMatch(ProgressCalculator.Form::owned) : species.owned();
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
        var slice = paginate(sorted, request.page(), size);
        Optional<Row> detail = sorted.stream().filter(row -> row.id().equals(request.detail())).findFirst();
        return new Page(slice.items(), slice.total(), slice.number(), slice.pages(), detail, List.copyOf(types), List.copyOf(categories));
    }
}
