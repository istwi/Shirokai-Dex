package br.com.shirokaidex.config;

import br.com.shirokaidex.rewards.RewardPlan;
import br.com.shirokaidex.rewards.CurrencyReward;
import com.google.gson.stream.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

final class RewardsCodec {
    private RewardsCodec() { }
    static List<RewardPlan> read(JsonReader r,java.util.function.Function<String,br.com.shirokaidex.rewards.SavedItem> resolver)throws IOException {
        List<RewardPlan> result=new ArrayList<>();Set<String> ids=new HashSet<>();r.beginArray();
        while(r.hasNext()) {
            if(result.size()>=128)throw error(r,"maximum 128 rewards");
            Set<String> keys=new HashSet<>();String id=null,collection=null;int minimum=0;BigDecimal percent=null;List<RewardPlan.Item> items=new ArrayList<>();List<br.com.shirokaidex.rewards.PokemonReward> pokemon=List.of();List<CurrencyReward> currencies=new ArrayList<>();
            r.beginObject();while(r.hasNext()){
                String key=r.nextName();unique(r,keys,key);
                switch(key){
                    case "id"->id=string(r);
                    case "collection"->collection=string(r);
                    case "minimum_registered"->minimum=integer(r);
                    case "minimum_percent"->{if(r.peek()!=JsonToken.NUMBER)throw error(r,"expected numeric percent");percent=new BigDecimal(r.nextString());}
                    case "currencies"->{
                        r.beginArray();while(r.hasNext()){
                            if(currencies.size()>=8)throw error(r,"maximum eight currency entries");
                            Set<String> fields=new HashSet<>();String currency=null,type="CURRENCY";BigDecimal amount=null;
                            r.beginObject();while(r.hasNext()){String field=r.nextName();unique(r,fields,field);switch(field){
                                case "type"->type=string(r);case "currency"->currency=string(r);
                                case "amount"->{if(r.peek()!=JsonToken.NUMBER)throw error(r,"amount must be numeric");String value=r.nextString();if(!value.matches("[0-9]{1,16}(\\.[0-9]{1,8})?"))throw error(r,"invalid amount");amount=new BigDecimal(value);}
                                default->throw error(r,"unknown currency field "+field);
                            }}r.endObject();if(!type.equals("CURRENCY")||currency==null||amount==null)throw error(r,"CURRENCY requires currency and amount");currencies.add(new CurrencyReward(currency,amount));
                        }r.endArray();
                    }
                    case "pokemon"->pokemon=PokemonRewardsCodec.read(r);
                    case "items"->{
                        r.beginArray();while(r.hasNext()){
                            if(items.size()>=32)throw error(r,"maximum 32 item entries");
                            Set<String> fields=new HashSet<>();String item=null,saved=null,data="",type=null;Integer count=null;r.beginObject();
                            while(r.hasNext()){String field=r.nextName();unique(r,fields,field);switch(field){case "type"->type=string(r);case "id"->item=string(r);case "count"->count=integer(r);case "saved_item"->saved=string(r);case "payload_snbt"->data=string(r);default->throw error(r,"unknown item field "+field);}}
                            r.endObject();
                            if((item==null)==(saved==null))throw error(r,"choose id or saved_item");
                            if(type!=null){
                                if(!Set.of("ITEM","CUSTOM_ITEM","SAVED_ITEM").contains(type))throw error(r,"unsupported item type "+type);
                                if(type.equals("ITEM")&&(saved!=null||!data.isEmpty()))throw error(r,"ITEM cannot contain a saved reference or components");
                                if(type.equals("CUSTOM_ITEM")&&(saved!=null||data.isBlank()))throw error(r,"CUSTOM_ITEM requires id and payload_snbt");
                                if(type.equals("SAVED_ITEM")&&saved==null)throw error(r,"SAVED_ITEM requires saved_item");
                            }
                            if(saved!=null){
                                if(fields.contains("payload_snbt"))throw error(r,"saved_item cannot override payload");
                                if(!saved.matches("[a-z][a-z0-9_]{0,47}"))throw error(r,"invalid saved item reference");
                                br.com.shirokaidex.rewards.SavedItem template;
                                try{template=resolver.apply(saved);}catch(RuntimeException failure){throw new IOException("saved item lookup failed at "+r.getPath(),failure);}
                                if(template==null)throw error(r,"unknown saved item "+saved);
                                items.add(template.snapshot(count));
                            }else{if(count==null)throw error(r,"item count required");items.add(new RewardPlan.Item(item,count,data));}
                        }r.endArray();
                    }
                    default->throw error(r,"unknown reward field "+key);
                }
            }r.endObject();
            if(keys.contains("minimum_registered")&&keys.contains("minimum_percent"))throw error(r,"choose one threshold");
            RewardPlan plan=new RewardPlan(id,collection,minimum,percent,items,false,pokemon,currencies);
            if(!ids.add(plan.id()))throw error(r,"duplicate reward id "+id);result.add(plan);
        }r.endArray();return List.copyOf(result);
    }
    private static String string(JsonReader r)throws IOException{if(r.peek()!=JsonToken.STRING)throw error(r,"expected string");return r.nextString();}
    private static int integer(JsonReader r)throws IOException{
        if(r.peek()!=JsonToken.NUMBER)throw error(r,"expected integer");String number=r.nextString();
        if(!number.matches("[1-9][0-9]{0,9}"))throw error(r,"expected positive integer");
        try{return Integer.parseInt(number);}catch(NumberFormatException failure){throw error(r,"integer out of range");}
    }
    private static void unique(JsonReader r,Set<String> keys,String key)throws IOException{if(!keys.add(key))throw error(r,"duplicate field "+key);}
    private static IOException error(JsonReader r,String message){return new IOException("config.json "+r.getPath()+": "+message);}
}
