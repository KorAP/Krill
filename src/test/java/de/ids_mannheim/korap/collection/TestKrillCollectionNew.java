package de.ids_mannheim.korap.collection;

import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.collection.CollectionBuilderNew;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class TestKrillCollectionNew {

    @Test
    public void builderTerm () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("author:tree",
                     kc.term("author", "tree").toString());
    };

    @Test
    public void builderRegex () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("QueryWrapperFilter(author:/tre*?/)",
                     kc.re("author", "tre*?").toString());
    };

    @Test
    public void builderDateYear () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("pubDate:[20050000 TO 20059999]",
                     kc.date("pubDate", "2005").toString());
    };

    @Test
    public void builderDateMonth () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("pubDate:[20051000 TO 20051099]",
                     kc.date("pubDate", "2005-10").toString());
    };

    @Test
    public void builderDateDay () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("pubDate:20051011",
                     kc.date("pubDate", "2005-10-11").toString());
    };

    @Test
    public void builderDateBorders () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        // CollectionBuilderNew.CollectionBuilderInterface kbi = ;
        assertNull(kc.date("pubDate", ""));

        assertEquals("pubDate:20051580",
                     kc.date("pubDate", "2005-15-80").toString());

        assertNull(kc.date("pubDate", "2005-15-8"));
        assertNull(kc.date("pubDate", "2005-5-18"));
        assertNull(kc.date("pubDate", "200-05-18"));
    };

    @Test
    public void builderSince () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("pubDate:[20050000 TO 99999999]",
                     kc.since("pubDate", "2005").toString());

        assertEquals("pubDate:[20051000 TO 99999999]",
                     kc.since("pubDate", "2005-10").toString());

        assertEquals("pubDate:[20051012 TO 99999999]",
                     kc.since("pubDate", "2005-10-12").toString());
    };


    @Test
    public void builderTill () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("pubDate:[0 TO 20059999]",
                     kc.till("pubDate", "2005").toString());

        assertEquals("pubDate:[0 TO 20051299]",
                     kc.till("pubDate", "2005-12").toString());

        assertEquals("pubDate:[0 TO 20051204]",
                     kc.till("pubDate", "2005-12-04").toString());
    };


    @Test
    public void builderAndSimple () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("author:tree", kc.andGroup(kc.term("author", "tree")).toString());
    };

    @Test
    public void builderOrSimple () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("author:tree", kc.orGroup(kc.term("author", "tree")).toString());
    };

    @Test
    public void builderAndCombined () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("BooleanFilter(+author:tree +title:name)",
                     kc.andGroup(kc.term("author", "tree"))
                     .with(kc.term("title", "name")).toString());
    };

    @Test
    public void builderAndNestedSimple () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("BooleanFilter(+author:tree +title:name)",
                     kc.andGroup(kc.andGroup(kc.term("author", "tree")).with(kc.term("title", "name"))).toString());
    };


    @Test
    public void builderOrCombined () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("BooleanFilter(author:tree title:name)",
                     kc.orGroup(kc.term("author", "tree"))
                     .with(kc.term("title", "name")).toString());
    };

    @Test
    public void builderOrNestedSimple () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("BooleanFilter(author:tree title:name)",
                     kc.orGroup(kc.orGroup(kc.term("author", "tree"))
                                .with(kc.term("title", "name"))).toString()
                     );
    };

    @Test
    public void builderGroups () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        String g = kc.orGroup(
              kc.orGroup(kc.term("author", "tree1")).with(kc.term("title", "name1"))
        ).with(
              kc.andGroup(kc.term("author", "tree2")).with(kc.term("title", "name2"))
               ).toString();
        assertEquals("BooleanFilter(BooleanFilter(author:tree1 title:name1) BooleanFilter(+author:tree2 +title:name2))", g);
    };

    @Test
    public void builderNegationRoot () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        CollectionBuilderNew.CollectionBuilderInterface kbi = kc.orGroup(kc.term("author", "tree1")).with(kc.term("title", "name1"));
        assertEquals(
                     "BooleanFilter(author:tree1 title:name1)",
                     kbi.toString());
        assertFalse(kbi.isNegative());

        kbi = kc.andGroup(
              kc.orGroup(kc.term("author", "tree1")).with(kc.term("title", "name1"))
              ).not();
        assertEquals("BooleanFilter(author:tree1 title:name1)", kbi.toString());
        assertTrue(kbi.isNegative());
    };


    @Test
    public void builderNegation () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        CollectionBuilderNew.CollectionBuilderInterface kbi =
            kc.term("author", "tree").not();
        assertEquals("author:tree", kbi.toString());
        assertTrue(kbi.isNegative());

        kbi = kc.andGroup(kc.term("author", "tree").not());
        assertEquals("author:tree", kbi.toString());
        assertTrue(kbi.isNegative());

        kbi = kc.orGroup(kc.term("author", "tree").not());
        assertEquals("author:tree", kbi.toString());
        assertTrue(kbi.isNegative());
    };


};
