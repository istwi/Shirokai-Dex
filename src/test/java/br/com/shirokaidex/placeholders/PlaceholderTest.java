package br.com.shirokaidex.placeholders;

import br.com.shirokaidex.pokedex.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class PlaceholderTest {
    private static ProgressCalculator.CollectionProgress known(int total, int registered, int seen) {
        return new ProgressCalculator.CollectionProgress(DexProgress.known(total, registered),seen);
    }
    @Test void knownMetricsPreserveCountsAndTruncatedLocalePercent() {
        var value=known(3,1,2);
        assertEquals("33,33%",PlaceholderValues.format("percent",value,"pt_br"));
        assertEquals("33.33%",PlaceholderValues.format("percent",value,"en_us"));
        assertEquals("1",PlaceholderValues.format("registered",value,"pt_br"));
        assertEquals("2",PlaceholderValues.format("seen",value,"pt_br"));
        assertEquals("3",PlaceholderValues.format("total",value,"pt_br"));
        assertEquals("2",PlaceholderValues.format("remaining",value,"pt_br"));
    }
    @Test void unsupportedCaptureEvidenceIsNeverConvertedToZero() {
        var value=new ProgressCalculator.CollectionProgress(DexProgress.unavailable(100),3);
        for(var metric:List.of("registered","remaining","percent"))assertEquals("N/D",PlaceholderValues.format(metric,value,"pt_br"));
        assertEquals("100",PlaceholderValues.format("total",value,"pt_br"));
        assertEquals("3",PlaceholderValues.format("seen",value,"pt_br"));
    }
    @Test void emptyAndMissingDataRemainDistinct() {
        assertEquals("N/D",PlaceholderValues.format("percent",known(0,0,0),"pt_br"));
        assertEquals("0",PlaceholderValues.format("registered",known(0,0,0),"pt_br"));
        for(String metric:PlaceholderValues.METRICS)assertEquals("N/D",PlaceholderValues.format(metric,null,"pt_br"));
        assertEquals("N/D",PlaceholderValues.format("captures",known(5,1,2),"pt_br"));
    }
    @Test void argumentsCannotEmbedOtherPlaceholdersOrCommands() {
        assertEquals("national",PlaceholderValues.collection(null)); assertEquals("national",PlaceholderValues.collection(" "));
        assertEquals("custom_2",PlaceholderValues.collection(" custom_2 "));
        for(String arg:List.of("national %player:name%","../x","/op player","NAME","x".repeat(49)))assertNull(PlaceholderValues.collection(arg));
    }
    @Test void pendingRefreshesCoalesceAndSnapshotsAreImmutable() {
        var cache=new PlaceholderCache();var id=UUID.randomUUID();var revision=new Object();
        assertTrue(cache.read(id,revision,0).isEmpty());cache.read(id,revision,0);
        assertEquals(id,cache.poll());assertNull(cache.poll());
        var source=new HashMap<String,ProgressCalculator.CollectionProgress>();source.put("national",known(5,1,2));
        cache.publish(id,revision,0,source);source.clear();
        assertEquals(1,cache.read(id,revision,1).size());assertNull(cache.poll());
        assertThrows(UnsupportedOperationException.class,()->cache.read(id,revision,1).clear());
    }
    @Test void expiredReadReturnsOnlySameRevisionWhileQueueingRefresh() {
        var cache=new PlaceholderCache();var id=UUID.randomUUID();var revision=new Object();
        cache.publish(id,revision,0,Map.of("national",known(5,1,2)));
        assertEquals(1,cache.read(id,revision,TimeUnit.SECONDS.toNanos(6)).size());assertEquals(id,cache.poll());
        assertTrue(cache.read(id,new Object(),TimeUnit.SECONDS.toNanos(6)).isEmpty());assertEquals(id,cache.poll());
    }
    @Test void invalidationLogoutAndShutdownDiscardPrivateSnapshots() {
        var cache=new PlaceholderCache();var id=UUID.randomUUID();var revision=new Object();
        cache.publish(id,revision,0,Map.of("locked",known(3,2,2)));cache.invalidate(id);
        assertTrue(cache.read(id,revision,1).isEmpty());cache.remove(id);assertNull(cache.poll());
        cache.invalidate(id);cache.clear();assertNull(cache.poll());assertTrue(cache.read(id,revision,2).isEmpty());
    }
    @Test void failedRefreshCanBackOffWithoutRetryingEveryExpansion() {
        var cache=new PlaceholderCache();var id=UUID.randomUUID();var revision=new Object();
        cache.publish(id,revision,0,Map.of());cache.read(id,revision,1);assertNull(cache.poll());
        cache.read(id,revision,TimeUnit.SECONDS.toNanos(5));assertEquals(id,cache.poll());
    }
    @Test void conflictsAndLaterReplacementsArePreservedOnCleanup() {
        var registry=new HashMap<String,Object>();var external=new Object();registry.put("conflict",external);
        var owned=new OwnedRegistrations<String,Object>(registry::get,registry::put,registry::remove);
        assertFalse(owned.install("conflict",new Object()));assertSame(external,registry.get("conflict"));
        var handler=new Object();assertTrue(owned.install("ours",handler));assertTrue(owned.owns("ours"));
        assertTrue(owned.install("replaced",handler));registry.put("replaced",external);assertFalse(owned.owns("replaced"));
        owned.close();assertFalse(registry.containsKey("ours"));assertSame(external,registry.get("replaced"));assertSame(external,registry.get("conflict"));
    }
}
