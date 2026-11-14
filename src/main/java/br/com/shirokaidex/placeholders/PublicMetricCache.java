package br.com.shirokaidex.placeholders;

import java.util.*;
import java.util.concurrent.TimeUnit;

/** Server-thread cache of requested public values. Bounded requests; no database work in read(). */
public final class PublicMetricCache {
    public record Key(UUID player,String metric,String argument,String access) {
        public Key(UUID player,String metric,String argument){this(player,metric,argument,"");}
    }
    public record Request(Key key,Object settings,long privacy,long generation) { }
    private record Value(Request request,long created,String text) { }
    private static final int LIMIT=4096, PER_PLAYER=64;
    private final LinkedHashMap<Key,Value> values=new LinkedHashMap<>(64,.75f,true);
    private final LinkedHashMap<Key,Request> pending=new LinkedHashMap<>();
    private final Map<Key,Request> running=new HashMap<>();
    private final Map<UUID,Integer> queuedCounts=new HashMap<>();
    private long generation;
    public String read(Key key,Object settings,long privacy,long now) {
        var value=values.get(key);
        boolean current=value!=null&&value.request.settings()==settings&&value.request.privacy()==privacy;
        if(!current||now-value.created>=TimeUnit.SECONDS.toNanos(5)) {
            if(!pending.containsKey(key)&&!running.containsKey(key)&&pending.size()+running.size()<LIMIT) {
                int count=queuedCounts.getOrDefault(key.player(),0);
                if(count<PER_PLAYER){pending.put(key,new Request(key,settings,privacy,generation));queuedCounts.put(key.player(),count+1);}
            }
        }
        return current?value.text:PlaceholderValues.UNKNOWN;
    }
    public Request poll(){if(pending.isEmpty()||running.size()>=4)return null;var it=pending.entrySet().iterator();var next=it.next();var request=next.getValue();it.remove();running.put(request.key(),request);return request;}
    public void complete(Request request,long now,String text){
        if(running.get(request.key())!=request)return;
        running.remove(request.key());
        queuedCounts.computeIfPresent(request.key().player(),(id,count)->count<=1?null:count-1);
        if(request.generation()!=generation)return;
        values.put(request.key(),new Value(request,now,Objects.requireNonNull(text)));
        while(values.size()>LIMIT)values.remove(values.keySet().iterator().next());
    }
    public void remove(UUID player){values.keySet().removeIf(k->k.player().equals(player));pending.keySet().removeIf(k->k.player().equals(player));running.keySet().removeIf(k->k.player().equals(player));queuedCounts.remove(player);}
    public void clear(){generation++;values.clear();pending.clear();running.clear();queuedCounts.clear();}
}
