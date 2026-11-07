package br.com.shirokaidex.spawning;

import java.util.*;

/** Declarative, partial world-pool data, not a prediction or a proof that a species cannot spawn. */
public final class SpawnCatalog {
    public enum Kind { RULE, ALLOW, DENY }
    public enum Field { BIOME, DIMENSION, TICKS }
    public record Value(Field field, String value) { }
    public record Group(Kind kind, List<Value> values) { public Group { values=List.copyOf(values); } }
    public record Rule(String id, String species, String declaredForm, List<Group> groups, boolean composite) {
        public Rule { groups=List.copyOf(groups); }
    }
    public record Card(String rule, String declaredForm, Kind kind, int group, int part, int parts,
                       List<Value> values, boolean composite) {
        public Card { values=List.copyOf(values); }
    }
    private final boolean available;
    private final Map<String,List<Card>> cards;
    private final int skipped;
    private SpawnCatalog(boolean available, List<Rule> rules, int skipped) {
        this.available=available;this.skipped=skipped;
        Map<String,List<Card>> grouped=new HashMap<>();
        for(var rule:rules.stream().sorted(Comparator.comparing(Rule::id).thenComparing(Rule::species)).toList()) {
            var output=grouped.computeIfAbsent(rule.species(),ignored->new ArrayList<>());
            output.add(new Card(rule.id(),rule.declaredForm(),Kind.RULE,0,1,1,List.of(),rule.composite()));
            for(int i=0;i<rule.groups().size();i++) {
                var group=rule.groups().get(i);int parts=Math.max(1,(group.values().size()+9)/10);
                for(int part=0;part<parts;part++) {
                    int first=part*10;
                    output.add(new Card(rule.id(),rule.declaredForm(),group.kind(),i+1,part+1,parts,
                            group.values().subList(first,Math.min(first+10,group.values().size())),rule.composite()));
                }
            }
        }
        Map<String,List<Card>> frozen=new HashMap<>();grouped.forEach((id,value)->frozen.put(id,List.copyOf(value)));cards=Map.copyOf(frozen);
    }
    public static String speciesId(String raw) {
        if(raw==null||raw.isBlank())return null;
        String id=raw.contains(":")?raw:"cobblemon:"+raw;
        return id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")?id:null;
    }
    public static SpawnCatalog unavailable(){return new SpawnCatalog(false,List.of(),0);}
    public static SpawnCatalog known(List<Rule> rules,int skipped){return new SpawnCatalog(true,rules,skipped);}
    public boolean available(){return available;}
    public int skipped(){return skipped;}
    public List<Card> cards(String species){return cards.getOrDefault(species,List.of());}
}
