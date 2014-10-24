package de.ids_mannheim.korap.collection;

import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.KorapCollection;

import static de.ids_mannheim.korap.TestSimple.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestKorapCollectionJSONLegacy {

    @Test
    public void metaQuery1 () {
	String metaQuery = getString(getClass().getResource("/queries/metaquery.jsonld").getFile());
	KorapCollection kc = new KorapCollection(metaQuery);

	assertEquals("filter with QueryWrapperFilter(+textClass:wissenschaft)", kc.getFilter(0).toString());
	assertEquals("filter with QueryWrapperFilter(+(+pubPlace:Erfurt +author:Hesse))", kc.getFilter(1).toString());
	assertEquals("extend with QueryWrapperFilter(+(+pubDate:[20110429 TO 20131231] +textClass:freizeit))", kc.getFilter(2).toString());
	assertEquals(3, kc.getCount());
    };


    @Test
    public void metaQuery2 () {
	String metaQuery = getString(getClass().getResource("/queries/metaquery2.jsonld").getFile());
	KorapCollection kc = new KorapCollection(metaQuery);
	assertEquals(1,kc.getCount());
	assertEquals("filter with QueryWrapperFilter(+(+author:Hesse +pubDate:[0 TO 20131205]))",kc.getFilter(0).toString());
    };

    @Test
    public void metaQuery3 () {
	String metaQuery = getString(getClass().getResource("/queries/metaquery4.jsonld").getFile());
	KorapCollection kc = new KorapCollection(metaQuery);
	assertEquals(1,kc.getCount());
	assertEquals("filter with QueryWrapperFilter(+pubDate:[20000101 TO 20131231])",kc.getFilter(0).toString());
    };

    @Test
    public void metaQuery7 () {
	String metaQuery = getString(getClass().getResource("/queries/metaquery7.jsonld").getFile());
	KorapCollection kc = new KorapCollection(metaQuery);
	assertEquals(2,kc.getCount());
	assertEquals("filter with QueryWrapperFilter(+(corpusID:c-1 corpusID:c-2))",kc.getFilter(0).toString());
	assertEquals("filter with QueryWrapperFilter(+(+corpusID:d-1 +corpusID:d-2))",kc.getFilter(1).toString());
    };

    @Test
    public void metaQuery9 () {
	String metaQuery = getString(getClass().getResource("/queries/metaquery9.jsonld").getFile());
	KorapCollection kc = new KorapCollection(metaQuery);
	assertEquals(1,kc.getCount());
	assertEquals("filter with QueryWrapperFilter(+corpusID:WPD)",kc.getFilter(0).toString());
    };
};
