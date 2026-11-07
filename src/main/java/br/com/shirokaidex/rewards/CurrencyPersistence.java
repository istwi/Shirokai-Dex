package br.com.shirokaidex.rewards;

import com.google.gson.stream.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

/** Read-only verification for the audited native JSON provider, which suppresses save errors. */
public final class CurrencyPersistence {
    private static final ScheduledExecutorService IO=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"shirokaidex-currency-readback");t.setDaemon(true);return t;});
    private CurrencyPersistence() { }
    public static void read(Path file,String currency,BigDecimal expected)throws IOException {
        try(var input=Files.newInputStream(file)){
            byte[] bytes=input.readNBytes(1048577);if(bytes.length>1048576)throw new IOException("Currency file exceeds readback limit");
            try(var reader=new JsonReader(new StringReader(new String(bytes,StandardCharsets.UTF_8)))){
                reader.setLenient(false);Set<String> keys=new HashSet<>();BigDecimal actual=null;
                reader.beginObject();while(reader.hasNext()){
                    String key=reader.nextName();if(!keys.add(key))throw new IOException("Duplicate currency balance");
                    if(key.equals(currency)){if(reader.peek()!=JsonToken.NUMBER)throw new IOException("Currency balance is not numeric");actual=new BigDecimal(reader.nextString());}else reader.skipValue();
                }reader.endObject();if(reader.peek()!=JsonToken.END_DOCUMENT||actual==null||actual.compareTo(expected)!=0)throw new IOException("Saved balance differs from acknowledged balance");
            }catch(IllegalArgumentException|IllegalStateException invalid){throw new IOException("Invalid native currency file",invalid);}
        }
    }
    public static CompletableFuture<Void> verify(Path file,String currency,BigDecimal expected){
        var result=new CompletableFuture<Void>();poll(result,file,currency,expected,40);return result;
    }
    private static void poll(CompletableFuture<Void> result,Path file,String currency,BigDecimal expected,int remaining){
        IO.schedule(()->{if(result.isDone())return;try{read(file,currency,expected);result.complete(null);}catch(IOException failure){if(remaining==1)result.completeExceptionally(new IOException("Impactor JSON readback failed; manual review required"));else poll(result,file,currency,expected,remaining-1);}},remaining==40?0:50,TimeUnit.MILLISECONDS);
    }
    public static boolean roundTrips(BigDecimal value){double number=value.doubleValue();return Double.isFinite(number)&&BigDecimal.valueOf(number).compareTo(value)==0;}
}
