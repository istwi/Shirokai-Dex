package br.com.shirokaidex.rewards;

import java.util.*;

/** No root wildcards: the operator must authorize the exact context/template pair. */
public record CommandPolicy(boolean enabled,List<CommandReward> allowed) {
    public CommandPolicy {
        allowed=List.copyOf(allowed);
        if(allowed.size()>32||new HashSet<>(allowed).size()!=allowed.size()||allowed.stream().anyMatch(c->!c.snapshot().isEmpty()))throw new IllegalArgumentException("Invalid command allowlist");
    }
    public static CommandPolicy disabled(){return new CommandPolicy(false,List.of());}
    public boolean authorizes(CommandReward command){return enabled&&allowed.contains(command.definition());}
}
