package de.ids_mannheim.korap.collection;

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

import de.ids_mannheim.korap.collection.CollectionBuilder;

@RunWith(JUnit4.class)
public class TestCollectionBuilder {

    @Test
    public void filterExample () throws IOException {
        CollectionBuilder kf = new CollectionBuilder();

        assertEquals("+textClass:tree", kf.and("textClass", "tree").toString());
        assertEquals("+textClass:tree +textClass:sport",
                kf.and("textClass", "tree").and("textClass", "sport")
                        .toString());
        assertEquals(
                "+textClass:tree +textClass:sport textClass:news",
                kf.and("textClass", "tree").and("textClass", "sport")
                        .or("textClass", "news").toString());
        assertEquals("+textClass:tree +textClass:sport +textClass:news", kf
                .and("textClass", "tree", "sport", "news").toString());

        assertEquals("corpusID:c-1 corpusID:c-2 corpusID:c-3",
                kf.or("corpusID", "c-1", "c-2", "c-3").toString());
    };


    @Test
    public void rangeExample () throws IOException {
        CollectionBuilder kf = new CollectionBuilder();
        assertEquals("+pubDate:[20030604 TO 20030899]",
                kf.between("2003-06-04", "2003-08-99").toString());
        assertEquals("+pubDate:[0 TO 20030604]", kf.till("2003-06-04")
                .toString());
        assertEquals("+pubDate:[20030604 TO 99999999]", kf.since("2003-06-04")
                .toString());
        assertEquals("+pubDate:20030604", kf.date("2003-06-04").toString());
    };


    @Test
    public void rangeLimited () throws IOException {
        CollectionBuilder kf = new CollectionBuilder();
        assertEquals("+pubDate:[20050000 TO 20099999]",
                kf.between("2005", "2009").toString());
        assertEquals("+pubDate:[20051000 TO 20090899]",
                kf.between("200510", "200908").toString());
        assertEquals("+pubDate:[20051000 TO 20090899]",
                kf.between("2005-10", "2009-08").toString());
        assertEquals("+pubDate:[20051006 TO 20090803]",
                kf.between("2005-1006", "2009-0803").toString());
        assertEquals("+pubDate:[20051006 TO 20090803]",
                kf.between("2005-10-06", "2009-08-03").toString());

        assertEquals("+pubDate:[0 TO 20059999]", kf.till("2005").toString());
        assertEquals("+pubDate:[0 TO 20051099]", kf.till("200510").toString());
        assertEquals("+pubDate:[0 TO 20051099]", kf.till("2005-10").toString());
        assertEquals("+pubDate:[0 TO 20051006]", kf.till("2005-1006")
                .toString());
        assertEquals("+pubDate:[0 TO 20051006]", kf.till("2005-10-06")
                .toString());

        assertEquals("+pubDate:[20050000 TO 99999999]", kf.since("2005")
                .toString());
        assertEquals("+pubDate:[20051000 TO 99999999]", kf.since("200510")
                .toString());
        assertEquals("+pubDate:[20051000 TO 99999999]", kf.since("2005-10")
                .toString());
        assertEquals("+pubDate:[20051006 TO 99999999]", kf.since("2005-1006")
                .toString());
        assertEquals("+pubDate:[20051006 TO 99999999]", kf.since("2005-10-06")
                .toString());

        assertEquals("+pubDate:[20050000 TO 20059999]", kf.date("2005")
                .toString());
        assertEquals("+pubDate:[20051000 TO 20051099]", kf.date("200510")
                .toString());
        assertEquals("+pubDate:[20051000 TO 20051099]", kf.date("2005-10")
                .toString());
        assertEquals("+pubDate:20051006", kf.date("2005-1006").toString());
        assertEquals("+pubDate:20051006", kf.date("2005-10-06").toString());
    };


    @Test
    public void rangeFailure () throws IOException {
        CollectionBuilder kf = new CollectionBuilder();
        assertEquals("", kf.between("aaaa-bb-cc", "aaaabbcc").toString());
        assertEquals("", kf.till("aaaa-bb-cc").toString());
        assertEquals("", kf.since("aaaa-bb-cc").toString());
        assertEquals("", kf.date("aaaa-bb-cc").toString());
    };


    // TODO: More extensive testing!
};
