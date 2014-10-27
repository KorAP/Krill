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
	System.err.println(kc.toString());
    };

    private String _getJSONString (String file) {
	return getString(getClass().getResource(path + file).getFile());
    };
};
