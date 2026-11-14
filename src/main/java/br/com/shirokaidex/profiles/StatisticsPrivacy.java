package br.com.shirokaidex.profiles;

import java.sql.*;
import java.util.*;

public record StatisticsPrivacy(boolean enabled,Map<Key,Boolean> defaults,boolean allowPlayerOverride) {
    public enum Key {
        TOTAL_CAPTURES, SHINY_CAPTURES, LEGENDARY_CAPTURES, SPECIES_CAPTURES, CATEGORY_CAPTURES, PERSONAL_RECORDS;
        public String id(){return name().toLowerCase(Locale.ROOT);}
        public static Key parse(String id){return valueOf(id.toUpperCase(Locale.ROOT));}
    }
    public StatisticsPrivacy {defaults=Map.copyOf(defaults);if(defaults.size()!=Key.values().length)throw new IllegalArgumentException("All privacy defaults are required");}
    public static StatisticsPrivacy defaultsPolicy(){var values=new EnumMap<Key,Boolean>(Key.class);for(var key:Key.values())values.put(key,key!=Key.SPECIES_CAPTURES);return new StatisticsPrivacy(true,values,true);}
    public boolean visible(Key key,Map<Key,Boolean> overrides){return !enabled||(allowPlayerOverride?overrides.getOrDefault(key,defaults.get(key)):defaults.get(key));}
    public static Key metricKey(br.com.shirokaidex.rankings.CaptureRanking.Metric metric){return switch(metric){case TOTAL->Key.TOTAL_CAPTURES;case SHINY->Key.SHINY_CAPTURES;case LEGENDARY,MYTHICAL->Key.LEGENDARY_CAPTURES;case SPECIES->Key.SPECIES_CAPTURES;case CATEGORY->Key.CATEGORY_CAPTURES;case COLLECTION->null;};}
    /** Key and aliases are fixed enums/internal identifiers, never user SQL. */
    public String sql(br.com.shirokaidex.rankings.CaptureRanking.Metric metric,String alias){
        return sql(metricKey(metric),alias);
    }
    public String sql(Key key,String alias){
        if(!enabled||key==null)return "1=1";
        String fallback=defaults.get(key)?"1":"0";
        if(!allowPlayerOverride)return fallback+"=1";
        if(!Set.of("s","e").contains(alias))throw new IllegalArgumentException("Invalid privacy query alias");
        return "coalesce((SELECT visible FROM player_stat_privacy pv WHERE pv.player_uuid="+alias+".player_uuid AND pv.statistic='"+key.id()+"'),"+fallback+")=1";
    }
    public static Map<Key,Boolean> preferences(Connection c,UUID player)throws SQLException {
        var result=new EnumMap<Key,Boolean>(Key.class);try(var q=c.prepareStatement("SELECT statistic,visible FROM player_stat_privacy WHERE player_uuid=?")){q.setString(1,player.toString());try(var rows=q.executeQuery()){while(rows.next())result.put(Key.parse(rows.getString(1)),rows.getBoolean(2));}}return Map.copyOf(result);
    }
    public static void set(Connection c,UUID player,Key key,boolean visible)throws SQLException {
        try(var q=c.prepareStatement("INSERT INTO player_stat_privacy VALUES(?,?,?) ON CONFLICT(player_uuid,statistic) DO UPDATE SET visible=excluded.visible")){q.setString(1,player.toString());q.setString(2,key.id());q.setBoolean(3,visible);q.executeUpdate();}
    }
}
