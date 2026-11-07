package br.com.shirokaidex.pokedex;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.OptionalInt;

/** Unknown evidence is distinct from known zero; an empty registry is not completion. */
public record DexProgress(int total, OptionalInt registered) {
    public DexProgress {
        if (total < 0 || registered == null || (registered.isPresent() &&
                (registered.getAsInt() < 0 || registered.getAsInt() > total))) {
            throw new IllegalArgumentException("Inconsistent Pokédex counts");
        }
    }
    public static DexProgress known(int total, int registered) { return new DexProgress(total, OptionalInt.of(registered)); }
    public static DexProgress unavailable(int total) { return new DexProgress(total, OptionalInt.empty()); }
    public Optional<BigDecimal> percentage() {
        if (total == 0 || registered.isEmpty()) return Optional.empty();
        // Never display 100% for a collection that has not actually completed.
        return Optional.of(BigDecimal.valueOf(registered.getAsInt()).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.DOWN));
    }
    public Optional<String> formattedPercentage(String language) {
        return percentage().map(value -> (language.equals("pt_br") ? value.toPlainString().replace('.', ',') : value.toPlainString()) + "%");
    }
    public boolean complete() { return total > 0 && registered.isPresent() && registered.getAsInt() == total; }
}
