package br.com.shirokaidex.menus.layout;

import br.com.shirokaidex.pokedex.CollectionDefinition;
import java.util.*;

/** Plain-text templates only; never evaluates commands or external placeholder expressions. */
public final class MenuAppearance {
    private MenuAppearance() { }
    public record Button(String icon,String name,List<String> lore){
        public Button {
            Objects.requireNonNull(icon);Objects.requireNonNull(name);lore=List.copyOf(lore);
            if(!icon.isEmpty()){CollectionDefinition.identifier(icon);if(icon.equals("minecraft:air"))throw new IllegalArgumentException("empty menu icon");}
            template(name,128,Set.of("name","player"));
            if(!name.isEmpty()&&!name.contains("{name}"))throw new IllegalArgumentException("button name must retain real action/state using {name}");
            if(lore.size()>8)throw new IllegalArgumentException("maximum eight extra lore lines");
            for(String line:lore)template(line,160,Set.of("name","player"));
        }
    }
    public static void template(String value,int maximum,Set<String> keys){
        Objects.requireNonNull(value);
        if(value.length()>maximum||value.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("invalid menu text length/control character");
        String rest=value;for(String key:keys)rest=rest.replace("{"+key+"}","");
        if(rest.contains("{")||rest.contains("}"))throw new IllegalArgumentException("unknown or malformed menu placeholder");
    }
    public static String render(String template,String key,String original,String player){
        return template.replace("{player}",player).replace("{"+key+"}",original);
    }
}
