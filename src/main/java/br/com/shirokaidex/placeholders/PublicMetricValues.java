package br.com.shirokaidex.placeholders;

import br.com.shirokaidex.profiles.*;
import java.util.Set;

/** Only consumes already filtered PUBLIC profile data; never the owner's privileged view. */
public final class PublicMetricValues {
    public static final Set<String> METRICS=Set.of("stats","rank","medals","victories","claimed_rewards","available_rewards","custom");
    public static final Set<String> STATS=Set.of("total_captures","shiny_captures","legendary_captures","today","week","month");
    private PublicMetricValues() { }
    public static String argument(String metric,String input){
        String arg=input==null?"":input.strip();
        return switch(metric){
            case "stats" -> STATS.contains(arg)?arg:null;
            case "rank","custom" -> arg.matches("[a-z][a-z0-9_]{0,47}")?arg:null;
            case "medals" -> arg.isEmpty()||arg.equals("count")||arg.matches("[123]")?arg:null;
            case "victories","claimed_rewards","available_rewards" -> arg.isEmpty()?arg:null;
            default -> null;
        };
    }
    public static String format(ProfileStore.Data data,String metric,String arg){
        if(data==null)return PlaceholderValues.UNKNOWN;
        return switch(metric){
            case "stats" -> switch(arg){
                case "today" -> number(data.today());case "week" -> number(data.week());case "month" -> number(data.month());
                default -> {var key=StatisticsPrivacy.Key.parse(arg);yield number(data.statistics().get(key));}
            };
            case "rank" -> number(data.rankingPositions().get(arg));
            case "victories" -> Long.toString(data.victories());
            case "claimed_rewards" -> Long.toString(data.claimedRewards());
            case "medals" -> arg.isEmpty()||arg.equals("count")?Integer.toString(data.medals().size()):data.medals().stream().filter(m->m.favoriteSlot()!=null&&m.favoriteSlot()==Integer.parseInt(arg)-1).map(m->m.definition().name()).findFirst().orElse("");
            default -> PlaceholderValues.UNKNOWN;
        };
    }
    private static String number(Long value){return value==null?PlaceholderValues.UNKNOWN:Long.toString(value);}
}
