package br.com.shirokaidex.rewards;

import br.com.shirokaidex.pokedex.DexProgress;

/** Evidence observed after installation; never inferred for past player activity. */
public record RewardUnlock(RewardPlan plan, int registered, int total) {
    public RewardUnlock {
        if(plan==null || !plan.unlocked(DexProgress.known(total,registered)))throw new IllegalArgumentException("Observed counts do not unlock this reward");
    }
}
