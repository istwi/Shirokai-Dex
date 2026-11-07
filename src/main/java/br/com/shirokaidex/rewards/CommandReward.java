package br.com.shirokaidex.rewards;

import com.google.gson.JsonObject;
import java.util.*;

/** Trusted exact templates only; a dispatch is never evidence of durable external delivery. */
public record CommandReward(Context context,String command,String snapshot) {
    public enum Context { PLAYER, CONSOLE }
    private static final Set<String> BLOCKED=Set.of("execute","function","schedule","return","op","deop","lp","luckperms","stop","reload","dex","shirokaidex","crates","shirokaicrates");
    public CommandReward(Context context,String command){this(context,command,"");}
    public CommandReward {
        Objects.requireNonNull(context);Objects.requireNonNull(command);Objects.requireNonNull(snapshot);
        if(command.isBlank()||command.length()>512||!command.equals(command.strip())||command.startsWith("/")||command.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid command template");
        String root=command.split(" ",2)[0];
        if(!root.matches("(?:[a-z0-9_.-]+:)?[a-z0-9_]+")||BLOCKED.contains(root.substring(root.indexOf(':')+1)))throw new IllegalArgumentException("Command root not supported for rewards");
        if(!command.contains("{player}")&&!command.contains("{uuid}"))throw new IllegalArgumentException("Command must bind recipient");
        if(java.util.regex.Pattern.compile("\\{[A-Za-z_][A-Za-z0-9_]*\\}").matcher(command.replace("{player}","").replace("{uuid}","")).find())throw new IllegalArgumentException("Unknown command placeholder");
        if(snapshot.length()>768||snapshot.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid command snapshot");
    }
    public String render(String name,UUID id){
        if(name==null||!name.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Invalid recipient name");
        return command.replace("{player}",name).replace("{uuid}",Objects.requireNonNull(id).toString());
    }
    public CommandReward freeze(String name,UUID id){return new CommandReward(context,command,render(name,id));}
    public CommandReward definition(){return new CommandReward(context,command);}
    public CommandReward withSnapshot(String value){return new CommandReward(context,command,value);}
    public JsonObject json(){var value=new JsonObject();value.addProperty("type","COMMAND");value.addProperty("context",context.name());value.addProperty("command",command);value.addProperty("acknowledgement","MANUAL_REVIEW");if(!snapshot.isEmpty())value.addProperty("snapshot",snapshot);return value;}
}
