package de.ids_mannheim.korap.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Message;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.StatusCodes;

public class TestInvalidVcId {

    private KrillIndex ki;
    
    public TestInvalidVcId () throws IOException {
        ki = TestVirtualCorpusCache.createIndex();
    }

    @Test
    public void testStoreVcNotExist () {
        String vcId = "snx";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            VirtualCorpusCache.store(vcId, ki);
        });
        assertEquals("de.ids_mannheim.korap.util.QueryException: "
                + "VC is not found queries/collections/named-vcs/snx.jsonld",
                ex.getMessage());
    }


    @Test
    public void testReferVcNotExist () throws IOException {
        String file = "/queries/collections/vc-ref/query-with-unknown-vc.jsonld";
        InputStream is = getClass().getResourceAsStream(file);
        String queryRefJson = IOUtils.toString(is, "utf-8");

        Krill krill = new Krill(queryRefJson);
        Result result = krill.apply(ki);
        Message m = result.getError(0);
        assertEquals(StatusCodes.MISSING_COLLECTION, m.getCode());
        assertEquals(
                "VC is not found queries/collections/named-vcs/unknown-vc.jsonld",
                m.getMessage());
        assertEquals(0, result.getTotalResults());
    }


    @Test
    public void testDeleteVcNotExist () throws IOException {
        VirtualCorpusCache.delete("unknown-vc-id");
    }


    @Test
    public void testStoreVcInvalidChars () {
        String vcId = "inval!d-vc-id";
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> {
                    VirtualCorpusCache.store(vcId, ki);
                });
        assertEquals("Cannot cache VC due to invalid VC ID", ex.getMessage());
    }

    @Test
    public void testStoreVcInvalidParentPath () {
        String vcId = "..";
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> {
                    VirtualCorpusCache.store(vcId, ki);
                });
        assertEquals("Cannot cache VC due to invalid VC ID", ex.getMessage());
    }
    
    @Test
    public void testStoreVcInvalidNonASCII () {
        String vcId = "aßäüö";
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> {
                    VirtualCorpusCache.store(vcId, ki);
                });
        assertEquals("Cannot cache VC due to invalid VC ID", ex.getMessage());
    }
    
    @Test
    public void testReferVcInvalidChars () throws IOException {
        String file = "/queries/collections/vc-ref/query-with-invalid-vc.jsonld";
        InputStream is = getClass().getResourceAsStream(file);
        String queryRefJson = IOUtils.toString(is, "utf-8");

        Krill krill = new Krill(queryRefJson);
        Result result = krill.apply(ki);
        Message m = result.getError(0);
        assertEquals("Cannot cache VC due to invalid VC ID", m.getMessage());
        assertEquals(104, m.getCode());
    }
}
