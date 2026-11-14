package br.com.shirokaidex.profiles;

import br.com.shirokaidex.captures.*;
import br.com.shirokaidex.database.Database;
import br.com.shirokaidex.config.Settings;
import br.com.shirokaidex.pokedex.CollectionDefinition;
import br.com.shirokaidex.rankings.CaptureRanking;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfileMigrationTest {
    @TempDir Path directory;
    private static <T>T await(CompletableFuture<T> f)throws Exception{return f.get(10,TimeUnit.SECONDS);}
    private CaptureRecord record(UUID player,String form,boolean shiny,boolean verified){return new CaptureRecord(UUID.randomUUID(),UUID.randomUUID(),player,"mod:a",form,shiny,Set.of(),Set.of(),Set.of(),"a".repeat(64),Instant.parse("2026-11-14T00:00:00Z"),"minecraft:overworld",0,64,0,verified?CaptureRecord.Origin.NATIVE_PLAYER_SPAWN:CaptureRecord.Origin.UNKNOWN);}
    @Test void migrationThirteenUsesOnlySurvivingVerifiedDetailsAndPermanentShinyProofs()throws Exception{
        var file=directory.resolve("thirteen.sqlite");var player=UUID.randomUUID();var normal=record(player,"normal",false,true);var alola=record(player,"alola",true,true);var unknown=record(player,"galar",true,false);
        try(var db=new Database(file)){await(db.initialize());await(db.recordCapture(normal));await(db.recordCapture(alola));await(db.recordCapture(unknown));}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()){
            q.executeUpdate("DELETE FROM capture_events WHERE form='alola'");
            for(String table:List.of("player_medal_favorites","player_achievements","achievement_audit","achievement_definitions","profile_collection_points","player_stat_privacy","player_preferences","captured_forms"))q.execute("DROP TABLE "+table);
            q.execute("DELETE FROM schema_migrations WHERE version=14");q.execute("PRAGMA user_version=13");
        }
        for(int i=0;i<2;i++)try(var db=new Database(file)){await(db.initialize());assertFalse(await(db.recordCapture(alola)));assertEquals(2,await(db.captureSummary(player,normal.capturedAt(),ZoneId.of("UTC"))).total());}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var rows=q.executeQuery("SELECT form,shiny FROM captured_forms ORDER BY form")){var forms=new LinkedHashMap<String,Integer>();while(rows.next())forms.put(rows.getString(1),rows.getInt(2));assertEquals(Map.of("normal",0,"alola",1),forms);}
    }
    @Test void defaultRankingsSupportAllSixtyFourCollectionsAndMaximumLengthIds(){
        var definitions=new ArrayList<CollectionDefinition>();for(int i=0;i<64;i++){String id="a".repeat(45)+String.format("%03d",i);definitions.add(new CollectionDefinition(id,"Collection "+i,"","minecraft:book",CollectionDefinition.Mode.OWNED,CollectionDefinition.Criteria.all(),""));}
        var settings=new Settings(1,"pt_br","Dex",definitions);assertEquals(67,settings.rankings().size());assertEquals(67,settings.rankings().stream().map(CaptureRanking::id).distinct().count());assertTrue(settings.rankings().stream().allMatch(r->r.id().length()<=48));
    }
}
