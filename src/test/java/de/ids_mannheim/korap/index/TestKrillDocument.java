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
        assertEquals("20141012", krd.getPubDate().toString());
        assertEquals("2014-10-12", krd.getPubDateString());

        krd.addDate("creationDate", "2012-09-05");
        assertEquals("20120905", krd.getCreationDate().toString());
        assertEquals("2012-09-05", krd.getCreationDateString());

        krd.addText("author", "Stephen King");
        assertEquals("Stephen King", krd.getAuthor());

        krd.addString("pubPlace","Düsseldorf");
        assertEquals("Düsseldorf", krd.getPubPlace());

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
        assertEquals("An Example", krd.getTitle());

        krd.addText("subTitle", "An Example");
        assertEquals("An Example", krd.getSubTitle());

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
        assertEquals("Pope Francis", krd.getPublisher());

        krd.addStored("editor", "Michael Knight");
        assertEquals("Michael Knight", krd.getEditor());

        krd.addString("textType", "shortstory");
        assertEquals("shortstory", krd.getTextType());

        krd.addString("textTypeArt", "Reportage");
        assertEquals("Reportage", krd.getTextTypeArt());

        krd.addString("textTypeRef", "Hm");
        assertEquals("Hm", krd.getTextTypeRef());

        krd.addString("textColumn", "Feuilleton");
        assertEquals("Feuilleton", krd.getTextColumn());

        krd.addString("textDomain", "Comment");
        assertEquals("Comment", krd.getTextDomain());

        krd.addString("availability", "cc");
        assertEquals("cc", krd.getAvailability());

        /*
		  krd.setPages("56-78");
		  assertEquals("56-78", krd.getPages());
		*/

        krd.addStored("fileEditionStatement", "no problemo 1");
        assertEquals("no problemo 1", krd.getFileEditionStatement());

        krd.addStored("biblEditionStatement", "no problemo 2");
        assertEquals("no problemo 2", krd.getBiblEditionStatement());

        krd.addString("language", "de");
        assertEquals("de", krd.getLanguage());

        krd.addText("corpusTitle", "Mannheimer Morgen");
        assertEquals("Mannheimer Morgen", krd.getCorpusTitle());

        krd.addText("corpusSubTitle", "Zeitung für Mannheim");
        assertEquals("Zeitung für Mannheim", krd.getCorpusSubTitle());

        krd.addText("corpusAuthor", "Peter Gabriel");
        assertEquals("Peter Gabriel", krd.getCorpusAuthor());

        krd.addStored("corpusEditor", "Phil Collins");
        assertEquals("Phil Collins", krd.getCorpusEditor());

        krd.addText("docTitle", "New York Times");
        assertEquals("New York Times", krd.getDocTitle());

        krd.addText("docSubTitle", "Newspaper for New York");
        assertEquals("Newspaper for New York", krd.getDocSubTitle());

        krd.addText("docAuthor", "Dean Baquet");
        assertEquals("Dean Baquet", krd.getDocAuthor());

        krd.addText("docEditor", "Arthur Ochs Sulzberger Jr.");
        assertEquals("Arthur Ochs Sulzberger Jr.", krd.getDocEditor());
    };
};
