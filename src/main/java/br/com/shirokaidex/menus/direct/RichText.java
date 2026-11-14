package br.com.shirokaidex.menus.direct;

import net.minecraft.network.chat.*;
import java.util.*;
import java.util.regex.*;

/** Small presentation-only MiniMessage subset used by the supplied menus; dynamic values stay literal. */
public final class RichText {
    private static final Pattern TOKEN=Pattern.compile("<[^>]*>|%[a-zA-Z0-9_]+%");
    private static final Map<String,Integer> COLORS=Map.ofEntries(Map.entry("black",0x000000),Map.entry("dark_blue",0x0000AA),Map.entry("dark_green",0x00AA00),Map.entry("dark_aqua",0x00AAAA),Map.entry("dark_red",0xAA0000),Map.entry("dark_purple",0xAA00AA),Map.entry("gold",0xFFAA00),Map.entry("gray",0xAAAAAA),Map.entry("dark_gray",0x555555),Map.entry("blue",0x5555FF),Map.entry("green",0x55FF55),Map.entry("aqua",0x55FFFF),Map.entry("red",0xFF5555),Map.entry("light_purple",0xFF55FF),Map.entry("yellow",0xFFFF55),Map.entry("white",0xFFFFFF));
    private RichText() { }
    public static void validate(String template){if(template.length()>2048||template.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid menu template size/control character");var m=TOKEN.matcher(template);while(m.find())if(m.group().startsWith("<")){String tag=m.group().substring(1,m.group().length()-1);if(tag.startsWith("/"))tag=tag.substring(1);if(tag.equals("gradient")&&!m.group().startsWith("</"))throw new IllegalArgumentException("Gradient requires two colors");if(!COLORS.containsKey(tag)&&!Set.of("bold","italic","underlined","reset","gradient").contains(tag)&&!tag.matches("#[0-9a-fA-F]{6}")&&!tag.matches("gradient:#[0-9a-fA-F]{6}:#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Unsupported menu text tag: "+tag);}}
    private record Format(Style style,Integer from,Integer to) { }
    public static Component render(String template,Map<String,String> values){
        validate(template);var out=Component.empty();var state=new Format(Style.EMPTY.withItalic(false).withColor(0xAAAAAA),null,null);var stack=new ArrayDeque<Format>();var matcher=TOKEN.matcher(template);int last=0;
        while(matcher.find()){append(out,template.substring(last,matcher.start()),state);String token=matcher.group();if(token.startsWith("%")){append(out,values.getOrDefault(token.substring(1,token.length()-1),token),state);}else{String tag=token.substring(1,token.length()-1);if(tag.startsWith("/")){if(!stack.isEmpty())state=stack.pop();}else if(tag.equals("reset")){stack.clear();state=new Format(Style.EMPTY.withItalic(false).withColor(0xAAAAAA),null,null);}else{stack.push(state);if(tag.startsWith("gradient:")){var parts=tag.split(":");state=new Format(state.style(),Integer.parseInt(parts[1].substring(1),16),Integer.parseInt(parts[2].substring(1),16));}else if(tag.equals("bold"))state=new Format(state.style().withBold(true),state.from(),state.to());else if(tag.equals("italic"))state=new Format(state.style().withItalic(true),state.from(),state.to());else if(tag.equals("underlined"))state=new Format(state.style().withUnderlined(true),state.from(),state.to());else state=new Format(state.style().withColor(tag.startsWith("#")?Integer.parseInt(tag.substring(1),16):COLORS.get(tag)),null,null);}}
            last=matcher.end();
        }append(out,template.substring(last),state);return out;
    }
    private static void append(MutableComponent out,String value,Format format){if(format.from()==null){out.append(Component.literal(value).setStyle(format.style()));return;}int[] points=value.codePoints().toArray();for(int i=0;i<points.length;i++){double ratio=points.length<=1?0:(double)i/(points.length-1);int color=0;for(int shift:new int[]{0,8,16})color|=((int)Math.round(((format.from()>>shift)&255)*(1-ratio)+((format.to()>>shift)&255)*ratio))<<shift;out.append(Component.literal(new String(Character.toChars(points[i]))).setStyle(format.style().withColor(color)));}}
}
