package br.com.shirokaidex.pokedex;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ShinyCaptureProgressTest {
    private static ProgressCalculator.Species species(String id,String... forms){return new ProgressCalculator.Species(id,true,true,Arrays.stream(forms).map(f->new ProgressCalculator.Form(f,Set.of(),Set.of("electric"),true,true,true)).toList());}
    private static DexProgress shiny(List<ProgressCalculator.Species> entries,Set<CapturedShinyForm> captures){return ProgressCalculator.calculate(entries,captures).get(DexCollection.SHINY).progress();}
    @Test void unavailableDiffersFromKnownZero(){var entries=List.of(species("mod:a","normal"));assertTrue(shiny(entries,null).registered().isEmpty());assertEquals(0,shiny(entries,Set.of()).registered().orElseThrow());}
    @Test void seenOwnedAndSeenShinyNeverImplyCaptured(){var entries=List.of(species("mod:a","normal"));assertEquals(0,shiny(entries,Set.of()).registered().orElseThrow());assertEquals(1,ProgressCalculator.calculate(entries,Set.of()).get(DexCollection.NATIONAL).progress().registered().orElseThrow());}
    @Test void repeatedCapturesAndFormsCountOneSpecies(){var entries=List.of(species("mod:a","normal","alola"));assertEquals(1,shiny(entries,Set.of(new CapturedShinyForm("mod:a","normal"),new CapturedShinyForm("mod:a","ALOLA"))).registered().orElseThrow());}
    @Test void removedSpeciesAndFormsDoNotInflateCurrentDenominator(){var captured=Set.of(new CapturedShinyForm("mod:a","gone"),new CapturedShinyForm("mod:removed","normal"));var result=shiny(List.of(species("mod:a","normal"),species("mod:new","normal")),captured);assertEquals(2,result.total());assertEquals(0,result.registered().orElseThrow());}
    @Test void formFilterRequiresSameCapturedForm(){var definition=new CollectionDefinition("custom","Custom","","minecraft:book",CollectionDefinition.Mode.SHINY_CAPTURED,new CollectionDefinition.Criteria(Set.of(),Set.of(),Set.of(),Set.of(),Set.of("alola")),"");var entries=List.of(species("mod:a","normal","alola"));assertEquals(0,ProgressCalculator.calculate(entries,List.of(definition),Set.of(new CapturedShinyForm("mod:a","normal"))).get("custom").progress().registered().orElseThrow());assertEquals(1,ProgressCalculator.calculate(entries,List.of(definition),Set.of(new CapturedShinyForm("mod:a","alola"))).get("custom").progress().registered().orElseThrow());}
    @Test void registryExpansionChangesDenominatorWithoutInventingCaptures(){var captured=Set.of(new CapturedShinyForm("mod:a","normal"));assertEquals(1,shiny(List.of(species("mod:a","normal")),captured).total());var expanded=shiny(List.of(species("mod:a","normal"),species("mod:b","normal")),captured);assertEquals(2,expanded.total());assertEquals(1,expanded.registered().orElseThrow());}
}
