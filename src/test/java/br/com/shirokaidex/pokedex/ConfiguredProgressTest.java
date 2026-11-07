package br.com.shirokaidex.pokedex;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguredProgressTest {
    private static CollectionDefinition collection(CollectionDefinition.Mode mode, CollectionDefinition.Criteria criteria) {
        return new CollectionDefinition("custom", "Custom", "", "minecraft:book", mode, criteria, "");
    }
    private static ProgressCalculator.CollectionProgress calculate(CollectionDefinition definition, ProgressCalculator.Species... entries) {
        return ProgressCalculator.calculate(List.of(entries), List.of(definition)).get("custom");
    }
    @Test void includeWhitelistAndExcludeWins() {
        var criteria = new CollectionDefinition.Criteria(Set.of("mod:a", "mod:b"), Set.of("mod:b"), Set.of(), Set.of(), Set.of());
        var result = calculate(collection(CollectionDefinition.Mode.OWNED, criteria),
                new ProgressCalculator.Species("mod:a", true, true, List.of()), new ProgressCalculator.Species("mod:b", true, true, List.of()),
                new ProgressCalculator.Species("mod:c", true, true, List.of()));
        assertEquals(1, result.progress().total()); assertTrue(result.progress().complete());
    }
    @Test void labelsAndTypesMustMatchTheSameForm() {
        var species = new ProgressCalculator.Species("mod:a", true, true, List.of(
                new ProgressCalculator.Form("normal", Set.of(), Set.of("electric"), true, true, false),
                new ProgressCalculator.Form("mega", Set.of("legendary"), Set.of("fire"), false, false, false)));
        var criteria = new CollectionDefinition.Criteria(Set.of(), Set.of(), Set.of("legendary"), Set.of("electric"), Set.of());
        assertEquals(0, calculate(collection(CollectionDefinition.Mode.OWNED, criteria), species).progress().total());
    }
    @Test void orWithinOneCriterionAndAndBetweenCriteria() {
        var form = new ProgressCalculator.Form("mega", Set.of("legendary"), Set.of("fire"), true, true, false);
        var species = new ProgressCalculator.Species("mod:a", true, true, List.of(form));
        var criteria = new CollectionDefinition.Criteria(Set.of(), Set.of(), Set.of("mythical", "legendary"), Set.of("fire", "water"), Set.of("mega"));
        assertTrue(calculate(collection(CollectionDefinition.Mode.OWNED, criteria), species).progress().complete());
    }
    @Test void ownedCommonFormDoesNotRegisterSeenRestrictedForm() {
        var species = new ProgressCalculator.Species("mod:a", true, true, List.of(
                new ProgressCalculator.Form("normal", Set.of(), Set.of(), true, true, false),
                new ProgressCalculator.Form("mega", Set.of(), Set.of(), true, false, false)));
        var criteria = new CollectionDefinition.Criteria(Set.of(), Set.of(), Set.of(), Set.of(), Set.of("mega"));
        assertEquals(0, calculate(collection(CollectionDefinition.Mode.OWNED, criteria), species).progress().registered().orElseThrow());
        assertTrue(calculate(collection(CollectionDefinition.Mode.SEEN, criteria), species).progress().complete());
    }
    @Test void shinySeenIsExplicitAndCannotBecomeCapturedEvidence() {
        var form = new ProgressCalculator.Form("normal", Set.of(), Set.of(), true, false, true);
        var species = new ProgressCalculator.Species("mod:a", true, false, List.of(form));
        var seen = calculate(collection(CollectionDefinition.Mode.SHINY_SEEN, CollectionDefinition.Criteria.all()), species);
        var captured = calculate(collection(CollectionDefinition.Mode.SHINY_CAPTURED, CollectionDefinition.Criteria.all()), species);
        assertTrue(seen.progress().complete()); assertEquals(1, seen.seen());
        assertTrue(captured.progress().registered().isEmpty()); assertEquals(1, captured.seen());
    }
    @Test void immutableDefinitionsCannotBeMutatedAfterValidation() {
        Set<String> ids = new HashSet<>(Set.of("mod:a"));
        var criteria = new CollectionDefinition.Criteria(ids, Set.of(), Set.of(), Set.of(), Set.of()); ids.clear();
        assertEquals(Set.of("mod:a"), criteria.includeSpecies());
        assertThrows(UnsupportedOperationException.class, () -> criteria.includeSpecies().clear());
    }
}
