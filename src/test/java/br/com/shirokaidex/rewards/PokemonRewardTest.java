package br.com.shirokaidex.rewards;

import br.com.shirokaidex.config.ConfigManager;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PokemonRewardTest {
    private static String config(String pokemon){return "{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"rewards\":[{\"id\":\"partner\",\"collection\":\"national\",\"minimum_registered\":1,\"pokemon\":["+pokemon+"]}]}";}
    @Test void pokemonOnlyDefinitionsAndMixedPlansKeepExistingItemJsonStable()throws Exception{
        var plan=ConfigManager.parse(config("{\"species\":\"cobblemon:bulbasaur\"}")).rewards().getFirst();
        assertEquals(1,plan.steps());assertTrue(plan.items().isEmpty());assertFalse(plan.pokemon().getFirst().shiny());assertNull(plan.pokemon().getFirst().level());
        var itemOnly=new RewardPlan("old","national",1,null,List.of(new RewardPlan.Item("minecraft:paper",1)));
        assertFalse(itemOnly.json().contains("pokemon"));assertEquals(itemOnly,itemOnly.reuse(itemOnly.json()));
        var mixed=new RewardPlan("mixed","national",1,null,itemOnly.items(),false,plan.pokemon());assertEquals(2,mixed.steps());
    }
    @Test void frozenPayloadReuseIsExactAndNeverChangesWithNewRolls()throws Exception{
        var definition=ConfigManager.parse(config("{\"species\":\"cobblemon:bulbasaur\",\"ivs\":{\"hp\":31}} ")).rewards().getFirst();
        var prepared=definition.withPokemon(List.of(definition.pokemon().getFirst().withSnapshot("{UUID:[I;1,2,3,4]}")));
        assertEquals(prepared,definition.reuse(prepared.json()));assertEquals(definition,prepared.definition());
        assertThrows(IllegalArgumentException.class,()->definition.reuse(prepared.json().replace("bulbasaur","pikachu")));
        assertThrows(RuntimeException.class,()->definition.reuse(definition.json()));
    }
    @ParameterizedTest @ValueSource(strings={
        "{\"species\":\"bulbasaur\"}","{\"species\":\"cobblemon:bulbasaur\",\"species\":\"cobblemon:bulbasaur\"}",
        "{\"species\":\"cobblemon:bulbasaur\",\"level\":0}","{\"species\":\"cobblemon:bulbasaur\",\"level\":101}",
        "{\"species\":\"cobblemon:bulbasaur\",\"level\":1.5}","{\"species\":\"cobblemon:bulbasaur\",\"shiny\":\"true\"}",
        "{\"species\":\"cobblemon:bulbasaur\",\"gender\":\"unknown\"}","{\"species\":\"cobblemon:bulbasaur\",\"moves\":[\"tackle\",\"tackle\"]}",
        "{\"species\":\"cobblemon:bulbasaur\",\"ivs\":{\"hp\":32}}","{\"species\":\"cobblemon:bulbasaur\",\"evs\":{\"hp\":253}}",
        "{\"species\":\"cobblemon:bulbasaur\",\"evs\":{\"hp\":252,\"attack\":252,\"speed\":7}}",
        "{\"species\":\"cobblemon:bulbasaur\",\"ivs\":{\"accuracy\":2}}","{\"species\":\"cobblemon:bulbasaur\",\"ivs\":{\"hp\":1,\"hp\":2}}",
        "{\"species\":\"cobblemon:bulbasaur\",\"snapshot_snbt\":\"{}\"}","{\"species\":\"cobblemon:bulbasaur\",\"type\":\"COMMAND\"}"
    }) void invalidRequestsFailWholeCandidate(String pokemon){assertThrows(java.io.IOException.class,()->ConfigManager.parse(config(pokemon)));}
    @Test void mapsListsAndPayloadSizesAreBounded()throws Exception{
        var plan=ConfigManager.parse(config("{\"species\":\"cobblemon:bulbasaur\",\"evs\":{\"hp\":6,\"special_attack\":252,\"speed\":252},\"moves\":[\"tackle\"]}")).rewards().getFirst();
        var pokemon=plan.pokemon().getFirst();assertThrows(UnsupportedOperationException.class,()->pokemon.evs().clear());assertThrows(UnsupportedOperationException.class,()->pokemon.moves().clear());
        assertThrows(IllegalArgumentException.class,()->pokemon.withSnapshot("x".repeat(65537)));
        assertThrows(IllegalArgumentException.class,()->plan.withPokemon(Collections.nCopies(7,pokemon)));
    }
}
