package br.com.shirokaidex.rewards;

import java.math.BigDecimal;
import java.util.*;

/** Exact decimal amount; identity is never replaced with the provider's primary currency. */
public record CurrencyReward(String currency,BigDecimal amount) {
    public CurrencyReward {
        br.com.shirokaidex.pokedex.CollectionDefinition.identifier(currency);
        Objects.requireNonNull(amount);amount=amount.stripTrailingZeros();
        if(amount.signum()<=0||amount.scale()>8||amount.compareTo(new BigDecimal("1000000000000000"))>0)throw new IllegalArgumentException("amount must be positive, <=10^15, maximum eight decimal places");
    }
}
