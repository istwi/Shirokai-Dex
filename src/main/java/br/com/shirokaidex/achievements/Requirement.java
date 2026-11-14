package br.com.shirokaidex.achievements;

import br.com.shirokaidex.captures.CapturedForms;
import br.com.shirokaidex.rankings.CaptureRanking;
import java.math.BigDecimal;
import java.util.*;

public sealed interface Requirement permits Requirement.All,Requirement.Any,Requirement.Count,Requirement.Progress,Requirement.Form,Requirement.Achieved {
    enum Result { MATCH, NO_MATCH, UNKNOWN }
    Result evaluate(AchievementFacts facts,AchievementFacts baseline);
    default boolean baselineReady(AchievementFacts facts){return true;}
    default Set<String> dependencies(){return Set.of();}
    record All(List<Requirement> conditions) implements Requirement {
        public All{conditions=List.copyOf(conditions);if(conditions.isEmpty()||conditions.size()>32)throw new IllegalArgumentException("all requires 1..32 conditions");}
        public Result evaluate(AchievementFacts f,AchievementFacts b){boolean unknown=false;for(var condition:conditions){var value=condition.evaluate(f,b);if(value==Result.NO_MATCH)return value;unknown|=value==Result.UNKNOWN;}return unknown?Result.UNKNOWN:Result.MATCH;}
        public boolean baselineReady(AchievementFacts facts){return conditions.stream().allMatch(c->c.baselineReady(facts));}
        public Set<String> dependencies(){return collect(conditions);}
    }
    record Any(List<Requirement> conditions) implements Requirement {
        public Any{conditions=List.copyOf(conditions);if(conditions.isEmpty()||conditions.size()>32)throw new IllegalArgumentException("any requires 1..32 conditions");}
        public Result evaluate(AchievementFacts f,AchievementFacts b){boolean unknown=false;for(var condition:conditions){var value=condition.evaluate(f,b);if(value==Result.MATCH)return value;unknown|=value==Result.UNKNOWN;}return unknown?Result.UNKNOWN:Result.NO_MATCH;}
        public boolean baselineReady(AchievementFacts facts){return conditions.stream().allMatch(c->c.baselineReady(facts));}
        public Set<String> dependencies(){return collect(conditions);}
    }
    record Count(CaptureRanking.Metric metric,String key,long minimum) implements Requirement {
        public Count{new CaptureRanking("criterion","Criterion",metric,key,"");if(metric==CaptureRanking.Metric.COLLECTION)throw new IllegalArgumentException("Use POKEDEX achievement conditions");if(minimum<1||minimum>1_000_000_000L)throw new IllegalArgumentException("Invalid capture achievement threshold");}
        public Result evaluate(AchievementFacts f,AchievementFacts b){long count=f.count(metric.name(),key)-(b==null?0:b.count(metric.name(),key));return count>=minimum?Result.MATCH:Result.NO_MATCH;}
    }
    record Progress(String collection,Integer minimumRegistered,BigDecimal minimumPercent) implements Requirement {
        public Progress{identifier(collection);if((minimumRegistered==null)==(minimumPercent==null)||minimumRegistered!=null&&minimumRegistered<1||minimumPercent!=null&&(minimumPercent.signum()<=0||minimumPercent.compareTo(new BigDecimal("100"))>0||minimumPercent.scale()>2))throw new IllegalArgumentException("Invalid Pokedex achievement threshold");}
        public boolean baselineReady(AchievementFacts facts){var value=facts.progress().get(collection);return value!=null&&value.registered()!=null&&value.total()>0;}
        public Result evaluate(AchievementFacts facts,AchievementFacts baseline){
            var proof=facts.progress().get(collection);if(proof==null||proof.registered()==null||proof.total()==0)return Result.UNKNOWN;int registered=proof.registered();
            if(baseline!=null){var previous=baseline.progress().get(collection);if(previous==null||previous.registered()==null||!previous.version().equals(proof.version()))return Result.UNKNOWN;registered=Math.max(0,registered-previous.registered());}
            boolean matches=minimumRegistered!=null?registered>=minimumRegistered:BigDecimal.valueOf(registered).multiply(BigDecimal.valueOf(100)).compareTo(minimumPercent.multiply(BigDecimal.valueOf(proof.total())))>=0;return matches?Result.MATCH:Result.NO_MATCH;
        }
    }
    record Form(String species,String form) implements Requirement {
        public Form{var value=new CapturedForms.Form(species,form);form=value.form();if(form.isBlank()||form.length()>128||form.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid achievement form");}
        public Result evaluate(AchievementFacts facts,AchievementFacts baseline){var value=new CapturedForms.Form(species,form);return facts.forms().contains(value)&&(baseline==null||!baseline.forms().contains(value))?Result.MATCH:Result.NO_MATCH;}
    }
    record Achieved(String achievement) implements Requirement {
        public Achieved{identifier(achievement);}
        public Result evaluate(AchievementFacts facts,AchievementFacts baseline){return facts.achievements().contains(achievement)?Result.MATCH:Result.NO_MATCH;}
        public Set<String> dependencies(){return Set.of(achievement);}
    }
    private static Set<String> collect(List<Requirement> values){var ids=new HashSet<String>();for(var value:values)ids.addAll(value.dependencies());return Set.copyOf(ids);}
    static void identifier(String value){if(value==null||!value.matches("[a-z][a-z0-9_]{0,47}"))throw new IllegalArgumentException("Invalid achievement/collection id");}
    static AchievementFacts relevant(AchievementFacts facts,Requirement requirement){
        var counts=new HashMap<String,Long>();var forms=new HashSet<CapturedForms.Form>();var progress=new HashMap<String,br.com.shirokaidex.profiles.ProfileProgress.Proof>();var ids=new HashSet<String>();collectFacts(facts,requirement,counts,forms,progress,ids);return new AchievementFacts(counts,forms,progress,ids);
    }
    private static void collectFacts(AchievementFacts facts,Requirement r,Map<String,Long> counts,Set<CapturedForms.Form> forms,Map<String,br.com.shirokaidex.profiles.ProfileProgress.Proof> progress,Set<String> ids){
        if(r instanceof All all)for(var child:all.conditions())collectFacts(facts,child,counts,forms,progress,ids);
        else if(r instanceof Any any)for(var child:any.conditions())collectFacts(facts,child,counts,forms,progress,ids);
        else if(r instanceof Count count)counts.put(count.metric().name()+"|"+count.key(),facts.count(count.metric().name(),count.key()));
        else if(r instanceof Progress value){var proof=facts.progress().get(value.collection());if(proof!=null)progress.put(value.collection(),proof);}
        else if(r instanceof Form form){var value=new CapturedForms.Form(form.species(),form.form());if(facts.forms().contains(value))forms.add(value);}
        else if(r instanceof Achieved achieved&&facts.achievements().contains(achieved.achievement()))ids.add(achieved.achievement());
    }
    static Set<String> collections(Requirement requirement){var result=new HashSet<String>();if(requirement instanceof Progress p)result.add(p.collection());else if(requirement instanceof All all)for(var child:all.conditions())result.addAll(collections(child));else if(requirement instanceof Any any)for(var child:any.conditions())result.addAll(collections(child));return Set.copyOf(result);}
    static boolean publiclyVisible(Requirement requirement,br.com.shirokaidex.profiles.StatisticsPrivacy policy,Map<br.com.shirokaidex.profiles.StatisticsPrivacy.Key,Boolean> overrides,Map<String,br.com.shirokaidex.pokedex.CollectionDefinition.Mode> modes,Map<String,Boolean> medalVisibility){
        if(requirement instanceof Count count)return policy.visible(br.com.shirokaidex.profiles.StatisticsPrivacy.metricKey(count.metric()),overrides);
        if(requirement instanceof Form)return policy.visible(br.com.shirokaidex.profiles.StatisticsPrivacy.Key.SPECIES_CAPTURES,overrides);
        if(requirement instanceof Progress progress)return modes.containsKey(progress.collection())&&(modes.get(progress.collection())!=br.com.shirokaidex.pokedex.CollectionDefinition.Mode.SHINY_CAPTURED||policy.visible(br.com.shirokaidex.profiles.StatisticsPrivacy.Key.SHINY_CAPTURES,overrides));
        if(requirement instanceof All all)return all.conditions().stream().allMatch(c->publiclyVisible(c,policy,overrides,modes,medalVisibility));
        if(requirement instanceof Any any)return any.conditions().stream().allMatch(c->publiclyVisible(c,policy,overrides,modes,medalVisibility));
        if(requirement instanceof Achieved achieved)return medalVisibility.getOrDefault(achieved.achievement(),false);
        return false;
    }
    static int size(Requirement requirement,int depth){if(depth>8)throw new IllegalArgumentException("Achievement requirement is too deep");if(requirement instanceof All all)return 1+all.conditions().stream().mapToInt(c->size(c,depth+1)).sum();if(requirement instanceof Any any)return 1+any.conditions().stream().mapToInt(c->size(c,depth+1)).sum();return 1;}
}
