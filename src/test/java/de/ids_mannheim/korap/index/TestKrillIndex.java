package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;

import org.apache.lucene.util.Version;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Bits;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.index.MultiTermTokenStream;

@RunWith(JUnit4.class)
public class TestKrillIndex {

    @Test
    public void indexExample () throws IOException {
        KrillIndex ki = new KrillIndex();

        assertEquals(0, ki.numberOf("base", "documents"));
        assertEquals(0, ki.numberOf("base", "tokens"));
        assertEquals(0, ki.numberOf("base", "sentences"));
        assertEquals(0, ki.numberOf("base", "paragraphs"));

        FieldDocument fd = new FieldDocument();

        fd.addString("name", "Peter");
        fd.addInt("zahl1", 56);
        fd.addInt("zahl2", "58");
        fd.addText("teaser", "Das ist der Name der Rose");
        fd.addTV("base", "ich bau",
                "[(0-3)s:ich|l:ich|p:PPER|-:sentences$<i>2]"
                        + "[(4-7)s:bau|l:bauen|p:VVFIN]");
        ki.addDoc(fd);

        fd = new FieldDocument();

        fd.addString("name", "Hans");
        fd.addInt("zahl1", 14);
        fd.addText("teaser", "Das Sein");

        MultiTermTokenStream mtts = fd.newMultiTermTokenStream();
        mtts.addMultiTermToken("s:wir#0-3", "l:wir", "p:PPER");
        mtts.addMultiTermToken("s:sind#4-8", "l:sein", "p:VVFIN");
        mtts.addMeta("sentences", (int) 5);
        fd.addTV("base", "wir sind", mtts);

        ki.addDoc(fd);

        /* Save documents */
        ki.commit();

        assertEquals(2, ki.numberOf("base", "documents"));
        assertEquals(7, ki.numberOf("base", "sentences"));

        fd = new FieldDocument();

        fd.addString("name", "Frank");
        fd.addInt("zahl1", 59);
        fd.addInt("zahl2", 65);
        fd.addText("teaser", "Noch ein Versuch");
        fd.addTV("base", "ich bau", "[(0-3)s:der|l:der|p:DET|-:sentences$<i>3]"
                + "[(4-8)s:baum|l:baum|p:NN]");
        ki.addDoc(fd);

        /* Save documents */
        ki.commit();

        assertEquals(3, ki.numberOf("base", "documents"));
        assertEquals(10, ki.numberOf("base", "sentences"));

        // KrillQuery kq = new KrillQuery("text");
        // ki.search();
    };


    @Test
    public void indexAlteration () throws IOException {
        KrillIndex ki = new KrillIndex();

        assertEquals(0, ki.numberOf("base", "documents"));

        FieldDocument fd = new FieldDocument();
        fd.addString("name", "Peter");
        ki.addDoc(fd);

        assertEquals(0, ki.numberOf("base", "documents"));

        fd = new FieldDocument();
        fd.addString("name", "Michael");
        ki.addDoc(fd);

        assertEquals(0, ki.numberOf("base", "documents"));

        ki.commit();

        assertEquals(2, ki.numberOf("base", "documents"));

        // hasDeletions, hasPendingMerges
    };
};
