package br.com.shirokaidex.cobblemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import net.minecraft.server.level.ServerPlayer;

/** Read-only access to the real registry and official records. Invoke on the server thread. */
public final class CobblemonAccess {
    public record Progress(int total, int seen, int owned) { }
    public Progress progress(ServerPlayer player) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Pokédex access requires the server thread");
        var dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
        int total = 0, seen = 0, owned = 0;
        for (var species : PokemonSpecies.INSTANCE.getImplemented()) {
            total++;
            var record = dex.getSpeciesRecord(species.getResourceIdentifier());
            if (record == null) continue;
            if (record.hasAtLeast(PokedexEntryProgress.SEEN)) seen++;
            if (record.hasAtLeast(PokedexEntryProgress.OWNED)) owned++;
        }
        return new Progress(total, seen, owned);
    }
}
