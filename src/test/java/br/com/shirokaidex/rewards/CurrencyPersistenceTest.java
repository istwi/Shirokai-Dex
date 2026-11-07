package br.com.shirokaidex.rewards;

import java.math.BigDecimal;
import java.nio.file.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CurrencyPersistenceTest {
    @TempDir Path directory;
    @Test void readsExactCurrencyWithoutFallingBackOrAcceptingAmbiguousData()throws Exception{
        var file=directory.resolve("account.conf");Files.writeString(file,"{\"impactor:dollars\":12.50,\"impactor:tokens\":3}");
        CurrencyPersistence.read(file,"impactor:dollars",new BigDecimal("12.5"));
        assertThrows(java.io.IOException.class,()->CurrencyPersistence.read(file,"custom:missing",BigDecimal.ZERO));
        assertThrows(java.io.IOException.class,()->CurrencyPersistence.read(file,"impactor:dollars",new BigDecimal("12.51")));
        Files.writeString(file,"{\"impactor:dollars\":12.5,\"impactor:dollars\":12.5}");
        assertThrows(java.io.IOException.class,()->CurrencyPersistence.read(file,"impactor:dollars",new BigDecimal("12.5")));
        Files.writeString(file,"{\"impactor:dollars\":\"12.5\"}");assertThrows(java.io.IOException.class,()->CurrencyPersistence.read(file,"impactor:dollars",new BigDecimal("12.5")));
    }
    @Test void rejectsNativeDoublePrecisionLossBeforeDeposit(){
        assertTrue(CurrencyPersistence.roundTrips(new BigDecimal("12.50")));assertTrue(CurrencyPersistence.roundTrips(new BigDecimal("500")));
        assertFalse(CurrencyPersistence.roundTrips(new BigDecimal("1000000000000000.01")));
    }
    @Test void missingOrUnwrittenBalanceNeverAcknowledgesProviderSuccess()throws Exception{
        var missing=directory.resolve("missing.conf");assertThrows(ExecutionException.class,()->CurrencyPersistence.verify(missing,"impactor:dollars",BigDecimal.ONE).get(5,TimeUnit.SECONDS));
        Files.writeString(missing,"{\"impactor:dollars\":1}");CurrencyPersistence.verify(missing,"impactor:dollars",BigDecimal.ONE).get(5,TimeUnit.SECONDS);
    }
}
