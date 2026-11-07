package br.com.shirokaidex.categories;

import br.com.shirokaidex.config.CategoriesCodec;
import br.com.shirokaidex.config.CollectionValidator;
import br.com.shirokaidex.pokedex.ProgressCalculator;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CategoryCatalogTest {
    private static ProgressCalculator.Form form(String name, String type, String... labels) {
        return new ProgressCalculator.Form(name, Set.of(labels), Set.of(type), false, false, false);
    }
    @Test void compositionUsesSameFormAndExclusionsWin() throws Exception {
        var categories = CategoriesCodec.parse("""
            [
              {"id":"rare","criteria":{"labels":["legendary"]}},
              {"id":"fire","criteria":{"types":["fire"]}},
              {"id":"banned","criteria":{"include_species":["test:banned"]}},
              {"id":"target","all_of":["rare","fire"],"none_of":["banned"]}
            ]
            """);
        assertTrue(categories.matches("target", "test:one", form("normal", "fire", "legendary")));
        assertFalse(categories.matches("target", "test:banned", form("normal", "fire", "legendary")));
        var split = new ProgressCalculator.Species("test:split", false, false,
                List.of(form("normal", "water", "legendary"), form("alternate", "fire")));
        assertTrue(categories.species("target", List.of(split)).isEmpty());
    }
    @Test void unionCountsSpeciesOnceWithoutChangingProgress() throws Exception {
        var categories = CategoriesCodec.parse("""
            [{"id":"fire","criteria":{"types":["fire"]}},
             {"id":"rare","criteria":{"labels":["legendary"]}},
             {"id":"either","any_of":["fire","rare"]}]
            """);
        var species = new ProgressCalculator.Species("test:one", false, false,
                List.of(form("normal", "fire", "legendary"), form("alternate", "fire")));
        assertEquals(Set.of("test:one"), categories.species("either", List.of(species, species)));
        assertFalse(species.owned()); assertFalse(species.seen());
        assertThrows(UnsupportedOperationException.class, () -> categories.definitions().clear());
        assertThrows(UnsupportedOperationException.class, () -> categories.species("either", List.of(species)).clear());
    }
    @Test void forwardReferencesAndSharedDependencyAreValid() throws Exception {
        var catalog = CategoriesCodec.parse("""
            [{"id":"root","all_of":["a","b"]}, {"id":"a","all_of":["leaf"]},
             {"id":"b","any_of":["leaf"]}, {"id":"leaf"}]
            """);
        assertTrue(catalog.matches("root", "test:one", form("normal", "fire")));
        assertThrows(IllegalArgumentException.class, () -> catalog.species("missing", List.of()));
    }
    @ParameterizedTest @ValueSource(strings = {
        "[{\"id\":\"a\",\"all_of\":[\"a\"]}]",
        "[{\"id\":\"a\",\"all_of\":[\"b\"]},{\"id\":\"b\",\"none_of\":[\"a\"]}]",
        "[{\"id\":\"a\",\"any_of\":[\"missing\"]}]",
        "[{\"id\":\"a\"},{\"id\":\"a\"}]",
        "[{\"id\":\"a\",\"id\":\"b\"}]",
        "[{\"id\":\"a\",\"all_of\":[\"b\",\"b\"]}]",
        "[{\"id\":\"a\",\"criteria\":{\"types\":[5]}}]",
        "[{\"id\":\"a\",\"permission\":\"arbitrary\"}]",
        "[{\"id\":\"A\"}]", "[{\"id\":true}]", "[{}]", "[] []", "null",
        "[{\"id\":\"a\",\"criteria\":{\"include_species\":[\"pikachu\"]}}]"
    }) void rejectsAmbiguousOrBrokenGraph(String json) {
        assertThrows(IOException.class, () -> CategoriesCodec.parse(json));
    }
    @Test void validatesRealRegistryAndDoesNotInferParadoxClassification() throws Exception {
        var normal = new ProgressCalculator.Species("test:one", false, false, List.of(form("normal", "fire")));
        var validator = new CollectionValidator(List.of(normal), Set.of());
        var explicit = CategoriesCodec.parse("[{\"id\":\"paradox\",\"criteria\":{\"include_species\":[\"test:one\"]}}]");
        validator.validate(explicit);
        assertEquals(Set.of("test:one"), explicit.species("paradox", List.of(normal)));
        for (String field : List.of("include_species", "exclude_species", "labels", "types", "forms")) {
            String value = field.endsWith("species") ? "test:missing" : "unknown";
            var invalid = CategoriesCodec.parse("[{\"id\":\"a\",\"criteria\":{\"" + field + "\":[\"" + value + "\"]}}]");
            assertThrows(IOException.class, () -> validator.validate(invalid));
        }
    }
    @Test void emptyCatalogAndBoundedInput() throws Exception {
        assertTrue(CategoriesCodec.parse("[]").definitions().isEmpty());
        assertThrows(IOException.class, () -> CategoriesCodec.parse(" ".repeat(65537)));
        String entries = java.util.stream.IntStream.range(0, 65).mapToObj(i -> "{\"id\":\"a" + i + "\"}")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        assertThrows(IOException.class, () -> CategoriesCodec.parse(entries));
    }
}
