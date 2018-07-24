package de.ids_mannheim.korap.collection;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.response.Message;
import de.ids_mannheim.korap.util.StatusCodes;

public class TestVCReference {

    @Test
    public void testUnknownVC () throws IOException {

        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("collection/unknown-vc-ref.jsonld");
        String json = IOUtils.toString(is);

        KrillCollection kc = new KrillCollection(json);
        List<Message> messages = kc.getErrors().getMessages();
        assertEquals(1, messages.size());

        assertEquals(StatusCodes.MISSING_COLLECTION, messages.get(0).getCode());
    }
}
