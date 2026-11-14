package br.com.shirokaidex.notifications;

import br.com.shirokaidex.config.*;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.profiles.ProfileProgress;
import br.com.shirokaidex.rewards.*;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {
    @TempDir Path directory;
    private static final Instant NOW=Instant.parse("2026-11-14T00:00:00Z");
    private static <T>T await(CompletableFuture<T> future)throws Exception{return future.get(10,TimeUnit.SECONDS);}
    private static RewardPlan plan(){return new RewardPlan("first","national",1,null,List.of(new RewardPlan.Item("minecraft:diamond",1)));}
    @Test void unlockEventIsAtomicDeduplicatedAndConsumedBeforeRestart()throws Exception{
        var file=directory.resolve("outbox.sqlite");var player=UUID.randomUUID();
        try(var db=new Database(file)){await(db.initialize());var unlock=new RewardUnlock(plan(),1,100);await(db.rewardUnlocks(player,List.of(unlock)));await(db.rewardUnlocks(player,List.of(unlock)));var notices=await(db.claimNotifications(player.toString(),NOW));assertEquals(1,notices.size());assertEquals(NotificationPolicy.Event.REWARD_UNLOCKED,notices.getFirst().event());assertTrue(await(db.claimNotifications(player.toString(),NOW)).isEmpty());}
        try(var db=new Database(file)){await(db.initialize());assertTrue(await(db.claimNotifications(player.toString(),NOW)).isEmpty());await(db.rewardUnlocks(player,List.of(new RewardUnlock(plan(),1,100))));assertTrue(await(db.claimNotifications(player.toString(),NOW)).isEmpty());}
    }
    @Test void optOutConsumesQueuedEventsAndDoesNotReplayWhenEnabled()throws Exception{
        var player=UUID.randomUUID();try(var db=new Database(directory.resolve("optout.sqlite"))){await(db.initialize());await(db.notificationPreference(player,false));await(db.rewardUnlocks(player,List.of(new RewardUnlock(plan(),1,100))));assertTrue(await(db.claimNotifications(player.toString(),NOW)).isEmpty());await(db.notificationPreference(player,true));assertTrue(await(db.claimNotifications(player.toString(),NOW)).isEmpty());}
    }
    @Test void claimsAreBoundedAndTransactionRollbackDoesNotLeavePhantomNotices()throws Exception{
        var file=directory.resolve("bounded.sqlite");var player=UUID.randomUUID();try(var db=new Database(file)){await(db.initialize());}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file)){
            c.setAutoCommit(false);NotificationOutbox.enqueue(c,"rolled-back",player.toString(),NotificationPolicy.Event.PROGRESS,"fake",NOW);c.rollback();c.setAutoCommit(true);
            for(int i=0;i<20;i++)NotificationOutbox.enqueue(c,"event:"+i,player.toString(),NotificationPolicy.Event.PROGRESS,"progress",NOW);
        }
        try(var db=new Database(file)){await(db.initialize());assertEquals(8,await(db.claimNotifications(player.toString(),NOW)).size());assertEquals(8,await(db.claimNotifications(player.toString(),NOW)).size());assertEquals(4,await(db.claimNotifications(player.toString(),NOW)).size());assertTrue(await(db.claimNotifications(player.toString(),NOW)).isEmpty());}
    }
    private static ProfileProgress.Proof proof(int registered,String version){return new ProfileProgress.Proof("national",version,registered,100,NOW);}
    @Test void progressEstablishesBaselineAndDoesNotRepeatAfterRegressionOrCatalogChange()throws Exception {
        var file=directory.resolve("progress.sqlite");var player=UUID.randomUUID();try(var db=new Database(file)){await(db.initialize());await(db.observeProfile(player,List.of(proof(24,"a".repeat(64))),Settings.defaults(),NOW));assertTrue(await(db.claimNotifications(player.toString(),NOW)).isEmpty());await(db.observeProfile(player,List.of(proof(51,"a".repeat(64))),Settings.defaults(),NOW));assertEquals(2,await(db.claimNotifications(player.toString(),NOW)).size());await(db.observeProfile(player,List.of(proof(0,"a".repeat(64))),Settings.defaults(),NOW));await(db.observeProfile(player,List.of(proof(51,"a".repeat(64))),Settings.defaults(),NOW));assertTrue(await(db.claimNotifications(player.toString(),NOW)).isEmpty());await(db.observeProfile(player,List.of(proof(100,"b".repeat(64))),Settings.defaults(),NOW));assertTrue(await(db.claimNotifications(player.toString(),NOW)).isEmpty());}
    }
    @Test void competitionLifecycleCreatesOnePublicBroadcastCandidateAndIndividualReceiptsPerEvent()throws Exception {
        String config="""
          {"schema_version":1,"language":"pt_br","menu_title":"Dex","competitions":[{"id":"short","name":"Short","cadence":"CUSTOM","start":"2026-11-14T00:00:00Z","end":"2026-11-14T00:01:00Z","metric":"TOTAL","result_review":{"enabled":false}}]}
          """;
        var settings=ConfigManager.parse(config);var player=UUID.randomUUID();try(var db=new Database(directory.resolve("lifecycle.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(settings,NOW));var edition=await(db.competitionEditions(false)).getFirst();assertTrue(await(db.enrollCompetition(edition.id(),player,NOW)));assertFalse(await(db.enrollCompetition(edition.id(),player,NOW)));var joined=await(db.claimNotifications(player.toString(),NOW));assertEquals(1,joined.size());assertEquals(NotificationPolicy.Event.COMPETITION_JOINED,joined.getFirst().event());await(db.synchronizeCompetitions(settings,NOW.plusSeconds(61)));var individual=await(db.claimNotifications(player.toString(),NOW));assertEquals(Set.of(NotificationPolicy.Event.COMPETITION_ENDED,NotificationPolicy.Event.RESULTS),individual.stream().map(NotificationOutbox.Notice::event).collect(java.util.stream.Collectors.toSet()));assertTrue(individual.stream().allMatch(NotificationOutbox.Notice::broadcastEligible));assertEquals(2,await(db.claimNotifications(NotificationOutbox.GLOBAL,NOW)).size());await(db.synchronizeCompetitions(settings,NOW.plusSeconds(62)));assertTrue(await(db.claimNotifications(NotificationOutbox.GLOBAL,NOW)).isEmpty());}
    }
    @Test void configurationKeepsGlobalOffAndRejectsPrivateBroadcastsInvalidTypesAndChannels()throws Exception {
        assertTrue(NotificationPolicy.defaults().rules().values().stream().noneMatch(NotificationPolicy.Rule::global));
        String prefix="{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"notifications\":";
        var settings=ConfigManager.parse(prefix+"{\"results\":{\"global\":true,\"channels\":[\"CHAT\",\"TITLE\",\"SOUND\"]}}}");assertTrue(settings.notifications().rules().get(NotificationPolicy.Event.RESULTS).global());
        for(String invalid:List.of("{\"achievement\":{\"global\":true}}","{\"results\":{\"channels\":[]}}","{\"results\":{\"channels\":[\"CHAT\",\"CHAT\"]}}","{\"results\":{\"enabled\":1}}","{\"results\":{\"command\":\"op someone\"}}","{\"results\":{\"sound\":\"bad\"}}","{\"unknown\":{}}"))assertThrows(java.io.IOException.class,()->ConfigManager.parse(prefix+invalid+"}"));
    }
}
