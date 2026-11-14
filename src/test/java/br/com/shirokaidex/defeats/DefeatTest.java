package br.com.shirokaidex.defeats;
import br.com.shirokaidex.database.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
class DefeatTest {
    @TempDir Path directory;
    @Test void duplicateBattlePokemonCannotCreditTwiceOrChangeOwnerAcrossRestart()throws Exception {
        var file=directory.resolve("defeat.sqlite");var player=UUID.randomUUID();var pokemon=UUID.randomUUID();var battle=UUID.randomUUID();var now=Instant.now();
        try(var db=new Database(file)){db.initialize().get(10,TimeUnit.SECONDS);db.recordLogin(player,"Winner",now).get(10,TimeUnit.SECONDS);assertTrue(db.recordDefeat(new DefeatRecord(battle,pokemon,player,"cobblemon:pikachu",now)).get(10,TimeUnit.SECONDS));assertFalse(db.recordDefeat(new DefeatRecord(battle,pokemon,UUID.randomUUID(),"cobblemon:pikachu",now)).get(10,TimeUnit.SECONDS));assertEquals(1,db.defeatRanking().get(10,TimeUnit.SECONDS).entries().getFirst().total());}
        try(var db=new Database(file)){db.initialize().get(10,TimeUnit.SECONDS);assertFalse(db.recordDefeat(new DefeatRecord(battle,pokemon,player,"cobblemon:pikachu",now)).get(10,TimeUnit.SECONDS));assertEquals("Winner",db.defeatRanking().get(10,TimeUnit.SECONDS).entries().getFirst().name());db.rankingPrivacy(player,true).get(10,TimeUnit.SECONDS);assertTrue(db.defeatRanking().get(10,TimeUnit.SECONDS).entries().isEmpty());db.rankingPrivacy(player,false).get(10,TimeUnit.SECONDS);assertEquals(1,db.defeatRanking().get(10,TimeUnit.SECONDS).entries().getFirst().total());}
    }
}
