package br.com.shirokaidex.competitions;

import br.com.shirokaidex.captures.*;
import br.com.shirokaidex.categories.CategoryRules;
import br.com.shirokaidex.config.*;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.rankings.CaptureRanking;
import br.com.shirokaidex.rewards.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CompetitionAwardsTest {
    @TempDir Path directory;
    private static final Instant AT=Instant.parse("2026-11-14T00:00:00Z");
    private static <T>T await(CompletableFuture<T> f)throws Exception{return f.get(10,TimeUnit.SECONDS);}
    private Settings settings(int days)throws Exception{
        var policy=new PrizePolicy(true,List.of(new PrizePolicy.Band(1,"winner",1)),1,1,false,days);
        var definition=new CompetitionDefinition("prizes","Prizes",CompetitionDefinition.Cadence.CUSTOM,ZoneId.of("UTC"),AT,AT.plusSeconds(60),CaptureRanking.Metric.TOTAL,"",CompetitionDefinition.CategoryMode.SNAPSHOT,CompetitionDefinition.Visibility.PUBLIC,0,true,CompetitionDefinition.ReviewMode.LOG_ONLY,0,"",policy);
        return ConfigManager.parse("{\"schema_version\":1,\"language\":\"pt_br\",\"menu_title\":\"Dex\",\"rewards\":[{\"id\":\"winner\",\"collection\":\"national\",\"minimum_registered\":1,\"items\":[{\"id\":\"minecraft:diamond\",\"count\":2}]}],\"competitions\":["+CompetitionJson.write(definition)+"]}");
    }
    private void capture(Database db,UUID player,Settings settings)throws Exception{
        var r=new CaptureRecord(UUID.randomUUID(),UUID.randomUUID(),player,"cobblemon:pikachu","normal",false,Set.of(),Set.of(),Set.of(),CategoryRules.of(settings.categories()).fingerprint(),AT.plusSeconds(2),"minecraft:overworld",0,64,0,CaptureRecord.Origin.UNKNOWN);var entity=UUID.randomUUID();await(db.recordNativeSpawnProof(new NativeSpawnProof(r.pokemon(),entity,r.dimension(),AT,NativeSpawnProof.Source.PREHIT)));await(db.recordObservedCapture(new CaptureObservation(r,entity,CaptureObservation.Kind.LIVE_NATIVE),settings));
    }
    @Test void officialAwardsAreImmutableIdempotentPrivateAndExpireFromRegistrationAcrossRestart()throws Exception{
        var file=directory.resolve("awards.sqlite");var settings=settings(7);var player=UUID.randomUUID();String id;String awardId;
        try(var db=new Database(file)){
            await(db.initialize());await(db.synchronizeCompetitions(settings,AT));id=await(db.competitionEditions(false)).getFirst().id();await(db.enrollCompetition(id,player,AT));capture(db,player,settings);assertTrue(await(db.competitionAwards(player,0)).isEmpty());
            var registered=AT.plusSeconds(61);await(db.synchronizeCompetitions(settings,registered));var award=await(db.competitionAwards(player,0)).getFirst();awardId=award.id();assertEquals(2,award.plan().items().getFirst().count());assertEquals(registered,award.assignedAt());assertTrue(award.available(registered.plus(Duration.ofDays(7)).minusMillis(1)));assertFalse(award.available(registered.plus(Duration.ofDays(7))));assertEquals(award.id(),award.plan().id());assertTrue(await(db.competitionAward(award.id(),UUID.randomUUID())).isEmpty());
            // Config edits after official registration cannot rewrite the archived package.
            await(db.synchronizeCompetitions(Settings.defaults(),AT.plusSeconds(62)));assertEquals(1,await(db.competitionAwards(player,0)).size());assertFalse(await(db.finalizeCompetition(id,player.toString(),"No duplicate official prize",AT.plusSeconds(62))));
        }
        try(var db=new Database(file)){await(db.initialize());await(db.synchronizeCompetitions(settings,AT.plus(Duration.ofDays(10))));var award=await(db.competitionAward(awardId,player)).orElseThrow();assertFalse(award.available(AT.plus(Duration.ofDays(10))));assertEquals(1,await(db.competitionAwards(player,0)).size());}
    }
    @Test void hiddenWinnerCannotEarnNewAwardButAlreadyAssignedAwardSurvivesLaterPrivacyChange()throws Exception{
        var settings=settings(0);var player=UUID.randomUUID();
        try(var db=new Database(directory.resolve("hidden.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));var id=await(db.competitionEditions(false)).getFirst().id();await(db.enrollCompetition(id,player,AT));capture(db,player,settings);await(db.rankingPrivacy(player,true));await(db.synchronizeCompetitions(settings,AT.plusSeconds(61)));assertTrue(await(db.competitionAwards(player,0)).isEmpty());}
        try(var db=new Database(directory.resolve("earned.sqlite"))){await(db.initialize());await(db.synchronizeCompetitions(settings,AT));var id=await(db.competitionEditions(false)).getFirst().id();await(db.enrollCompetition(id,player,AT));capture(db,player,settings);await(db.synchronizeCompetitions(settings,AT.plusSeconds(61)));await(db.rankingPrivacy(player,true));var award=await(db.competitionAwards(player,0)).getFirst();assertTrue(award.available(AT.plus(Duration.ofDays(365))));}
    }
    @Test void internalRewardSnapshotValidatesRecordsAndRejectsUnrecognizedOrCorruptedData(){
        var pokemon=new PokemonReward("cobblemon:pikachu","",5,true,"","","","","",List.of(),Map.of(),Map.of(),"");
        var plan=new RewardPlan("winner","national",1,null,List.of(new RewardPlan.Item("minecraft:diamond",2)),false,List.of(pokemon),List.of(new CurrencyReward("impactor:dollars",new java.math.BigDecimal("12.50"))));
        assertEquals(plan,RewardSnapshot.decode(RewardSnapshot.encode(plan)));
        assertThrows(IllegalArgumentException.class,()->RewardSnapshot.decode(RewardSnapshot.encode(plan).replace("\"format_version\":1","\"format_version\":2")));
        var raw=JsonParser.parseString(RewardSnapshot.encode(plan)).getAsJsonObject();raw.getAsJsonObject("plan").addProperty("extra","ignored");assertThrows(IllegalArgumentException.class,()->RewardSnapshot.decode(raw.toString()));
        assertThrows(RuntimeException.class,()->RewardSnapshot.decode("{\"format_version\":1,\"plan\":{}}"));
    }
}
