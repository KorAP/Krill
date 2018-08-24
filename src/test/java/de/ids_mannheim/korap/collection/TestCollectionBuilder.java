package de.ids_mannheim.korap.collection;

import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.collection.CollectionBuilder;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// TODO: More extensive testing!


@RunWith(JUnit4.class)
public class TestCollectionBuilder {

    @Test
    public void builderTerm () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("author:tree", kc.term("author", "tree").toString());
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
        // CollectionBuilderNew.Interface kbi = ;
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
    public void builderReference () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("referTo(ndiewald/myCorpus)",
                kc.referTo("ndiewald/myCorpus").toString());
    };


    @Test
    public void builderReferenceNested () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();

        // Doesn't work (as intended), because the filtering
        // phase won't work
        assertEquals(
            "",
            kc.orGroup().with(
                kc.referTo("example")
                ).with(
                    kc.term("opennlp","check")
                    ).toString()
            );
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
        assertEquals("author:tree",
                kc.andGroup().with(kc.term("author", "tree")).toString());
    };


    @Test
    public void builderOrSimple () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("author:tree",
                kc.orGroup().with(kc.term("author", "tree")).toString());
    };


    @Test
    public void builderAndCombined () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("AndGroup(author:tree title:name)",
                kc.andGroup().with(kc.term("author", "tree"))
                        .with(kc.term("title", "name")).toString());
    };

    @Test
    public void builderAndCombinedNeg () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("AndGroup(author:tree -title:name)",
                kc.andGroup().with(kc.term("author", "tree"))
					 .with(kc.term("title", "name").not()).toString());
    };
	

    @Test
    public void builderAndNestedSimple () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        assertEquals("AndGroup(author:tree title:name)",
                kc.andGroup().with(kc.andGroup().with(kc.term("author", "tree"))
                        .with(kc.term("title", "name"))).toString());
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
                        .with(kc.term("title", "name"))).toString());
    };


    @Test
    public void builderGroups () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        String g = kc.orGroup()
                .with(kc.orGroup().with(kc.term("author", "tree1"))
                        .with(kc.term("title", "name1")))
                .with(kc.andGroup().with(kc.term("author", "tree2"))
                        .with(kc.term("title", "name2")))
                .toString();
        assertEquals(
                "OrGroup(OrGroup(author:tree1 title:name1) AndGroup(author:tree2 title:name2))",
                g);
    };


    @Test
    public void builderNegationRoot () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        CollectionBuilder.Interface kbi = kc.orGroup()
                .with(kc.term("author", "tree1"))
                .with(kc.term("title", "name1"));
        assertEquals("OrGroup(author:tree1 title:name1)", kbi.toString());
        assertFalse(kbi.isNegative());

        kbi = kc.andGroup().with(kc.orGroup().with(kc.term("author", "tree1"))
                .with(kc.term("title", "name1"))).not();
        assertEquals("OrGroup(author:tree1 title:name1)", kbi.toString());
        assertTrue(kbi.isNegative());
    };


    @Test
    public void builderNegation () throws IOException {
        CollectionBuilder kc = new CollectionBuilder();
        CollectionBuilder.Interface kbi = kc.term("author", "tree").not();
        assertEquals("author:tree", kbi.toString());
        assertTrue(kbi.isNegative());

        kbi = kc.andGroup().with(kc.term("author", "tree").not());
        kbi.not();
        assertEquals("author:tree", kbi.toString());
        assertTrue(kbi.isNegative());

        kbi = kc.orGroup().with(kc.term("author", "tree").not());
        kbi.not();
        assertEquals("author:tree", kbi.toString());
        assertTrue(kbi.isNegative());
    };


    // The legacy tests were adopted from the legacy collection builder and reformuated

    @Test
    public void LegacyFilterExample () throws IOException {
        CollectionBuilder kf = new CollectionBuilder();

        /*
        assertEquals("+textClass:tree", kf.and("textClass", "tree").toString());
        */
        assertEquals("textClass:tree",
                kf.andGroup().with(kf.term("textClass", "tree")).toString());

        /*
        assertEquals("+textClass:tree +textClass:sport",
                kf.and("textClass", "tree").and("textClass", "sport")
                        .toString());
        */
        assertEquals("AndGroup(textClass:tree textClass:sport)",
                kf.andGroup().with(kf.term("textClass", "tree"))
                        .with(kf.term("textClass", "sport")).toString());

        /*
        assertEquals(
                "+textClass:tree +textClass:sport textClass:news",
                kf.and("textClass", "tree").and("textClass", "sport")
                        .or("textClass", "news").toString());
        */
        assertEquals(
                "OrGroup(AndGroup(textClass:tree textClass:sport) textClass:news)",
                kf.orGroup()
                        .with(kf.andGroup().with("textClass", "tree")
                                .with("textClass", "sport"))
                        .with("textClass", "news").toString());

        /*
        assertEquals("+textClass:tree +textClass:sport +textClass:news", kf
                .and("textClass", "tree", "sport", "news").toString());
        */
        assertEquals("AndGroup(textClass:tree textClass:sport textClass:news)",
                kf.andGroup().with("textClass", "tree")
                        .with("textClass", "sport").with("textClass", "news")
                        .toString());

        /*
        assertEquals("corpusID:c-1 corpusID:c-2 corpusID:c-3",
                kf.or("corpusID", "c-1", "c-2", "c-3").toString());
        */
        assertEquals("OrGroup(corpusID:c-1 corpusID:c-2 corpusID:c-3)",
                kf.orGroup().with("corpusID", "c-1").with("corpusID", "c-2")
                        .with("corpusID", "c-3").toString());

    };


    @Test
    public void LegacyRangeExample () throws IOException {
        CollectionBuilder kf = new CollectionBuilder();
        /*
        assertEquals("+pubDate:[20030604 TO 20030899]",
                kf.between("2003-06-04", "2003-08-99").toString());
        */
        // This will be optimized and probably crash
        assertEquals(
                "AndGroup(pubDate:[20030604 TO 99999999] pubDate:[0 TO 20030899])",
                kf.andGroup().with(kf.since("pubDate", "2003-06-04"))
                        .with(kf.till("pubDate", "2003-08-99")).toString());

        /*
        assertEquals("+pubDate:[0 TO 20030604]", kf.till("2003-06-04")
                .toString());
        */
        assertEquals("pubDate:[0 TO 20030604]",
                kf.till("pubDate", "2003-06-04").toString());


        /*
        assertEquals("+pubDate:[20030604 TO 99999999]", kf.since("2003-06-04")
                .toString());
        */
        assertEquals("pubDate:[20030604 TO 99999999]",
                kf.since("pubDate", "2003-06-04").toString());

        /*
        assertEquals("+pubDate:20030604", kf.date("2003-06-04").toString());
        */
        assertEquals("pubDate:[20030604 TO 20030604]",
                kf.date("pubDate", "2003-06-04").toString());
    };


    @Test
    public void LegacyRangeLimited () throws IOException {
        CollectionBuilder kf = new CollectionBuilder();
        /*
        assertEquals("+pubDate:[20050000 TO 20099999]",
                kf.between("2005", "2009").toString());
        */
        assertEquals(
                "AndGroup(pubDate:[20050000 TO 99999999] pubDate:[0 TO 20099999])",
                kf.between("pubDate", "2005", "2009").toString());

        /*
        assertEquals("+pubDate:[20051000 TO 20090899]",
                kf.between("200510", "200908").toString());
        */
        assertEquals(
                "AndGroup(pubDate:[20051000 TO 99999999] pubDate:[0 TO 20090899])",
                kf.between("pubDate", "200510", "200908").toString());

        /*
        assertEquals("+pubDate:[20051000 TO 20090899]",
                kf.between("2005-10", "2009-08").toString());
        */
        assertEquals(
                "AndGroup(pubDate:[20051000 TO 99999999] pubDate:[0 TO 20090899])",
                kf.between("pubDate", "2005-10", "2009-08").toString());


        /*
        assertEquals("+pubDate:[20051006 TO 20090803]",
                kf.between("2005-1006", "2009-0803").toString());
         */
        assertEquals(
                "AndGroup(pubDate:[20051006 TO 99999999] pubDate:[0 TO 20090803])",
                kf.between("pubDate", "2005-1006", "2009-0803").toString());

        /*
        assertEquals("+pubDate:[20051006 TO 20090803]",
                kf.between("2005-10-06", "2009-08-03").toString());
        */
        assertEquals(
                "AndGroup(pubDate:[20051006 TO 99999999] pubDate:[0 TO 20090803])",
                kf.between("pubDate", "2005-10-06", "2009-08-03").toString());

        /*
        assertEquals("+pubDate:[0 TO 20059999]", kf.till("2005").toString());
        */
        assertEquals("pubDate:[0 TO 20059999]",
                kf.till("pubDate", "2005").toString());

        /*
        assertEquals("+pubDate:[0 TO 20051099]", kf.till("200510").toString());
        */
        assertEquals("pubDate:[0 TO 20051099]",
                kf.till("pubDate", "200510").toString());

        /*
        assertEquals("+pubDate:[0 TO 20051099]", kf.till("200510").toString());
        */
        assertEquals("pubDate:[0 TO 20051099]",
                kf.till("pubDate", "200510").toString());

        /*
        assertEquals("+pubDate:[0 TO 20051099]", kf.till("2005-10").toString());
        */
        assertEquals("pubDate:[0 TO 20051099]",
                kf.till("pubDate", "2005-10").toString());

        /*
        assertEquals("+pubDate:[0 TO 20051006]", kf.till("2005-1006")
                .toString());
         */
        assertEquals("pubDate:[0 TO 20051006]",
                kf.till("pubDate", "2005-1006").toString());

        /*
        assertEquals("+pubDate:[0 TO 20051006]", kf.till("2005-10-06")
                .toString());
        */
        assertEquals("pubDate:[0 TO 20051006]",
                kf.till("pubDate", "2005-10-06").toString());

        /*
        assertEquals("+pubDate:[20050000 TO 99999999]", kf.since("2005")
                .toString());
        */
        assertEquals("pubDate:[20050000 TO 99999999]",
                kf.since("pubDate", "2005").toString());

        /*
        assertEquals("+pubDate:[20051000 TO 99999999]", kf.since("200510")
                .toString());
        */
        assertEquals("pubDate:[20051000 TO 99999999]",
                kf.since("pubDate", "200510").toString());


        /*
        assertEquals("+pubDate:[20051000 TO 99999999]", kf.since("2005-10")
                .toString());
        */
        assertEquals("pubDate:[20051000 TO 99999999]",
                kf.since("pubDate", "2005-10").toString());

        /*
        assertEquals("+pubDate:[20051006 TO 99999999]", kf.since("2005-1006")
                .toString());
        */
        assertEquals("pubDate:[20051006 TO 99999999]",
                kf.since("pubDate", "2005-1006").toString());

        /*
        assertEquals("+pubDate:[20051006 TO 99999999]", kf.since("2005-10-06")
                .toString());
        */
        assertEquals("pubDate:[20051006 TO 99999999]",
                kf.since("pubDate", "2005-10-06").toString());

        /*
        assertEquals("+pubDate:[20050000 TO 20059999]", kf.date("2005")
                .toString());
        */
        assertEquals("pubDate:[20050000 TO 20059999]",
                kf.date("pubDate", "2005").toString());


        /*
        assertEquals("+pubDate:[20051000 TO 20051099]", kf.date("200510")
                .toString());
        */
        assertEquals("pubDate:[20051000 TO 20051099]",
                kf.date("pubDate", "200510").toString());

        /*
        assertEquals("+pubDate:[20051000 TO 20051099]", kf.date("2005-10")
                .toString());
        */
        assertEquals("pubDate:[20051000 TO 20051099]",
                kf.date("pubDate", "2005-10").toString());

        /*
        assertEquals("+pubDate:20051006", kf.date("2005-1006").toString());
        */
        assertEquals("pubDate:[20051006 TO 20051006]",
                kf.date("pubDate", "2005-1006").toString());

        /*
        assertEquals("+pubDate:20051006", kf.date("2005-10-06").toString());
        */
        assertEquals("pubDate:[20051006 TO 20051006]",
                kf.date("pubDate", "2005-10-06").toString());
    };


    @Test
    public void LegacyRangeFailure () throws IOException {
        CollectionBuilder kf = new CollectionBuilder();
        /*
        assertEquals("", kf.between("aaaa-bb-cc", "aaaabbcc").toString());
        assertEquals("", kf.till("aaaa-bb-cc").toString());
        assertEquals("", kf.since("aaaa-bb-cc").toString());
        assertEquals("", kf.date("aaaa-bb-cc").toString());
         */
        assertNull(kf.between("pubDate", "aaaa-bb-cc", "aaaabbcc"));
        assertNull(kf.till("pubDate", "aaaa-bb-cc"));
        assertNull(kf.since("pubDate", "aaaa-bb-cc"));
        assertNull(kf.date("pubDate", "aaaa-bb-cc"));
    };
};
