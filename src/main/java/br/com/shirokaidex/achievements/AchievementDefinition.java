package br.com.shirokaidex.achievements;

import java.util.*;

public record AchievementDefinition(String id,String name,String description,String icon,boolean secret,String hint,
        Policy policy,boolean retroactive,boolean publicMedal,boolean notifyPlayer,Set<String> dependencies,Requirement requirement) {
    public enum Policy { PERMANENT, RECALCULATE }
    public AchievementDefinition {
        Requirement.identifier(id);text(name,96,false);text(description,512,true);text(hint,512,true);br.com.shirokaidex.pokedex.CollectionDefinition.identifier(icon);Objects.requireNonNull(policy);Objects.requireNonNull(requirement);dependencies=Set.copyOf(dependencies);dependencies.forEach(Requirement::identifier);if(Requirement.size(requirement,0)>256)throw new IllegalArgumentException("Too many achievement conditions");
    }
    private static void text(String value,int maximum,boolean empty){if(value==null||value.length()>maximum||!empty&&value.isBlank()||value.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid achievement text");}
    public Set<String> references(){var ids=new HashSet<>(dependencies);ids.addAll(requirement.dependencies());return Set.copyOf(ids);}
    public static List<AchievementDefinition> validate(List<AchievementDefinition> definitions){
        if(definitions.size()>128)throw new IllegalArgumentException("Too many achievements");var byId=new LinkedHashMap<String,AchievementDefinition>();for(var definition:definitions)if(byId.putIfAbsent(definition.id(),definition)!=null)throw new IllegalArgumentException("Duplicate achievement id");
        var done=new HashSet<String>();var ordered=new ArrayList<AchievementDefinition>();for(var id:byId.keySet())visit(id,byId,new HashSet<>(),done,ordered);return List.copyOf(ordered);
    }
    private static void visit(String id,Map<String,AchievementDefinition> definitions,Set<String> active,Set<String> done,List<AchievementDefinition> ordered){if(done.contains(id))return;var definition=definitions.get(id);if(definition==null)throw new IllegalArgumentException("Unknown achievement dependency");if(!active.add(id))throw new IllegalArgumentException("Achievement dependency cycle");for(var ref:definition.references())visit(ref,definitions,active,done,ordered);active.remove(id);done.add(id);ordered.add(definition);}
}
