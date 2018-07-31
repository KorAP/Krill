package de.ids_mannheim.korap.collection;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.store.MMapDirectory;
import org.junit.Test;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;

public class TestWithExternalIndex {
    private KrillIndex getSampleIndex () throws IOException {
        return new KrillIndex(new MMapDirectory(
                Paths.get(getClass().getResource("/sample-index").getFile())));
    }

    private KrillIndex index;

    public TestWithExternalIndex () throws IOException {
        index = getSampleIndex();
    }

    @Test
    public void testIndexTextSigleNe () throws IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("queries/collections/query-textSigle-ne.jsonld");
        String json = IOUtils.toString(is);

        Krill k = new Krill(json).apply(index);
        long totalResults = k.getTotalResults();
//        System.out.println(k.toJsonString());
        assertTrue(totalResults > 0);
    }
}
