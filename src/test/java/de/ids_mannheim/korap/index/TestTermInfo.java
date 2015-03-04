package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.index.TermInfo;
import java.nio.ByteBuffer;



@RunWith(JUnit4.class)
public class TestTermInfo {
    @Test
    public void termExample1 () throws IOException {

        byte[] b = new byte[16];
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putInt(20); // startOffset
        bb.putInt(25); // endOffset
        bb.putInt(7);  // endPos
        bb.put((byte) 4);

        TermInfo term = new TermInfo("<>:mate/p:NN", 4, bb).analyze();
        assertEquals("type", term.getType(), "span");
        assertEquals("value", term.getValue(), "NN");
        assertEquals("foundry", term.getFoundry(), "mate");
        assertEquals("layer", term.getLayer(), "p");
        assertEquals("startPos", term.getStartPos(), 4);
        assertEquals("endPos", term.getEndPos(), 6);
        assertEquals("startChar", term.getStartChar(), 20);
        assertEquals("endChar", term.getEndChar(), 25);
        assertEquals("depth", term.getDepth(), (byte) 4);

        bb.clear();
        term = new TermInfo("mate/p:NN", 9, bb).analyze();
        assertEquals("type", term.getType(), "term");
        assertEquals("value", term.getValue(), "NN");
        assertEquals("foundry", term.getFoundry(), "mate");
        assertEquals("layer", term.getLayer(), "p");
        assertEquals("startPos", term.getStartPos(), 9);
        assertEquals("endPos", term.getEndPos(), 9);
        assertEquals("startChar", term.getStartChar(), -1);
        assertEquals("endChar", term.getEndChar(), -1);
        assertEquals("depth", term.getDepth(), 0);

        bb.clear();
        bb.putInt(17).put((byte) 2);
        term = new TermInfo(">:xip/p:ADJ", 11, bb).analyze();
        assertEquals("type", term.getType(), "relSrc");
        assertEquals("value", term.getValue(), "ADJ");
        assertEquals("foundry", term.getFoundry(), "xip");
        assertEquals("layer", term.getLayer(), "p");
        assertEquals("startPos", term.getStartPos(), 11);
        assertEquals("endPos", term.getEndPos(), 16);
        assertEquals("startChar", term.getStartChar(), -1);
        assertEquals("endChar", term.getEndChar(), -1);
        assertEquals("depth", term.getDepth(), 0);

        bb.clear();
        bb.putInt(24);
        term = new TermInfo("<:xip/m:number:pl", 20, bb).analyze();
        assertEquals("type", term.getType(), "relTarget");
        assertEquals("value", term.getValue(), "number:pl");
        assertEquals("foundry", term.getFoundry(), "xip");
        assertEquals("layer", term.getLayer(), "m");
        assertEquals("startPos", term.getStartPos(), 20);
        assertEquals("endPos", term.getEndPos(), 23);
        assertEquals("startChar", term.getStartChar(), -1);
        assertEquals("endChar", term.getEndChar(), -1);
        assertEquals("depth", term.getDepth(), 0);

        bb.clear();
        bb.putInt(240).putInt(400);
        term = new TermInfo("_30", 30, bb).analyze();
        assertEquals("type", term.getType(), "pos");
        assertEquals("value", term.getValue(), "30");
        assertNull("foundry", term.getFoundry());
        assertNull("layer", term.getLayer());
        assertEquals("startPos", term.getStartPos(), 30);
        assertEquals("endPos", term.getEndPos(), 30);
        assertEquals("startChar", term.getStartChar(), 240);
        assertEquals("endChar", term.getEndChar(), 400);
        assertEquals("depth", term.getDepth(), 0);

        bb.clear();
        bb.putInt(20); // startOffset
        bb.putInt(25); // endOffset
        bb.putInt(24); // endPos
        term = new TermInfo("<>:s", 20, bb).analyze();
        assertEquals("type", term.getType(), "span");
        assertNull("value", term.getValue());
        assertEquals("foundry", term.getFoundry(), "base");
        assertEquals("layer", term.getLayer(), "s");
        assertEquals("startPos", term.getStartPos(), 20);
        assertEquals("endPos", term.getEndPos(), 23);
        assertEquals("startChar", term.getStartChar(), 20);
        assertEquals("endChar", term.getEndChar(), 25);
        assertEquals("depth", term.getDepth(), 0);

        bb.clear();
        bb.putInt(20); // startOffset
        bb.putInt(25); // endOffset
        bb.putInt(24); // endPos
        term = new TermInfo("<>:tag/x", 20, bb).analyze();
        assertEquals("type", term.getType(), "span");
        assertNull("value", term.getValue());
        assertEquals("foundry", term.getFoundry(), "tag");
        assertEquals("layer", term.getLayer(), "x");
        assertEquals("startPos", term.getStartPos(), 20);
        assertEquals("endPos", term.getEndPos(), 23);
        assertEquals("startChar", term.getStartChar(), 20);
        assertEquals("endChar", term.getEndChar(), 25);
        assertEquals("depth", term.getDepth(), 0);
    };
};