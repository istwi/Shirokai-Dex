package br.com.shirokaidex.competitions;

import br.com.shirokaidex.rankings.CaptureRanking;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Immutable schedule/rules. Existing editions retain their serialized definition. */
public record CompetitionDefinition(String id,String name,Cadence cadence,ZoneId timezone,Instant start,Instant end,
        CaptureRanking.Metric metric,String key,CategoryMode categoryMode,Visibility visibility,int reviewHours,
        boolean autoFinalize,ReviewMode reviewMode,int hourlyReviewThreshold,String permission,PrizePolicy prizes) {
    public CompetitionDefinition(String id,String name,Cadence cadence,ZoneId timezone,Instant start,Instant end,CaptureRanking.Metric metric,String key,CategoryMode categoryMode,Visibility visibility,int reviewHours,boolean autoFinalize,ReviewMode reviewMode,int hourlyReviewThreshold,String permission){this(id,name,cadence,timezone,start,end,metric,key,categoryMode,visibility,reviewHours,autoFinalize,reviewMode,hourlyReviewThreshold,permission,PrizePolicy.disabled());}
    public enum Cadence { WEEKLY, MONTHLY, CUSTOM }
    public enum CategoryMode { SNAPSHOT, LIVE }
    public enum Visibility { PUBLIC, PARTIAL, HIDDEN }
    public enum ReviewMode { LOG_ONLY, HOLD_FOR_REVIEW }
    public record Window(Instant start,Instant end) { }
    public CompetitionDefinition {
        new CaptureRanking(id,name,metric,key,permission);
        if(metric==CaptureRanking.Metric.COLLECTION)throw new IllegalArgumentException("Competitions count verified captures, not Pokedex entries");
        Objects.requireNonNull(prizes);Objects.requireNonNull(cadence);Objects.requireNonNull(timezone);Objects.requireNonNull(categoryMode);Objects.requireNonNull(visibility);Objects.requireNonNull(reviewMode);
        if(reviewHours<0||reviewHours>8760||hourlyReviewThreshold<0||hourlyReviewThreshold>100000)throw new IllegalArgumentException("Invalid competition review limits");
        if(cadence==Cadence.CUSTOM){if(start==null||end==null||!start.isBefore(end))throw new IllegalArgumentException("CUSTOM requires ordered start/end");}
        else if(start!=null||end!=null)throw new IllegalArgumentException("Recurring calendars do not accept custom dates");
    }
    public Optional<Window> window(Instant now) {
        if(cadence==Cadence.CUSTOM)return now.isBefore(start)||!now.isBefore(end)?Optional.empty():Optional.of(new Window(start,end));
        var date=now.atZone(timezone).toLocalDate();
        var first=cadence==Cadence.WEEKLY?date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)):date.withDayOfMonth(1);
        var last=cadence==Cadence.WEEKLY?first.plusWeeks(1):first.plusMonths(1);
        return Optional.of(new Window(first.atStartOfDay(timezone).toInstant(),last.atStartOfDay(timezone).toInstant()));
    }
    public static List<CompetitionDefinition> validate(List<CompetitionDefinition> definitions) {
        if(definitions.size()>32)throw new IllegalArgumentException("Too many competitions");var ids=new HashSet<String>();for(var definition:definitions)if(!ids.add(definition.id()))throw new IllegalArgumentException("Duplicate competition id");return List.copyOf(definitions);
    }
}
