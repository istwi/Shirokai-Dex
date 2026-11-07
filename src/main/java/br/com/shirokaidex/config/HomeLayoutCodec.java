package br.com.shirokaidex.config;

import br.com.shirokaidex.menus.layout.HomeLayout;
import com.google.gson.stream.*;
import java.io.IOException;
import java.util.*;

final class HomeLayoutCodec {
    private HomeLayoutCodec() { }
    static HomeLayout read(JsonReader r)throws IOException {
        int rows=6;var palette=HomeLayout.Palette.VIOLET;Map<String,Integer> slots=new HashMap<>(HomeLayout.DEFAULT_SLOTS);Set<String> fields=new HashSet<>();
        r.beginObject();while(r.hasNext()){
            String key=r.nextName();if(!fields.add(key))throw new IOException("duplicate home field "+key);
            switch(key){
                case "rows"->rows=integer(r);
                case "palette"->{if(r.peek()!=JsonToken.STRING)throw new IOException("home.palette must be string");palette=HomeLayout.Palette.valueOf(r.nextString());}
                case "slots"->{Set<String> known=new HashSet<>();r.beginObject();while(r.hasNext()){String button=r.nextName();if(!HomeLayout.DEFAULT_SLOTS.containsKey(button)||!known.add(button))throw new IOException("unknown/duplicate home button "+button);slots.put(button,integer(r));}r.endObject();}
                default->throw new IOException("unknown home field "+key);
            }
        }r.endObject();return new HomeLayout(rows,palette,slots);
    }
    private static int integer(JsonReader r)throws IOException{if(r.peek()!=JsonToken.NUMBER)throw new IOException("home slot/rows must be integer");String value=r.nextString();if(!value.matches("0|[1-9][0-9]{0,2}"))throw new IOException("invalid home number");return Integer.parseInt(value);}
}
