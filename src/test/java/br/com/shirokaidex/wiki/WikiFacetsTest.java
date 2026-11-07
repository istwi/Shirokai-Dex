package br.com.shirokaidex.wiki;

import br.com.shirokaidex.pokedex.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class WikiFacetsTest {
    private static final CollectionDefinition ALL=CollectionDefinition.defaults().getFirst();
    private static final List<Wiki.Entry> ENTRIES=List.of(new Wiki.Entry("Mixed",
            new ProgressCalculator.Species("test:mixed",true,true,List.of(
                    new ProgressCalculator.Form("normal",Set.of("legendary"),Set.of("fire"),true,true,false),
                    new ProgressCalculator.Form("regional",Set.of("mythical"),Set.of("water"),false,false,false)))));
    @Test void typeAndCategoryMustMatchTheSameEligibleForm() {
        var invalid=Wiki.select(ENTRIES,ALL,Wiki.Request.initial("").type("water").category("legendary"),18);
        assertEquals(0,invalid.total());
        var valid=Wiki.select(ENTRIES,ALL,Wiki.Request.initial("").type("water").category("mythical"),18);
        assertEquals(1,valid.total());assertEquals(List.of("regional"),valid.rows().getFirst().forms().stream().map(ProgressCalculator.Form::name).toList());
        assertFalse(valid.rows().getFirst().seen());assertFalse(valid.rows().getFirst().owned());
    }
    @Test void stateFilterUsesTheSelectedFormRatherThanGlobalSpeciesKnowledge() {
        var selected=Wiki.Request.initial("").type("water");
        assertEquals(0,Wiki.select(ENTRIES,ALL,selected.filtered(Wiki.Filter.OWNED),18).total());
        assertEquals(1,Wiki.select(ENTRIES,ALL,selected.filtered(Wiki.Filter.UNSEEN),18).total());
        assertEquals(1,Wiki.select(ENTRIES,ALL,Wiki.Request.initial(""),18).rows().stream().filter(Wiki.Row::owned).count());
    }
    @Test void collectionCriteriaCannotBeBypassedBySecondaryFacets() {
        var restricted=new CollectionDefinition("restricted","Restricted","","minecraft:book",CollectionDefinition.Mode.OWNED,
                new CollectionDefinition.Criteria(Set.of(),Set.of(),Set.of(),Set.of("fire"),Set.of()),"");
        var page=Wiki.select(ENTRIES,restricted,Wiki.Request.initial("").type("water"),18);
        assertEquals(0,page.total());assertEquals(List.of("fire"),page.types());assertEquals(List.of("legendary"),page.categories());
    }
    @Test void optionsAreDynamicStableAndIndependentOfSearchAndState() {
        var page=Wiki.select(ENTRIES,ALL,Wiki.Request.initial("missing").filtered(Wiki.Filter.SHINY_SEEN),18);
        assertEquals(0,page.total());assertEquals(List.of("fire","water"),page.types());assertEquals(List.of("legendary","mythical"),page.categories());
        assertEquals(List.of("","fire","water"),Wiki.options(page.types(),0,18).items());
        assertThrows(UnsupportedOperationException.class,()->page.types().clear());
    }
    @Test void everyFormBeyondThePreviousLimitIsReachableInDeterministicPages() {
        var forms=IntStream.range(0,100).mapToObj(i->new ProgressCalculator.Form(String.format(Locale.ROOT,"variant_%03d",i),Set.of(),Set.of("water"),false,false,false)).toList();
        var shuffled=new ArrayList<>(forms);Collections.reverse(shuffled);
        var row=new Wiki.Row("Many","test:many",shuffled,false,false,false);
        List<ProgressCalculator.Form> visited=new ArrayList<>();
        for(int i=0;i<6;i++){var page=Wiki.forms(row,i,18);assertEquals(6,page.pages());assertTrue(page.items().size()<=18);visited.addAll(page.items());}
        assertEquals(forms,visited);assertEquals(100,new HashSet<>(visited).size());
        assertEquals(5,Wiki.forms(row,Integer.MAX_VALUE,18).number());assertEquals(0,Wiki.forms(row,-99,18).number());
    }
    @Test void optionsHandleMoreThanOnePageAndStalePagesWithoutDroppingChoices() {
        var values=IntStream.range(0,40).mapToObj(i->String.format(Locale.ROOT,"category_%02d",i)).toList();
        List<String> all=new ArrayList<>();for(int i=0;i<3;i++)all.addAll(Wiki.options(values,i,18).items());
        assertEquals(41,all.size());assertEquals("",all.getFirst());assertEquals(values,all.subList(1,all.size()));
        assertEquals(0,Wiki.options(List.of("water"),999,18).number());
    }
    @Test void navigationPreservesQueryAndFacetsAndResetsOnlyTheRelevantPage() {
        var request=Wiki.Request.initial("Mixed").type("water").category("mythical").filtered(Wiki.Filter.UNSEEN).ordered(Wiki.Order.ID).page(4);
        var picker=request.pick(Wiki.Facet.TYPE,2);assertEquals(4,picker.page());assertEquals("mythical",picker.category());
        var detail=request.detail("test:mixed").forms(3);assertEquals(4,detail.page());assertEquals(3,detail.formPage());assertEquals("water",detail.type());
        assertEquals(-1,detail.detail("test:mixed").formPage());
        assertEquals(0,request.type("fire").page());assertEquals("Mixed",request.type("fire").query());
        assertEquals("mythical",request.ordered(Wiki.Order.NAME).category());
    }
    @Test void detailsDisappearWhenReloadMakesTheSelectedFormIneligible() {
        var request=Wiki.Request.initial("").type("water").detail("test:mixed").forms(2);
        assertTrue(Wiki.select(ENTRIES,ALL,request,18).detail().isPresent());
        var newEntries=List.of(new Wiki.Entry("Mixed",new ProgressCalculator.Species("test:mixed",true,true,List.of(ENTRIES.getFirst().species().forms().getFirst()))));
        var updated=Wiki.select(newEntries,ALL,request,18);assertEquals(0,updated.total());assertTrue(updated.detail().isEmpty());
    }
}
