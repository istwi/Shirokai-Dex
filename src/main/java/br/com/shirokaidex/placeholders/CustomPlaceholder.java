package br.com.shirokaidex.placeholders;

import java.util.*;

/** A bounded presentation alias over an existing authorized metric, never an expression or command. */
public record CustomPlaceholder(String id,String metric,String argument,String template) {
    public CustomPlaceholder {
        if(id==null||!id.matches("[a-z][a-z0-9_]{0,47}"))throw new IllegalArgumentException("Invalid custom placeholder id");
        Objects.requireNonNull(metric);Objects.requireNonNull(argument);Objects.requireNonNull(template);
        if(metric.equals("custom")||!PlaceholderValues.METRICS.contains(metric)&&!PublicMetricValues.METRICS.contains(metric))throw new IllegalArgumentException("Unknown custom placeholder metric");
        if(PlaceholderValues.METRICS.contains(metric)?PlaceholderValues.collection(argument)==null:PublicMetricValues.argument(metric,argument)==null)throw new IllegalArgumentException("Invalid custom placeholder argument");
        if(template.length()>160||!template.contains("{value}")||template.replace("{value}","").matches("(?s).*[{}%].*")||template.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Custom placeholder template must contain only literal text and {value}");
    }
    public String render(String value){return value.equals(PlaceholderValues.UNKNOWN)?value:template.replace("{value}",value);}
    public static List<CustomPlaceholder> validate(List<CustomPlaceholder> list){if(list.size()>64)throw new IllegalArgumentException("Too many custom placeholders");var ids=new HashSet<String>();for(var item:list)if(!ids.add(item.id()))throw new IllegalArgumentException("Duplicate custom placeholder id");return List.copyOf(list);}
}
