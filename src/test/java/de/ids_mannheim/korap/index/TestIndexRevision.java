package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.store.MMapDirectory;
import org.junit.Ignore;
import org.junit.Test;

import de.ids_mannheim.korap.KrillIndex;

public class TestIndexRevision {

    @Test
    public void testIndexRevisionAdd () throws IOException {
        KrillIndex ki = new KrillIndex();

        assertEquals("null", ki.getFingerprint());
        
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        String x1 = ki.getFingerprint();
        assertEquals("ibtSULzKIMrfGAtES3GXRA==",x1);

        ki.addDoc(getClass().getResourceAsStream("/wiki/00002.json.gz"), true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/00003.json.gz"), true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/00004.json.gz"), true);
        ki.commit();

        String x2 = ki.getFingerprint();
        assertEquals("0UIQZpZVfiGDD2leAq6YQA==",x2);

        ki.addDoc(getClass().getResourceAsStream("/wiki/00006.json.gz"), true);
        ki.commit();

        String x3 = ki.getFingerprint();
        assertEquals("fS3GqnKynhPQ5wFyC9_XWw==",x3);


        // Check if the same changes will have the same effect
        KrillIndex ki2 = new KrillIndex();

        assertEquals("null",ki2.getFingerprint());
        
        ki2.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki2.commit();

        assertEquals(x1, ki2.getFingerprint());

        ki2.addDoc(getClass().getResourceAsStream("/wiki/00002.json.gz"), true);
        ki2.addDoc(getClass().getResourceAsStream("/wiki/00003.json.gz"), true);
        ki2.addDoc(getClass().getResourceAsStream("/wiki/00004.json.gz"), true);
        ki2.commit();

        assertEquals(x2, ki2.getFingerprint());

        ki2.addDoc(getClass().getResourceAsStream("/wiki/00006.json.gz"), true);
        ki2.commit();

        assertEquals(x3, ki2.getFingerprint());
    };

    @Test
    public void testIndexRevisionDel () throws IOException {
        KrillIndex ki = new KrillIndex();

        assertEquals("null", ki.getFingerprint());
        
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        String x1 = ki.getFingerprint();
        assertEquals("ibtSULzKIMrfGAtES3GXRA==",x1);

        assertTrue(ki.delDocs("title", "A"));
        ki.commit();

        String x2 = ki.getFingerprint();
        assertNotEquals(x1, x2);
    };

    @Test
    public void testIndexRevisionTempFile () throws IOException {

        Path tmpdir = Files.createTempDirectory("wiki");
        KrillIndex ki = new KrillIndex(tmpdir);

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
    
    public void testIndexRevisionLeafTempFile () throws IOException {

        String x1, x2, x3;
        
        Path tmpdir = Files.createTempDirectory("wikileaf");
        KrillIndex ki = new KrillIndex(new MMapDirectory(tmpdir));

        assertEquals("null", ki.getFingerprint());

        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        List<LeafReaderContext> contexts = ki.reader().leaves();
        assertEquals(1, contexts.size());
        x1 = contexts.get(0).reader().getCombinedCoreAndDeletesKey().toString();
        assertEquals("_0(5.0.0):c1", x1);
        
        ki.addDoc(getClass().getResourceAsStream("/wiki/00002.json.gz"), true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/00003.json.gz"), true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/00004.json.gz"), true);
        ki.commit();

        ki.addDoc(getClass().getResourceAsStream("/wiki/00006.json.gz"), true);
        ki.commit();

        contexts = ki.reader().leaves();
        assertEquals(3, contexts.size());
        x1 = contexts.get(0).reader().getCombinedCoreAndDeletesKey().toString();
        assertEquals("_0(5.0.0):c1", x1);
        x2 = contexts.get(1).reader().getCombinedCoreAndDeletesKey().toString();
        assertEquals("_1(5.0.0):c3", x2);
        x3 = contexts.get(2).reader().getCombinedCoreAndDeletesKey().toString();
        assertEquals("_2(5.0.0):c1", x3);
        
        assertTrue(ki.delDocs("title", "A (Band)"));
        ki.commit();

        assertEquals(false, ki.isReaderOpen());

        contexts = ki.reader().leaves();
        assertEquals(3, contexts.size());
        x1 = contexts.get(0).reader().getCombinedCoreAndDeletesKey().toString();
        assertEquals("_0(5.0.0):c1", x1);
        x2 = contexts.get(1).reader().getCombinedCoreAndDeletesKey().toString();
        assertEquals("_1(5.0.0):c3/1:delGen=1", x2);
        x3 = contexts.get(2).reader().getCombinedCoreAndDeletesKey().toString();
        assertEquals("_2(5.0.0):c1", x3);
        
        String fingerp = "241/XHj/9ZxeO5Lm3zZ+iw==";
        assertEquals(fingerp, ki.getFingerprint());

        ki.close();

        // Reload index
        ki = new KrillIndex(new MMapDirectory(tmpdir));

        assertEquals(fingerp, ki.getFingerprint());

        contexts = ki.reader().leaves();
        assertEquals(3, contexts.size());
        x1 = contexts.get(0).reader().getCombinedCoreAndDeletesKey().toString();
        assertEquals("_0(5.0.0):c1", x1);
        x2 = contexts.get(1).reader().getCombinedCoreAndDeletesKey().toString();
        assertEquals("_1(5.0.0):c3/1:delGen=1", x2);
        x3 = contexts.get(2).reader().getCombinedCoreAndDeletesKey().toString();
        assertEquals("_2(5.0.0):c1", x3);
        
        ki.close();
    };


    @Ignore
    public void testIndexRevisionSample () throws IOException {
        KrillIndex ki = new KrillIndex(
                Paths.get(getClass().getResource("/sample-index").getFile()));

        assertEquals("Wes8Bd4h1OypPqbWF5njeQ==",ki.getFingerprint());
    };
};
