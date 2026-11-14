package br.com.shirokaidex.notifications;

import java.util.*;

public record NotificationPolicy(Map<Event,Rule> rules) {
    public enum Event { PROGRESS, REWARD_UNLOCKED, REWARD_CLAIMED, ACHIEVEMENT, COMPETITION_JOINED, COMPETITION_ENDED, RESULTS, PRIZE_AVAILABLE }
    public enum Channel { CHAT, ACTIONBAR, TITLE, SOUND }
    public record Rule(boolean enabled,Set<Channel> channels,boolean global,String sound){
        public Rule {channels=Set.copyOf(channels);if(channels.isEmpty())throw new IllegalArgumentException("Notification needs a channel");br.com.shirokaidex.pokedex.CollectionDefinition.identifier(sound);}
    }
    public NotificationPolicy {
        rules=Map.copyOf(rules);if(rules.size()!=Event.values().length)throw new IllegalArgumentException("Missing notification rule");
        for(var entry:rules.entrySet())if(entry.getValue().global()&&entry.getKey()!=Event.COMPETITION_ENDED&&entry.getKey()!=Event.RESULTS)throw new IllegalArgumentException("Only public competition closure/results may be global");
    }
    public static NotificationPolicy defaults(){var rules=new EnumMap<Event,Rule>(Event.class);for(var event:Event.values())rules.put(event,new Rule(event!=Event.PROGRESS,Set.of(Channel.CHAT),false,"minecraft:entity.experience_orb.pickup"));return new NotificationPolicy(rules);}
}
