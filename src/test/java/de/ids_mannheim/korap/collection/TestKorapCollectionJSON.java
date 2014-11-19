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
public class TestKorapCollectionJSON {

    final String path = "/queries/collections/";

    @Test
    public void collection1 () {
	String metaQuery = _getJSONString("collection_1.jsonld");
	KorapCollection kc = new KorapCollection(metaQuery);
	assertEquals(kc.toString(), "filter with QueryWrapperFilter(+pubDate:20000101); ");
    };

    @Test
    public void collection2 () {
	String metaQuery = _getJSONString("collection_2.jsonld");
	KorapCollection kc = new KorapCollection(metaQuery);
	assertEquals(kc.toString(), "filter with QueryWrapperFilter(+(+pubDate:[19900000 TO 99999999] +pubDate:[0 TO 20061099])); ");
    };

    @Test
    public void collection3 () {
	String metaQuery = _getJSONString("collection_3.jsonld");
	KorapCollection kc = new KorapCollection(metaQuery);
	assertEquals(kc.toString(), "");
    };

    @Test
    public void collection5 () {
	String metaQuery = _getJSONString("collection_5.jsonld");
	KorapCollection kc = new KorapCollection(metaQuery);
	assertEquals(kc.toString(), "filter with QueryWrapperFilter(+(pubDate:[19900000 TO 99999999] title:Mannheim)); ");
    };

    @Test
    public void nocollectiontypegiven () {
	String metaQuery = _getJSONString("multiterm_rewrite_collection.jsonld");
	KorapCollection kc = new KorapCollection(metaQuery);
	assertEquals(701, kc.getError(0).getCode());
    };


    @Ignore
    public void noCollection () {
	String metaQuery = _getJSONString("no_collection.jsonld");
	// TODO!!!
	// Use KorapSearch and test
    };


    private String _getJSONString (String file) {
	return getString(getClass().getResource(path + file).getFile());
    };
};
