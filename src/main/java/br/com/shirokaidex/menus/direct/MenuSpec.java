package br.com.shirokaidex.menus.direct;
import java.util.*;
public record MenuSpec(String title,int rows,Icon background,Map<String,Button> buttons,List<Integer> contentSlots,List<Integer> rewardSlots,Map<String,Icon> rewardStates,Icon entry,String rankingLine,Map<String,String> filterNames){
    public record Icon(String item,String name,List<String> lore,boolean glint){public Icon{Objects.requireNonNull(item);if(!item.isEmpty())br.com.shirokaidex.pokedex.CollectionDefinition.identifier(item);if(item.equals("minecraft:air"))throw new IllegalArgumentException("Empty menu icon");if(name==null||name.length()>512)throw new IllegalArgumentException("Menu name too long");lore=List.copyOf(lore);if(lore.size()>32||lore.stream().anyMatch(s->s.length()>1024))throw new IllegalArgumentException("Menu lore too long");}}
    public record Button(int slot,Icon icon,String command) {
        public Button(int slot,Icon icon){this(slot,icon,"");}
        public Button {
            Objects.requireNonNull(icon);Objects.requireNonNull(command);
            if(command.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid button command");
            command=command.strip();
            if(command.equals("/"))throw new IllegalArgumentException("Empty button command");
            if(command.startsWith("/"))command=command.substring(1);
            if(command.length()>256||(!command.isEmpty()&&!command.matches("[a-z0-9_.:-]+(?: +.+)?")))throw new IllegalArgumentException("Invalid button command");
        }
    }
    public MenuSpec {if(title==null||title.length()>512||rows<3||rows>6)throw new IllegalArgumentException("Invalid menu title/rows");Objects.requireNonNull(background);buttons=Map.copyOf(buttons);contentSlots=List.copyOf(contentSlots);rewardSlots=List.copyOf(rewardSlots);filterNames=Map.copyOf(filterNames);br.com.shirokaidex.menus.direct.RichText.validate(rankingLine);rewardStates=Map.copyOf(rewardStates);if(!rewardSlots.isEmpty()&&!rewardStates.keySet().equals(Set.of("locked","available","claimed")))throw new IllegalArgumentException("Missing reward styles");var used=new HashSet<Integer>();for(var b:buttons.values())if(!used.add(b.slot()))throw new IllegalArgumentException("Conflicting button slots");for(int n:contentSlots)if(!used.add(n))throw new IllegalArgumentException("Conflicting content slot");for(int n:rewardSlots)if(!used.add(n))throw new IllegalArgumentException("Conflicting reward slot");if(used.stream().anyMatch(n->n<0||n>=rows*9))throw new IllegalArgumentException("Menu slot outside inventory");}
}
