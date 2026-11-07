package br.com.shirokaidex.menus.layout;

import java.util.*;

/** Fixed, server-owned actions can move only within navigation rows, never over payloads. */
public record MenuNavigation(Map<Screen,Layout> layouts) {
    public enum Screen {
        SELECTOR(27,Map.of("previous",18,"next",26,"close",22,"home",23)),
        COLLECTION(27,Map.of("back",18,"close",22)),
        WIKI(36,Map.ofEntries(Map.entry("previous",18),Map.entry("filter",19),Map.entry("order",20),Map.entry("search",21),Map.entry("close",22),Map.entry("back",23),Map.entry("clear",24),Map.entry("type",25),Map.entry("next",26),Map.entry("category",27))),
        WIKI_TYPES(36,Map.of("previous",18,"next",26,"back",23,"close",22)),
        WIKI_CATEGORIES(36,Map.of("previous",18,"next",26,"back",23,"close",22)),
        WIKI_DETAILS(36,Map.of("back",18,"collection",23,"close",22)),
        WIKI_FORMS(36,Map.of("previous",18,"next",26,"back",23,"close",22)),
        WIKI_SPAWNS(36,Map.of("previous",18,"next",26,"back",23,"close",22,"scope",27)),
        REWARDS(27,Map.of("previous",18,"next",26,"home",23,"close",22)),
        REWARD(27,Map.of("back",18,"status",19,"previous",20,"close",22,"claim",24,"next",26)),
        CONFIRMATION(27,Map.of("back",18,"close",22));
        public final int size;
        public final Map<String,Integer> defaults;
        Screen(int size,Map<String,Integer> defaults){this.size=size;this.defaults=Map.copyOf(defaults);}
        public String id(){return name().toLowerCase(Locale.ROOT);}
    }
    public record Layout(Screen screen,boolean footer,Map<String,Integer> slots,int rows,String title,Optional<HomeLayout.Palette> palette,Map<String,MenuAppearance.Button> buttons){
        public Layout(Screen screen,boolean footer,Map<String,Integer> slots){this(screen,footer,slots,screen.size/9,"",Optional.empty(),Map.of());}
        public Layout {
            Objects.requireNonNull(screen);slots=Map.copyOf(slots);Objects.requireNonNull(palette);buttons=Map.copyOf(buttons);
            if(rows<screen.size/9||rows>6)throw new IllegalArgumentException("menu rows outside supported range");
            MenuAppearance.template(title,160,Set.of("title","player"));
            if(!screen.defaults.keySet().containsAll(buttons.keySet()))throw new IllegalArgumentException("unknown button appearance");
            if(!slots.keySet().equals(screen.defaults.keySet()))throw new IllegalArgumentException("unknown/missing navigation action");
            if(new HashSet<>(slots.values()).size()!=slots.size())throw new IllegalArgumentException("navigation slot conflict");
            if(slots.values().stream().anyMatch(slot->slot<18||slot>=rows*9))throw new IllegalArgumentException("navigation must stay outside content and inside menu");
        }
        /** Source->destination permutation, including absent controls, supports swaps without overwriting. */
        public Map<Integer,Integer> moves(){
            Map<Integer,Integer> result=new HashMap<>();screen.defaults.forEach((role,source)->result.put(source,slots.get(role)));return Map.copyOf(result);
        }
    }
    public MenuNavigation {
        layouts=Map.copyOf(layouts);
        layouts.forEach((screen,layout)->{if(screen!=layout.screen())throw new IllegalArgumentException("navigation screen mismatch");});
    }
    public Layout layout(Screen screen){return layouts.getOrDefault(screen,new Layout(screen,true,screen.defaults));}
    public static MenuNavigation defaults(){return new MenuNavigation(Map.of());}
}
