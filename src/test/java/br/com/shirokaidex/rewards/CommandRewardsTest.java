package br.com.shirokaidex.rewards;

import br.com.shirokaidex.config.ConfigManager;
import br.com.shirokaidex.database.Database;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CommandRewardsTest {
    @TempDir Path directory;
    private static CommandReward command(){return new CommandReward(CommandReward.Context.CONSOLE,"scoreboard players add {player} points 1");}
    private static RewardPlan plan(){return new RewardPlan("command","national",1,null,List.of(),false,List.of(),List.of(),List.of(command()));}
    private static JsonObject config(){
        var root=new JsonObject();root.addProperty("schema_version",1);root.addProperty("language","pt_br");root.addProperty("menu_title","Dex");
        var policy=new JsonObject();policy.addProperty("enabled",true);var allowed=new JsonArray();allowed.add(command().json());policy.add("allowed",allowed);root.add("command_policy",policy);
        var rewards=new JsonArray();var reward=JsonParser.parseString(plan().json()).getAsJsonObject();reward.remove("format_version");rewards.add(reward);root.add("rewards",rewards);return root;
    }
    @Test void exactTemplateAndContextRequireExplicitAuthorization()throws Exception{
        var parsed=ConfigManager.parse(config().toString());assertTrue(parsed.commandPolicy().authorizes(command()));assertFalse(parsed.commandPolicy().authorizes(new CommandReward(CommandReward.Context.PLAYER,command().command())));
        assertFalse(parsed.commandPolicy().authorizes(new CommandReward(CommandReward.Context.CONSOLE,command().command()+"0")));
        var missing=config();missing.remove("command_policy");assertFalse(ConfigManager.parse(missing.toString()).commandPolicy().authorizes(command()));
        var mismatch=config();mismatch.getAsJsonObject("command_policy").getAsJsonArray("allowed").get(0).getAsJsonObject().addProperty("context","PLAYER");assertThrows(java.io.IOException.class,()->ConfigManager.parse(mismatch.toString()));
    }
    @Test void disablingPolicyRetainsDefinitionsButRevokesAuthorization()throws Exception{
        var value=config();value.getAsJsonObject("command_policy").addProperty("enabled",false);var parsed=ConfigManager.parse(value.toString());assertEquals(1,parsed.rewards().size());assertFalse(parsed.commandPolicy().authorizes(command()));
    }
    @Test void frozenRecipientAndTemplateSurviveReuseButCannotBeRetargeted(){
        var uuid=UUID.randomUUID();var prepared=plan().withCommands(List.of(command().freeze("DexCommand",uuid)));
        assertEquals("scoreboard players add DexCommand points 1",prepared.commands().getFirst().snapshot());assertEquals(plan(),prepared.definition());assertEquals(prepared,plan().reuse(prepared.json()));assertEquals(1,prepared.steps());
        var changed=new RewardPlan("command","national",1,null,List.of(),false,List.of(),List.of(),List.of(new CommandReward(CommandReward.Context.PLAYER,command().command())));assertThrows(IllegalArgumentException.class,()->changed.reuse(prepared.json()));
        assertThrows(IllegalArgumentException.class,()->command().freeze("@a run op",uuid));
        assertEquals("say "+uuid,new CommandReward(CommandReward.Context.PLAYER,"say {uuid}").freeze("DexCommand",uuid).snapshot());
    }
    @Test void rejectsWrappersPrivilegeCommandsUnknownPlaceholdersAndControlCharacters(){
        for(String value:List.of("execute as {player} run give @s diamond","minecraft:op {player}","crates givekey {player} shiny1 1","function test:{player}","schedule function {player} 1t","lp user {player} permission set * true","say {player}\nstop","/say {player}","say {args}","say no_recipient"))assertThrows(IllegalArgumentException.class,()->new CommandReward(CommandReward.Context.CONSOLE,value),value);
    }
    @Test void commandsCannotHideInsideMixedOrMultipleActionRewards(){
        assertThrows(IllegalArgumentException.class,()->new RewardPlan("c","national",1,null,List.of(new RewardPlan.Item("minecraft:paper",1)),false,List.of(),List.of(),List.of(command())));
        assertThrows(IllegalArgumentException.class,()->plan().withCommands(List.of(command(),command())));
        assertThrows(IllegalArgumentException.class,()->new CommandPolicy(true,List.of(command(),command())));
    }
    @Test void parsingRejectsMissingReviewAcknowledgementRuntimeSnapshotsAndDuplicateFields(){
        var noAck=config();noAck.getAsJsonArray("rewards").get(0).getAsJsonObject().getAsJsonArray("commands").get(0).getAsJsonObject().remove("acknowledgement");assertThrows(java.io.IOException.class,()->ConfigManager.parse(noAck.toString()));
        var snapshot=config();snapshot.getAsJsonObject("command_policy").getAsJsonArray("allowed").get(0).getAsJsonObject().addProperty("snapshot","say injected");assertThrows(java.io.IOException.class,()->ConfigManager.parse(snapshot.toString()));
        var duplicate=config().toString().replace("\"enabled\":true","\"enabled\":true,\"enabled\":false");assertThrows(java.io.IOException.class,()->ConfigManager.parse(duplicate));
    }
    @Test void invalidPolicyReloadPreservesTheWholeCurrentConfiguration()throws Exception{
        java.nio.file.Files.writeString(directory.resolve("config.json"),config().toString());var manager=new ConfigManager(directory);manager.initialize();var before=manager.current();
        var invalid=config();invalid.getAsJsonObject("command_policy").add("allowed",new JsonArray());java.nio.file.Files.writeString(directory.resolve("config.json"),invalid.toString());assertThrows(java.io.IOException.class,manager::reload);assertEquals(before,manager.current());
    }
    @Test void dispatchIntentIsPersistedBeforeEffectAndReviewNeverAutomaticallyRetries()throws Exception{
        UUID player=UUID.randomUUID();var frozen=plan().withCommands(List.of(command().freeze("DexCommand",player)));var effects=new AtomicInteger();UUID operation;
        try(var database=new Database(directory.resolve("commands.sqlite"))){
            database.initialize().join();var delivery=new RewardDelivery(database);
            RewardDelivery.Adapter adapter=new RewardDelivery.Adapter(){
                public CompletableFuture<Boolean> preflight(RewardPlan plan,int from){return CompletableFuture.completedFuture(true);}
                public CompletableFuture<String> deliver(RewardPlan plan,int step){
                    return database.rewardForPlayer(player,plan.id()).thenCompose(value->{var reserved=value.orElseThrow();assertEquals(RewardJournal.State.PROCESSING,reserved.state());assertEquals(frozen.json(),reserved.plan());
                    try(var c=java.sql.DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("commands.sqlite"));var query=c.prepareStatement("SELECT state FROM reward_steps WHERE operation_id=? AND step_index=0")){
                        query.setString(1,reserved.id().toString());try(var row=query.executeQuery()){assertTrue(row.next());assertEquals("INTENT",row.getString(1));}
                    }catch(java.sql.SQLException failure){throw new CompletionException(failure);}
                    effects.incrementAndGet();return CompletableFuture.failedFuture(new RewardDelivery.ManualReview());});
                }
            };
            assertEquals(RewardDelivery.Result.NEEDS_REVIEW,delivery.claim(player,frozen,adapter).join());assertEquals(1,effects.get());assertEquals(RewardDelivery.Result.BLOCKED,delivery.claim(player,frozen,adapter).join());assertEquals(1,effects.get());operation=database.rewardForPlayer(player,frozen.id()).join().orElseThrow().id();
        }
        try(var restarted=new Database(directory.resolve("commands.sqlite"))){restarted.initialize().join();assertEquals(RewardJournal.State.NEEDS_REVIEW,restarted.rewardOperation(operation).join().orElseThrow().state());}
    }
}
