package br.com.shirokaidex.config;

import br.com.shirokaidex.menus.layout.*;
import com.google.gson.stream.*;
import java.io.IOException;
import java.util.*;

final class MenuNavigationCodec {
    private MenuNavigationCodec() { }
    static MenuNavigation read(JsonReader r)throws IOException {
        Map<MenuNavigation.Screen,MenuNavigation.Layout> layouts=new EnumMap<>(MenuNavigation.Screen.class);
        r.beginObject();while(r.hasNext()){
            String name=r.nextName();
            var screen=Arrays.stream(MenuNavigation.Screen.values()).filter(value->value.id().equals(name)).findFirst().orElseThrow(()->new IOException("unknown menu "+name));
            if(layouts.containsKey(screen))throw new IOException("duplicate menu "+name);
            Map<String,Integer> slots=new HashMap<>(screen.defaults);boolean footer=true;Set<String> fields=new HashSet<>();
            int rows=screen.size/9;String title="";Optional<HomeLayout.Palette> palette=Optional.empty();Map<String,MenuAppearance.Button> buttons=new HashMap<>();
            r.beginObject();while(r.hasNext()){
                String field=r.nextName();if(!fields.add(field))throw new IOException("duplicate menu field "+field);
                switch(field){
                    case "rows"->rows=integer(r);
                    case "title"->title=string(r);
                    case "palette"->palette=Optional.of(HomeLayout.Palette.valueOf(string(r)));
                    case "buttons"->{
                        r.beginObject();while(r.hasNext()){
                            String action=r.nextName();if(!screen.defaults.containsKey(action)||buttons.containsKey(action))throw new IOException("unknown/duplicate menu button "+action);
                            String icon="",nameTemplate="";List<String> lore=new ArrayList<>();Set<String> buttonFields=new HashSet<>();
                            r.beginObject();while(r.hasNext()){
                                String key=r.nextName();if(!buttonFields.add(key))throw new IOException("duplicate button field "+key);
                                switch(key){
                                    case "icon"->icon=string(r);
                                    case "name"->nameTemplate=string(r);
                                    case "lore"->{r.beginArray();while(r.hasNext()){if(lore.size()>=8)throw new IOException("maximum eight extra lore lines");lore.add(string(r));}r.endArray();}
                                    default->throw new IOException("unknown button field "+key);
                                }
                            }r.endObject();buttons.put(action,new MenuAppearance.Button(icon,nameTemplate,lore));
                        }r.endObject();
                    }
                    case "footer"->{if(r.peek()!=JsonToken.BOOLEAN)throw new IOException("footer must be boolean");footer=r.nextBoolean();}
                    case "slots"->{
                        Set<String> seen=new HashSet<>();r.beginObject();while(r.hasNext()){
                            String action=r.nextName();if(!screen.defaults.containsKey(action)||!seen.add(action))throw new IOException("unknown/duplicate navigation action "+action);
                            if(r.peek()!=JsonToken.NUMBER)throw new IOException("navigation slot must be integer");
                            String value=r.nextString();if(!value.matches("0|[1-9][0-9]?"))throw new IOException("invalid navigation number");slots.put(action,Integer.parseInt(value));
                        }r.endObject();
                    }
                    default->throw new IOException("unknown menu field "+field);
                }
            }r.endObject();layouts.put(screen,new MenuNavigation.Layout(screen,footer,slots,rows,title,palette,buttons));
        }r.endObject();return new MenuNavigation(layouts);
    }
    private static String string(JsonReader r)throws IOException{if(r.peek()!=JsonToken.STRING)throw new IOException("menu text must be string");return r.nextString();}
    private static int integer(JsonReader r)throws IOException{if(r.peek()!=JsonToken.NUMBER)throw new IOException("menu rows must be integer");String value=r.nextString();if(!value.matches("[3-6]"))throw new IOException("menu rows must be 3..6");return Integer.parseInt(value);}
}
