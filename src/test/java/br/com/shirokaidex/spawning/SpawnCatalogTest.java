package br.com.shirokaidex.spawning;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class SpawnCatalogTest {
    private static SpawnCatalog.Rule rule(String id,String species,List<SpawnCatalog.Group> groups) {
        return new SpawnCatalog.Rule(id,species,"",groups,false);
    }
    @Test void speciesIdentifiersPreserveNamespaceAndRejectMalformedValues() {
        assertEquals("cobblemon:pikachu",SpawnCatalog.speciesId("pikachu"));
        assertEquals("dex_test:ordinary",SpawnCatalog.speciesId("dex_test:ordinary"));
        for(String raw:List.of("", "a:b:c", "../bad name", "random=true"))assertNull(SpawnCatalog.speciesId(raw));
        assertNull(SpawnCatalog.speciesId(null));
    }
    @Test void unavailableIsDistinctFromKnownPoolWithoutSupportedRules() {
        assertFalse(SpawnCatalog.unavailable().available());
        var empty=SpawnCatalog.known(List.of(),3);assertTrue(empty.available());assertEquals(3,empty.skipped());assertTrue(empty.cards("test:none").isEmpty());
    }
    @Test void namespaceAndRulesAreNotInferredFromTheRuleId() {
        var catalog=SpawnCatalog.known(List.of(rule("arbitrary","one:species",List.of()),rule("species-2","two:species",List.of())),0);
        assertEquals("arbitrary",catalog.cards("one:species").getFirst().rule());assertEquals(1,catalog.cards("two:species").size());assertTrue(catalog.cards("cobblemon:species").isEmpty());
    }
    @Test void positiveAndExcludedGroupsStaySeparateWithoutFlatteningConditions() {
        var a=new SpawnCatalog.Value(SpawnCatalog.Field.BIOME,"#test:forests");
        var b=new SpawnCatalog.Value(SpawnCatalog.Field.DIMENSION,"minecraft:the_nether");
        var groups=List.of(new SpawnCatalog.Group(SpawnCatalog.Kind.ALLOW,List.of(a)),new SpawnCatalog.Group(SpawnCatalog.Kind.DENY,List.of(b)));
        var cards=SpawnCatalog.known(List.of(rule("r","test:a",groups)),0).cards("test:a");
        assertEquals(3,cards.size());assertEquals(SpawnCatalog.Kind.ALLOW,cards.get(1).kind());assertEquals(List.of(a),cards.get(1).values());assertEquals(SpawnCatalog.Kind.DENY,cards.get(2).kind());assertEquals(List.of(b),cards.get(2).values());
    }
    @Test void longGroupsAreSplitWithoutDroppingSelectorsOrCombiningThemWithOtherGroups() {
        var values=IntStream.range(0,35).mapToObj(i->new SpawnCatalog.Value(SpawnCatalog.Field.BIOME,"test:biome_"+i)).toList();
        var cards=SpawnCatalog.known(List.of(rule("r","test:a",List.of(new SpawnCatalog.Group(SpawnCatalog.Kind.ALLOW,values)))),0).cards("test:a");
        assertEquals(5,cards.size());assertEquals(values,cards.stream().skip(1).flatMap(c->c.values().stream()).toList());assertTrue(cards.stream().allMatch(c->c.values().size()<=10));assertEquals(4,cards.getLast().part());
    }
    @Test void declaredFormIsPreservedAndNeverFilledWithAnInventedDefault() {
        var cards=SpawnCatalog.known(List.of(new SpawnCatalog.Rule("r","test:a","regional",List.of(),true),rule("s","test:a",List.of())),0).cards("test:a");
        assertEquals("regional",cards.getFirst().declaredForm());assertTrue(cards.getFirst().composite());assertEquals("",cards.getLast().declaredForm());
    }
    @Test void snapshotsAreImmutableAndDeterministic() {
        var source=new ArrayList<SpawnCatalog.Rule>();source.add(rule("z","test:a",List.of()));source.add(rule("a","test:a",List.of()));
        var catalog=SpawnCatalog.known(source,0);source.clear();assertEquals(List.of("a","z"),catalog.cards("test:a").stream().map(SpawnCatalog.Card::rule).toList());assertThrows(UnsupportedOperationException.class,()->catalog.cards("test:a").clear());
    }
}
