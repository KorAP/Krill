package de.ids_mannheim.korap.highlight;

import java.util.*;
import java.io.IOException;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.index.FieldDocument;

import de.ids_mannheim.korap.util.QueryException;

import static de.ids_mannheim.korap.TestSimple.*;
//import static de.ids_mannheim.korap.Test.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestTemporaryQueryLimitations {

    @Test
    public void classRefCheckNotSupported () throws IOException, QueryException  {

	// Construct index
	KorapIndex ki = new KorapIndex();
	String json = new String(
"{" +
"  \"fields\" : [" +
"    { "+
"      \"primaryData\" : \"abc\"" +
"    }," +
"    {" +
"      \"name\" : \"tokens\"," +
"      \"data\" : [" +
"         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"]," +
"         [ \"s:b\", \"i:b\", \"_1#1-2\" ]," +
"         [ \"s:c\", \"i:c\", \"_2#2-3\" ]" +
"      ]" +
"    }" +
"  ]" +
"}");

	FieldDocument fd = ki.addDoc(json);
	ki.commit();

	json = getString(getClass().getResource("/queries/bugs/cosmas_classrefcheck.jsonld").getFile());
	
	KorapSearch ks = new KorapSearch(json);
	KorapResult kr = ks.run(ki);
	assertEquals(kr.getQuery(),"shrink(130: {131: spanContain({129: <tokens:s />}, {130: tokens:s:wegen})})");
	assertEquals(kr.totalResults(),0);
	assertEquals(kr.getStartIndex(),0);
	assertEquals(
	    kr.getWarning(),
	    "classRefCheck is not yet supported - results may not be correct; " +
	    "This is a warning coming from the serialization");

    };
};
