package br.com.shirokaidex.categories;

import br.com.shirokaidex.config.CategoriesCodec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CategoryRulesTest {
    @Test void orderingAndOmittedEmptyFieldsHaveStableFingerprint() throws Exception {
        var first = CategoryRules.of(CategoriesCodec.parse("""
            [{"id":"z","criteria":{"types":["water","fire"]}}, {"id":"a","all_of":["z"]}]
            """));
        var second = CategoryRules.of(CategoriesCodec.parse("""
            [{"all_of":["z"],"id":"a","any_of":[]}, {"criteria":{"types":["fire","water"],"forms":[]},"id":"z"}]
            """));
        assertEquals(first, second); assertEquals(64, first.fingerprint().length());
        var changed = CategoryRules.of(CategoriesCodec.parse("[{\"id\":\"z\",\"criteria\":{\"types\":[\"fire\"]}},{\"id\":\"a\",\"all_of\":[\"z\"]}]"));
        assertNotEquals(first.fingerprint(), changed.fingerprint());
    }
    @Test void snapshotRejectsContentThatDoesNotMatchHash() throws Exception {
        var value = CategoryRules.of(CategoriesCodec.parse("[]"));
        assertThrows(IllegalArgumentException.class, () -> new CategoryRules(1, value.json()+" ", value.fingerprint()));
        assertThrows(IllegalArgumentException.class, () -> new CategoryRules(2, value.json(), value.fingerprint()));
    }
}
