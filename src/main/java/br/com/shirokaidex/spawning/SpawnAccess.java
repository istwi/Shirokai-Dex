package br.com.shirokaidex.spawning;

import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.condition.SpawningCondition;
import com.cobblemon.mod.common.api.conditional.RegistryLikeIdentifierCondition;
import com.cobblemon.mod.common.api.conditional.RegistryLikeTagCondition;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Read only loaded simple Pokemon rules; do not evaluate conditions, create Pokemon, or reload data. */
public final class SpawnAccess {
    private SpawnAccess() { }
    public static SpawnCatalog capture(MinecraftServer server) {
        if(!server.isSameThread())throw new IllegalStateException("Spawn snapshot requires server thread");
        var pool=CobblemonSpawnPools.INSTANCE.getWORLD_SPAWN_POOL();
        if(pool==null)return SpawnCatalog.unavailable();
        List<SpawnCatalog.Rule> rules=new ArrayList<>();int skipped=0;
        for(var detail:pool.getDetails()) {
            if(!(detail instanceof PokemonSpawnDetail pokemon)){skipped++;continue;}
            var properties=pokemon.getPokemon();
            String id=SpawnCatalog.speciesId(properties.getSpecies());
            var species=id==null?null:PokemonSpecies.getByIdentifier(ResourceLocation.parse(id));
            if(species==null){skipped++;continue;}
            List<SpawnCatalog.Group> groups=new ArrayList<>();
            for(var condition:detail.getConditions())groups.add(group(SpawnCatalog.Kind.ALLOW,condition));
            for(var condition:detail.getAnticonditions())groups.add(group(SpawnCatalog.Kind.DENY,condition));
            rules.add(new SpawnCatalog.Rule(detail.getId(),species.getResourceIdentifier().toString(),
                    properties.getForm()==null?"":properties.getForm(),groups,detail.getCompositeCondition()!=null));
        }
        return SpawnCatalog.known(rules,skipped);
    }
    private static SpawnCatalog.Group group(SpawnCatalog.Kind kind,SpawningCondition<?> condition) {
        List<SpawnCatalog.Value> values=new ArrayList<>();
        if(condition.getBiomes()!=null) {
            List<String> selectors=new ArrayList<>();
            for(var biome:condition.getBiomes()) {
                if(biome instanceof RegistryLikeIdentifierCondition<?> id)selectors.add(id.getIdentifier().toString());
                else if(biome instanceof RegistryLikeTagCondition<?> tag)selectors.add("#"+tag.getTag().location());
                // Custom selector kinds are not guessed; every card is explicitly a partial summary.
            }
            selectors.stream().sorted().forEach(value->values.add(new SpawnCatalog.Value(SpawnCatalog.Field.BIOME,value)));
        }
        if(condition.getDimensions()!=null)condition.getDimensions().stream().map(Object::toString).sorted()
                .forEach(value->values.add(new SpawnCatalog.Value(SpawnCatalog.Field.DIMENSION,value)));
        if(condition.getTimeRange()!=null)for(var range:condition.getTimeRange().getRanges())
            values.add(new SpawnCatalog.Value(SpawnCatalog.Field.TICKS,range.getFirst()+".."+range.getLast()));
        return new SpawnCatalog.Group(kind,values);
    }
}
