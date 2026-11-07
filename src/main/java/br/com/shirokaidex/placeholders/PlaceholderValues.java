package br.com.shirokaidex.placeholders;

import br.com.shirokaidex.pokedex.ProgressCalculator;
import java.util.Set;

/** Values describe official collection progress, never verified capture statistics. */
public final class PlaceholderValues {
    public static final String UNKNOWN = "N/D";
    public static final Set<String> METRICS = Set.of("percent", "registered", "seen", "total", "remaining");
    private PlaceholderValues() { }
    public static String collection(String argument) {
        if (argument == null || argument.isBlank()) return "national";
        String id = argument.strip();
        return id.matches("[a-z][a-z0-9_]{0,47}") ? id : null;
    }
    public static String format(String metric, ProgressCalculator.CollectionProgress value, String language) {
        if (value == null) return UNKNOWN;
        var progress = value.progress();
        return switch (metric) {
            case "total" -> Integer.toString(progress.total());
            case "seen" -> Integer.toString(value.seen());
            case "registered" -> progress.registered().isPresent() ? Integer.toString(progress.registered().getAsInt()) : UNKNOWN;
            case "remaining" -> progress.registered().isPresent() ? Integer.toString(progress.total() - progress.registered().getAsInt()) : UNKNOWN;
            case "percent" -> progress.formattedPercentage(language).orElse(UNKNOWN);
            default -> UNKNOWN;
        };
    }
}
