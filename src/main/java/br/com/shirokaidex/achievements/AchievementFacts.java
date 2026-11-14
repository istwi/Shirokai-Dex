package br.com.shirokaidex.achievements;

import br.com.shirokaidex.captures.CapturedForms;
import br.com.shirokaidex.profiles.ProfileProgress;
import java.util.*;

public record AchievementFacts(Map<String,Long> counts,Set<CapturedForms.Form> forms,Map<String,ProfileProgress.Proof> progress,Set<String> achievements) {
    public AchievementFacts {counts=Map.copyOf(counts);forms=Set.copyOf(forms);progress=Map.copyOf(progress);achievements=Set.copyOf(achievements);if(counts.values().stream().anyMatch(value->value==null||value<0))throw new IllegalArgumentException("Invalid achievement counters");for(var id:achievements)Requirement.identifier(id);for(var entry:progress.entrySet())if(!entry.getKey().equals(entry.getValue().collection()))throw new IllegalArgumentException("Invalid progress identity");}
    public long count(String metric,String key){return counts.getOrDefault(metric+"|"+key,0L);}
    public AchievementFacts withAchievements(Set<String> ids){return new AchievementFacts(counts,forms,progress,ids);}
}
