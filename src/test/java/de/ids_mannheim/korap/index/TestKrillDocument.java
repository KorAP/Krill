package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;

// This may be better in a model subdirectory

import org.apache.lucene.util.Version;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Bits;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.index.AbstractDocument;

@RunWith(JUnit4.class)
public class TestKrillDocument {

    private class KrillRealDocument extends AbstractDocument {};


    @Test
    public void createDocument () throws IOException {
        KrillRealDocument krd = new KrillRealDocument();
        krd.addDate("pubDate", "2014-10-12");
        assertEquals("2014-10-12", krd.getFieldValue("pubDate"));
        // assertEquals("2014-10-12", krd.getFieldValueAsDate("pubDate").toDisplay());

        krd.addDate("creationDate", "2012-09-05");
        assertEquals("2012-09-05", krd.getFieldValue("creationDate"));
        // assertEquals("2012-09-05", krd.getFieldValueAsDate("creationDate").toDisplay());

        krd.addText("author", "Stephen King");
        assertEquals("Stephen King", krd.getFieldValue("author"));

        krd.addString("pubPlace","Düsseldorf");
        assertEquals("Düsseldorf", krd.getFieldValue("pubPlace"));

        krd.setUID(415);
        assertEquals(415, krd.getUID());

        krd.setUID("561");
        assertEquals(561, krd.getUID());

        try {
            krd.setUID("zzz");
        }
        catch (NumberFormatException e) {};
        assertEquals(561, krd.getUID());

        krd.addText("title", "An Example");
        assertEquals("An Example", krd.getFieldValue("title"));

        krd.addText("subTitle", "An Example");
        assertEquals("An Example", krd.getFieldValue("subTitle"));

        krd.setPrimaryData("We don't need no education");
        assertEquals("We don't need no education", krd.getPrimaryData());
        assertEquals("don't need no education", krd.getPrimaryData(3));
        assertEquals("do", krd.getPrimaryData(3, 5));
        assertEquals(26, krd.getPrimaryDataLength());

        krd.setPrimaryData("abc");
        assertEquals(3, krd.getPrimaryDataLength());
        krd.setPrimaryData("öäüß");
        assertEquals(4, krd.getPrimaryDataLength());

        krd.addString("textSigle", "U-abc-001");
        assertEquals("U-abc-001", krd.getTextSigle());

        krd.addString("docSigle", "U-abc");
        assertEquals("U-abc", krd.getDocSigle());

        krd.addString("corpusSigle", "U");
        assertEquals("U", krd.getCorpusSigle());

        krd.addStored("publisher", "Pope Francis");
        assertEquals("Pope Francis", krd.getFieldValue("publisher"));

        krd.addStored("editor", "Michael Knight");
        assertEquals("Michael Knight", krd.getFieldValue("editor"));

        krd.addString("textType", "shortstory");
        assertEquals("shortstory", krd.getFieldValue("textType"));

        krd.addString("textTypeArt", "Reportage");
        assertEquals("Reportage", krd.getFieldValue("textTypeArt"));

        krd.addString("textTypeRef", "Hm");
        assertEquals("Hm", krd.getFieldValue("textTypeRef"));

        krd.addString("textColumn", "Feuilleton");
        assertEquals("Feuilleton", krd.getFieldValue("textColumn"));

        krd.addString("textDomain", "Comment");
        assertEquals("Comment", krd.getFieldValue("textDomain"));

        krd.addString("availability", "cc");
        assertEquals("cc", krd.getFieldValue("availability"));

        /*
		  krd.setPages("56-78");
		  assertEquals("56-78", krd.getPages());
		*/

        krd.addStored("fileEditionStatement", "no problemo 1");
        assertEquals("no problemo 1", krd.getFieldValue("fileEditionStatement"));

        krd.addStored("biblEditionStatement", "no problemo 2");
        assertEquals("no problemo 2", krd.getFieldValue("biblEditionStatement"));

        krd.addString("language", "de");
        assertEquals("de", krd.getFieldValue("language"));

        krd.addText("corpusTitle", "Mannheimer Morgen");
        assertEquals("Mannheimer Morgen", krd.getFieldValue("corpusTitle"));

        krd.addText("corpusSubTitle", "Zeitung für Mannheim");
        assertEquals("Zeitung für Mannheim", krd.getFieldValue("corpusSubTitle"));

        krd.addText("corpusAuthor", "Peter Gabriel");
        assertEquals("Peter Gabriel", krd.getFieldValue("corpusAuthor"));

        krd.addStored("corpusEditor", "Phil Collins");
        assertEquals("Phil Collins", krd.getFieldValue("corpusEditor"));

        krd.addText("docTitle", "New York Times");
        assertEquals("New York Times", krd.getFieldValue("docTitle"));

        krd.addText("docSubTitle", "Newspaper for New York");
        assertEquals("Newspaper for New York", krd.getFieldValue("docSubTitle"));

        krd.addText("docAuthor", "Dean Baquet");
        assertEquals("Dean Baquet", krd.getFieldValue("docAuthor"));

        krd.addText("docEditor", "Arthur Ochs Sulzberger Jr.");
        assertEquals("Arthur Ochs Sulzberger Jr.", krd.getFieldValue("docEditor"));
    };
};
