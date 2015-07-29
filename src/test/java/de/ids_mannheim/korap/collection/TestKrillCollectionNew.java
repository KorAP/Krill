package de.ids_mannheim.korap.collection;

import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.collection.CollectionBuilder;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class TestKrillCollectionNew {

    @Test
    public void builderTerm () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("author:tree",
                     kc.term("author", "tree").toString());
    };

    @Test
    public void builderRegex () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("QueryWrapperFilter(author:/tre*?/)",
                     kc.re("author", "tre*?").toString());
    };

    @Test
    public void builderDateYear () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("pubDate:[20050000 TO 20059999]",
                     kc.date("pubDate", "2005").toString());
    };

    @Test
    public void builderDateMonth () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("pubDate:[20051000 TO 20051099]",
                     kc.date("pubDate", "2005-10").toString());
    };

    @Test
    public void builderDateDay () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("pubDate:[20051011 TO 20051011]",
                     kc.date("pubDate", "2005-10-11").toString());
    };

    @Test
    public void builderDateBorders () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        // CollectionBuilderNew.CollectionBuilderInterface kbi = ;
        assertNull(kc.date("pubDate", ""));

        assertEquals("pubDate:[20051580 TO 20051580]",
                     kc.date("pubDate", "2005-15-80").toString());

        assertNull(kc.date("pubDate", "2005-15-8"));
        assertNull(kc.date("pubDate", "2005-5-18"));
        assertNull(kc.date("pubDate", "200-05-18"));
    };

    @Test
    public void builderSince () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("pubDate:[20050000 TO 99999999]",
                     kc.since("pubDate", "2005").toString());

        assertEquals("pubDate:[20051000 TO 99999999]",
                     kc.since("pubDate", "2005-10").toString());

        assertEquals("pubDate:[20051012 TO 99999999]",
                     kc.since("pubDate", "2005-10-12").toString());
    };


    @Test
    public void builderTill () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("pubDate:[0 TO 20059999]",
                     kc.till("pubDate", "2005").toString());

        assertEquals("pubDate:[0 TO 20051299]",
                     kc.till("pubDate", "2005-12").toString());

        assertEquals("pubDate:[0 TO 20051204]",
                     kc.till("pubDate", "2005-12-04").toString());
    };


    @Test
    public void builderAndSimple () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("author:tree", kc.andGroup().with(kc.term("author", "tree")).toString());
    };

    @Test
    public void builderOrSimple () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("author:tree", kc.orGroup().with(kc.term("author", "tree")).toString());
    };

    @Test
    public void builderAndCombined () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("AndGroup(author:tree title:name)",
                     kc.andGroup().with(kc.term("author", "tree"))
                     .with(kc.term("title", "name")).toString());
    };

    @Test
    public void builderAndNestedSimple () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("AndGroup(author:tree title:name)",
                     kc.andGroup().with(kc.andGroup().with(kc.term("author", "tree")).with(kc.term("title", "name"))).toString());
    };


    @Test
    public void builderOrCombined () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("OrGroup(author:tree title:name)",
                     kc.orGroup().with(kc.term("author", "tree"))
                     .with(kc.term("title", "name")).toString());
    };

    @Test
    public void builderOrNestedSimple () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("OrGroup(author:tree title:name)",
                     kc.orGroup().with(kc.orGroup().with(kc.term("author", "tree"))
                                .with(kc.term("title", "name"))).toString()
                     );
    };

    @Test
    public void builderGroups () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        String g = kc.orGroup().with(
                                     kc.orGroup().with(kc.term("author", "tree1")).with(kc.term("title", "name1"))
        ).with(
               kc.andGroup().with(kc.term("author", "tree2")).with(kc.term("title", "name2"))
               ).toString();
        assertEquals("OrGroup(OrGroup(author:tree1 title:name1) AndGroup(author:tree2 title:name2))", g);
    };

    @Test
    public void builderNegationRoot () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        CollectionBuilder.CollectionBuilderInterface kbi = kc.orGroup().with(kc.term("author", "tree1")).with(kc.term("title", "name1"));
        assertEquals(
                     "OrGroup(author:tree1 title:name1)",
                     kbi.toString());
        assertFalse(kbi.isNegative());

        kbi = kc.andGroup().with(
                                 kc.orGroup().with(kc.term("author", "tree1")).with(kc.term("title", "name1"))
              ).not();
        assertEquals("OrGroup(author:tree1 title:name1)", kbi.toString());
        assertTrue(kbi.isNegative());
    };


    @Test
    public void builderNegation () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        CollectionBuilder.CollectionBuilderInterface kbi =
            kc.term("author", "tree").not();
        assertEquals("author:tree", kbi.toString());
        assertTrue(kbi.isNegative());

        kbi = kc.andGroup().with(kc.term("author", "tree").not());
        assertEquals("author:tree", kbi.toString());
        assertTrue(kbi.isNegative());

        kbi = kc.orGroup().with(kc.term("author", "tree").not());
        assertEquals("author:tree", kbi.toString());
        assertTrue(kbi.isNegative());
    };


};
