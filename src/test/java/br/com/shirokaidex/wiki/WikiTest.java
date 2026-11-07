package br.com.shirokaidex.wiki;

import br.com.shirokaidex.pokedex.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WikiTest {
    private static final CollectionDefinition ALL = CollectionDefinition.defaults().get(0);
    private static ProgressCalculator.Form form(String name, String type, boolean seen, boolean owned, boolean shiny) {
        return new ProgressCalculator.Form(name, Set.of(), Set.of(type), seen, owned, shiny);
    }
    private static Wiki.Entry entry(String id, String name, boolean seen, boolean owned, ProgressCalculator.Form... forms) {
        return new Wiki.Entry(name, new ProgressCalculator.Species(id, seen, owned, List.of(forms)));
    }
    private static final List<Wiki.Entry> DATA = List.of(
            entry("mod:zeta", "Alpha", true, true, form("normal", "fire", true, true, false)),
            entry("mod:alpha", "Zeta", true, false, form("normal", "water", true, false, true)),
            entry("other:alpha", "Alpha", false, false, form("normal", "normal", false, false, false)));
    private static Wiki.Page select(CollectionDefinition definition, String query, Wiki.Filter filter, Wiki.Order order, int page, int size) {
        return Wiki.select(DATA, definition, new Wiki.Request(query, filter, order, page, null), size);
    }
    @Test void nameOrderHasDeterministicIdentifierTiebreak() {
        assertEquals(List.of("mod:zeta", "other:alpha", "mod:alpha"), select(ALL,"", Wiki.Filter.ALL, Wiki.Order.NAME,0,18).rows().stream().map(Wiki.Row::id).toList());
    }
    @Test void identifierOrderAndPaginationAreBounded() {
        var page = select(ALL,"",Wiki.Filter.ALL,Wiki.Order.ID,Integer.MAX_VALUE,2);
        assertEquals(1,page.number()); assertEquals(2,page.pages()); assertEquals(1,page.rows().size()); assertEquals(3,page.total());
        assertEquals(0,select(ALL,"",Wiki.Filter.ALL,Wiki.Order.ID,-5,2).number());
    }
    @Test void searchFindsCanonicalNamesAndNamespacedIdentifiersWithoutCaseSensitivity() {
        assertEquals(2,select(ALL," ALPHA ",Wiki.Filter.ALL,Wiki.Order.NAME,0,18).rows().stream().filter(r->r.name().equals("Alpha")).count());
        assertEquals(1,select(ALL,"OTHER:ALPHA",Wiki.Filter.ALL,Wiki.Order.NAME,0,18).total());
        assertEquals(0,select(ALL,"<click:run_command>",Wiki.Filter.ALL,Wiki.Order.NAME,0,18).total());
    }
    @Test void filtersDistinguishSeenObtainedUnseenAndShinySeen() {
        assertEquals(2,select(ALL,"",Wiki.Filter.SEEN,Wiki.Order.NAME,0,18).total());
        assertEquals(1,select(ALL,"",Wiki.Filter.OWNED,Wiki.Order.NAME,0,18).total());
        assertEquals(1,select(ALL,"",Wiki.Filter.UNSEEN,Wiki.Order.NAME,0,18).total());
        var shiny=select(ALL,"",Wiki.Filter.SHINY_SEEN,Wiki.Order.NAME,0,18).rows().getFirst();
        assertTrue(shiny.shinySeen()); assertFalse(shiny.owned());
    }
    @Test void collectionExclusionAndEligibleFormsMatchProgressCalculator() {
        var criteria=new CollectionDefinition.Criteria(Set.of("mod:alpha","mod:zeta"),Set.of("mod:zeta"),Set.of(),Set.of("water"),Set.of("normal"));
        var collection=new CollectionDefinition("custom","Custom","","minecraft:book",CollectionDefinition.Mode.OWNED,criteria,"");
        var page=select(collection,"",Wiki.Filter.ALL,Wiki.Order.NAME,0,18);
        assertEquals(1,page.total()); assertEquals("mod:alpha",page.rows().getFirst().id());
        assertEquals(ProgressCalculator.calculate(DATA.stream().map(Wiki.Entry::species).toList(),List.of(collection)).get("custom").progress().total(),page.total());
    }
    @Test void unrelatedOwnedFormDoesNotMakeFilteredFormObtained() {
        var forms=List.of(form("normal","normal",true,true,false),form("regional","ice",false,false,false));
        var entries=List.of(new Wiki.Entry("Variant",new ProgressCalculator.Species("mod:variant",true,true,forms)));
        var collection=new CollectionDefinition("ice","Ice","","minecraft:book",CollectionDefinition.Mode.OWNED,
                new CollectionDefinition.Criteria(Set.of(),Set.of(),Set.of(),Set.of("ice"),Set.of()),"");
        var page=Wiki.select(entries,collection,Wiki.Request.initial(""),18);
        assertFalse(page.rows().getFirst().seen()); assertFalse(page.rows().getFirst().owned());
        assertEquals(List.of("regional"),page.rows().getFirst().forms().stream().map(ProgressCalculator.Form::name).toList());
    }
    @Test void unavailableShinyCollectionStillBrowsesSpeciesWithoutInventingCaptureStatus() {
        var page=select(CollectionDefinition.defaults().get(1),"",Wiki.Filter.ALL,Wiki.Order.NAME,0,18);
        assertEquals(3,page.total());
        assertTrue(ProgressCalculator.calculate(DATA.stream().map(Wiki.Entry::species).toList()).get(DexCollection.SHINY).progress().registered().isEmpty());
    }
    @Test void detailMustStillMatchCurrentCollectionQueryAndFilter() {
        assertTrue(Wiki.select(DATA,ALL,Wiki.Request.initial("").detail("mod:zeta"),18).detail().isPresent());
        assertTrue(Wiki.select(DATA,ALL,new Wiki.Request("other:",Wiki.Filter.ALL,Wiki.Order.NAME,0,"mod:zeta"),18).detail().isEmpty());
        assertTrue(Wiki.select(DATA,ALL,new Wiki.Request("",Wiki.Filter.UNSEEN,Wiki.Order.NAME,0,"mod:zeta"),18).detail().isEmpty());
    }
    @Test void emptyAndInvalidRequestsHaveExplicitResults() {
        var page=Wiki.select(List.of(),ALL,Wiki.Request.initial(""),18);
        assertEquals(0,page.total()); assertEquals(1,page.pages()); assertEquals(0,page.number());
        assertThrows(IllegalArgumentException.class,()->Wiki.select(DATA,ALL,Wiki.Request.initial("x".repeat(129)),18));
        assertThrows(IllegalArgumentException.class,()->Wiki.select(DATA,ALL,Wiki.Request.initial(""),0));
        assertThrows(UnsupportedOperationException.class,()->page.rows().add(null));
    }
}
