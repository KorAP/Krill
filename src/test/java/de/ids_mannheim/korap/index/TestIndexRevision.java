package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.Ignore;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillMeta;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.StatusCodes;

import java.nio.file.Paths;
import org.apache.lucene.store.MMapDirectory;

public class TestIndexRevision {

    @Test
    public void testIndexRevisionAdd () throws IOException {
        KrillIndex ki = new KrillIndex();

        assertEquals(ki.getFingerprint(),"null");
        
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        String x1 = ki.getFingerprint();
        assertEquals(x1,"ibtSULzKIMrfGAtES3GXRA==");

        ki.addDoc(getClass().getResourceAsStream("/wiki/00002.json.gz"), true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/00003.json.gz"), true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/00004.json.gz"), true);
        ki.commit();

        String x2 = ki.getFingerprint();
        assertEquals(x2,"0UIQZpZVfiGDD2leAq6YQA==");

        ki.addDoc(getClass().getResourceAsStream("/wiki/00006.json.gz"), true);
        ki.commit();

        String x3 = ki.getFingerprint();
        assertEquals(x3,"fS3GqnKynhPQ5wFyC9/XWw==");


        // Check if the same changes will have the same effect
        KrillIndex ki2 = new KrillIndex();

        assertEquals(ki2.getFingerprint(),"null");
        
        ki2.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki2.commit();

        assertEquals(ki2.getFingerprint(), x1);

        ki2.addDoc(getClass().getResourceAsStream("/wiki/00002.json.gz"), true);
        ki2.addDoc(getClass().getResourceAsStream("/wiki/00003.json.gz"), true);
        ki2.addDoc(getClass().getResourceAsStream("/wiki/00004.json.gz"), true);
        ki2.commit();

        assertEquals(ki2.getFingerprint(), x2);

        ki2.addDoc(getClass().getResourceAsStream("/wiki/00006.json.gz"), true);
        ki2.commit();

        assertEquals(ki2.getFingerprint(), x3);
    };

    @Test
    public void testIndexRevisionDel () throws IOException {
        KrillIndex ki = new KrillIndex();

        assertEquals(ki.getFingerprint(),"null");
        
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        String x1 = ki.getFingerprint();
        assertEquals(x1,"ibtSULzKIMrfGAtES3GXRA==");

        assertTrue(ki.delDocs("title", "A"));
        ki.commit();

        String x2 = ki.getFingerprint();
        assertNotEquals(x1, x2);
    };

    @Test
    public void testIndexRevisionTempFile () throws IOException {

        Path tmpdir = Files.createTempDirectory("wiki");
        KrillIndex ki = new KrillIndex(new MMapDirectory(tmpdir));

        assertEquals("null", ki.getFingerprint());
        
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        ki.addDoc(getClass().getResourceAsStream("/wiki/00002.json.gz"), true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/00003.json.gz"), true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/00004.json.gz"), true);
        ki.commit();

        ki.addDoc(getClass().getResourceAsStream("/wiki/00006.json.gz"), true);
        ki.commit();

        assertTrue(ki.delDocs("title", "A"));
        ki.commit();

        assertEquals(false, ki.isReaderOpen());

        String fingerp = "aoD2zQvZKa8oQPjFJlji1g==";
        assertEquals(fingerp, ki.getFingerprint());

        assertEquals(true, ki.isReaderOpen());
        assertEquals(4, ki.numberOf("base", "documents"));

        assertEquals(fingerp, ki.getFingerprint());

        ki.close();

        // Reload index
        ki = new KrillIndex(new MMapDirectory(tmpdir));

        assertEquals(fingerp, ki.getFingerprint());

        ki.close();
    };
    
    @Ignore
    public void testIndexRevisionSample () throws IOException {
        KrillIndex ki = new KrillIndex(new MMapDirectory(
                Paths.get(getClass().getResource("/sample-index").getFile())));

        assertEquals(ki.getFingerprint(),"Wes8Bd4h1OypPqbWF5njeQ==");
    };
};
