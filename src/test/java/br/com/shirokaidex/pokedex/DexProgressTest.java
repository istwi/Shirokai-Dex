package br.com.shirokaidex.pokedex;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DexProgressTest {
    private static ProgressCalculator.Species species(String id, boolean seen, boolean owned, ProgressCalculator.Form... forms) {
        return new ProgressCalculator.Species(id, seen, owned, List.of(forms));
    }
    @Test void percentageIsRegisteredDividedByCurrentTotal() {
        assertEquals(new BigDecimal("50.00"), DexProgress.known(10, 5).percentage().orElseThrow());
        assertEquals("50,00%", DexProgress.known(10, 5).formattedPercentage("pt_br").orElseThrow());
        assertEquals("50.00%", DexProgress.known(10, 5).formattedPercentage("en_us").orElseThrow());
    }
    @Test void roundsDownAndNeverAdvertisesCompletionEarly() {
        var progress = DexProgress.known(100001, 100000);
        assertEquals("99.99%", progress.formattedPercentage("en_us").orElseThrow());
        assertFalse(progress.complete());
        assertEquals("33.33%", DexProgress.known(3, 1).formattedPercentage("en_us").orElseThrow());
    }
    @Test void knownZeroAndCompleteAreDistinct() {
        assertEquals("0.00%", DexProgress.known(3, 0).formattedPercentage("en_us").orElseThrow());
        assertFalse(DexProgress.known(3, 0).complete());
        assertTrue(DexProgress.known(3, 3).complete());
        assertEquals("100.00%", DexProgress.known(3, 3).formattedPercentage("en_us").orElseThrow());
    }
    @Test void emptyRegistryIsUnavailableRatherThanFreeCompletion() {
        var empty = DexProgress.known(0, 0);
        assertTrue(empty.percentage().isEmpty()); assertFalse(empty.complete());
    }
    @Test void unknownCaptureEvidenceDoesNotBecomeZeroPercent() {
        var unavailable = DexProgress.unavailable(905);
        assertTrue(unavailable.registered().isEmpty()); assertTrue(unavailable.percentage().isEmpty());
    }
    @Test void rejectsInconsistentCounts() {
        assertThrows(IllegalArgumentException.class, () -> DexProgress.known(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> DexProgress.known(1, 2));
        assertThrows(IllegalArgumentException.class, () -> DexProgress.known(1, -1));
    }
    @Test void denominatorChangesWithCatalogWithoutCountingRemovedSpecies() {
        var owned = species("extra:species", true, true);
        var unseen = species("other:species", false, false);
        var before = ProgressCalculator.calculate(List.of(owned, unseen)).get(DexCollection.NATIONAL).progress();
        var after = ProgressCalculator.calculate(List.of(owned)).get(DexCollection.NATIONAL).progress();
        assertEquals("50.00%", before.formattedPercentage("en_us").orElseThrow());
        assertEquals("100.00%", after.formattedPercentage("en_us").orElseThrow());
    }
    @Test void duplicateEntriesAndMultipleFormsCountAsOneSpecies() {
        var entry = species("mod:one", true, true,
                new ProgressCalculator.Form(false, true, true), new ProgressCalculator.Form(false, true, true));
        var progress = ProgressCalculator.calculate(List.of(entry, entry)).get(DexCollection.NATIONAL).progress();
        assertEquals(1, progress.total()); assertEquals(1, progress.registered().orElseThrow());
    }
    @Test void legendaryOwnershipMustBeFromAnEligibleForm() {
        var entry = species("mod:mixed", true, true,
                new ProgressCalculator.Form(false, true, true), new ProgressCalculator.Form(true, true, false));
        var progress = ProgressCalculator.calculate(List.of(entry));
        assertEquals(1, progress.get(DexCollection.NATIONAL).progress().registered().orElseThrow());
        assertEquals(1, progress.get(DexCollection.LEGENDARY).progress().total());
        assertEquals(0, progress.get(DexCollection.LEGENDARY).progress().registered().orElseThrow());
        assertEquals(1, progress.get(DexCollection.LEGENDARY).seen());
    }
    @Test void legendaryOwnedFormContributesOnlyOnceAndShinyRemainsUnknown() {
        var form = new ProgressCalculator.Form(true, true, true);
        var values = ProgressCalculator.calculate(List.of(species("mod:legend", true, true, form, form)));
        assertTrue(values.get(DexCollection.LEGENDARY).progress().complete());
        assertTrue(values.get(DexCollection.SHINY).progress().registered().isEmpty());
    }
    @Test void nationalRetainsOfficialAggregateEvenIfOldFormIsNotInCurrentRegistry() {
        var values = ProgressCalculator.calculate(List.of(species("mod:known", true, true)));
        assertEquals(1, values.get(DexCollection.NATIONAL).progress().registered().orElseThrow());
        assertEquals(0, values.get(DexCollection.LEGENDARY).progress().total());
    }
    @Test void emptyCollectionsRemainUnavailable() {
        var values = ProgressCalculator.calculate(List.of());
        for (var value : values.values()) assertTrue(value.progress().percentage().isEmpty());
    }
}
