package br.com.shirokaidex.config;

import br.com.shirokaidex.rewards.*;
import com.google.gson.stream.*;
import java.io.IOException;
import java.util.*;

final class CommandRewardsCodec {
    private CommandRewardsCodec() { }
    static List<CommandReward> read(JsonReader r,int maximum)throws IOException{
        List<CommandReward> result=new ArrayList<>();r.beginArray();
        while(r.hasNext()){
            if(result.size()>=maximum)throw new IOException("Too many command entries");
            Set<String> fields=new HashSet<>();String context=null,command=null,ack=null,type=null;r.beginObject();
            while(r.hasNext()){String field=r.nextName();if(!fields.add(field))throw new IOException("Duplicate command field");if(r.peek()!=JsonToken.STRING)throw new IOException("Command values must be strings");String value=r.nextString();switch(field){case "type"->type=value;case "context"->context=value;case "command"->command=value;case "acknowledgement"->ack=value;default->throw new IOException("Unknown command field");}}
            r.endObject();if(!"COMMAND".equals(type)||!"MANUAL_REVIEW".equals(ack)||context==null||command==null)throw new IOException("COMMAND requires context, command and MANUAL_REVIEW acknowledgement");
            result.add(new CommandReward(CommandReward.Context.valueOf(context),command));
        }r.endArray();return List.copyOf(result);
    }
    static CommandPolicy policy(JsonReader r)throws IOException{
        Set<String> fields=new HashSet<>();Boolean enabled=null;List<CommandReward> allowed=null;r.beginObject();
        while(r.hasNext()){String field=r.nextName();if(!fields.add(field))throw new IOException("Duplicate command policy field");switch(field){case "enabled"->{if(r.peek()!=JsonToken.BOOLEAN)throw new IOException("enabled must be boolean");enabled=r.nextBoolean();}case "allowed"->allowed=read(r,32);default->throw new IOException("Unknown command policy field");}}
        r.endObject();if(enabled==null||allowed==null)throw new IOException("Command policy requires enabled and allowed");return new CommandPolicy(enabled,allowed);
    }
}
