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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KorapDocument;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanMatchModifyClassQuery;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.analysis.MultiTermTokenStream;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;

import de.ids_mannheim.korap.util.QueryException;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.index.Term;

// mvn -Dtest=TestWithinIndex#indexExample1 test

@RunWith(JUnit4.class)
public class TestFieldDocument {

    @Test
    public void indexExample1 () throws IOException {
	FieldDocument fd = new FieldDocument();

	fd.addString("corpusID", "WPD");
	fd.addString("ID", "WPD-AAA-00001");
	fd.addText("textClass", "music entertainment");
	fd.addText("author", "Peter Frankenfeld");
	fd.addInt("pubDate", 20130617);
	fd.addText("title", "Wikipedia");
	fd.addText("subTitle", "Die freie Enzyklopädie");
	fd.addString("pubPlace", "Bochum");
	fd.addInt("lastModified", 20130717);
	fd.addTV("tokens",
		 "abc",
		 "[(0-1)s:a|i:a|_0#0-1|-:t$<i>10]" +
		 "[(1-2)s:b|i:b|_1#1-2]" +
		 "[(2-3)s:c|i:c|_2#2-3]");

	assertEquals(fd.doc.getField("title").name(), "title");
	assertEquals(fd.doc.getField("title").stringValue(), "Wikipedia");

	assertEquals(fd.doc.getField("corpusID").name(), "corpusID");
	assertEquals(fd.doc.getField("corpusID").stringValue(), "WPD");

	assertEquals(fd.doc.getField("ID").name(), "ID");
	assertEquals(fd.doc.getField("ID").stringValue(), "WPD-AAA-00001");

	assertEquals(fd.doc.getField("subTitle").name(), "subTitle");
	assertEquals(fd.doc.getField("subTitle").stringValue(), "Die freie Enzyklopädie");

	assertEquals(fd.doc.getField("pubPlace").name(), "pubPlace");
	assertEquals(fd.doc.getField("pubPlace").stringValue(), "Bochum");

	assertEquals(fd.doc.getField("lastModified").name(), "lastModified");
	assertEquals(fd.doc.getField("lastModified").stringValue(), "20130717");

	assertEquals(fd.doc.getField("tokens").name(), "tokens");
	assertEquals(fd.doc.getField("tokens").stringValue(), "abc");

	assertEquals(fd.doc.getField("author").name(), "author");
	assertEquals(fd.doc.getField("author").stringValue(), "Peter Frankenfeld");

	assertEquals(fd.doc.getField("textClass").name(), "textClass");
	assertEquals(fd.doc.getField("textClass").stringValue(), "music entertainment");
    };

    @Test
    public void indexExample2 () throws IOException {

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
"  ]," +
"  \"corpusID\"  : \"WPD\"," +
"  \"ID\"        : \"WPD-AAA-00001\"," +
"  \"textClass\" : \"music entertainment\"," +
"  \"author\"    : \"Peter Frankenfeld\"," +
"  \"pubDate\"   : 20130617," +
"  \"title\"     : \"Wikipedia\"," +
"  \"subTitle\"  : \"Die freie Enzyklopädie\"," +
"  \"pubPlace\"  : \"Bochum\"" +
"}");

	KorapIndex ki = new KorapIndex();
	FieldDocument fd = ki.addDoc(json);

	ki.commit();

	assertEquals(fd.getPrimaryData(),"abc");
	assertEquals(fd.getCorpusID(),"WPD");
	assertEquals(fd.getID(),"WPD-AAA-00001");
	assertEquals(fd.getTextClass(),"music entertainment");
	assertEquals(fd.getAuthor(),"Peter Frankenfeld");
	assertEquals(fd.getTitle(),"Wikipedia");
	assertEquals(fd.getSubTitle(),"Die freie Enzyklopädie");
	assertEquals(fd.getPubPlace(),"Bochum");
	assertEquals(fd.getPubDate().toDisplay(),"2013-06-17");

	KorapQuery kq = new KorapQuery("tokens");
	KorapResult kr = ki.search((SpanQuery) kq.seq(kq._(3, kq.seg("s:b"))).toQuery());

	KorapMatch km = kr.getMatch(0);

	assertEquals(km.getPrimaryData(),"abc");
	assertEquals(km.getCorpusID(),"WPD");
	assertEquals(km.getDocID(),"WPD-AAA-00001");
	assertEquals(km.getTextClass(),"music entertainment");
	assertEquals(km.getAuthor(),"Peter Frankenfeld");
	assertEquals(km.getTitle(),"Wikipedia");
	assertEquals(km.getSubTitle(),"Die freie Enzyklopädie");
	assertEquals(km.getPubPlace(),"Bochum");
	assertEquals(km.getPubDate().toDisplay(),"2013-06-17");

	assertEquals(km.getSnippetBrackets(),"a[{3:b}]c");
	//	System.err.println(kr.toJSON());
    };

    @Test
    public void indexExample3 () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();

	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    FieldDocument fd = ki.addDocFile(
	        getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	// Start creating query
	KorapQuery kq = new KorapQuery("tokens");
	
	// within(<xip/const:NPA>, {1: {2: [cnx/p=A & mate/m=number:sg]}[opennlp/p=NN & tt/p=NN]})
	SpanQuery query =
	    kq.within(
              kq.tag("xip/c:NPA"),
              kq._(1,
                kq.seq(
	          kq._(2, kq.seg("cnx/p:A").with("mate/m:number:sg"))
                ).append(
  		  kq.seg("opennlp/p:NN").with("tt/p:NN")
	        )
	      )
            ).toQuery();


	KorapResult kr;

	/*
	kr = ki.search(query, 0, (short) 60, true, (short) 6, true, (short) 6);
	System.err.println(kr.toJSON());
	*/


	kr = ki.search(query, 0, (short) 1, true, (short) 2, false, (short) 5);
	assertEquals("... Buchstabe des [{1:{2:lateinischen} Alphabets}] und  ...", kr.match(0).getSnippetBrackets());

	
	kr = ki.search(query, 0, (short) 50, true, (short) 2, false, (short) 5);

//	System.err.println(kr.toJSON());
//	System.out.println(query.toString());
//	System.out.println(kr.match(37));	
	
	assertEquals(38, kr.totalResults());
	assertEquals(50, kr.itemsPerPage());
	assertEquals("... Buchstabe des [{1:{2:lateinischen} Alphabets}] und  ...", kr.match(0).getSnippetBrackets());
	assertEquals("... Texten eine [{1:{2:durchschnittliche} Häufigkeit}] von  ...", kr.match(1).getSnippetBrackets());
	assertEquals("... damit der [{1:{2:sechsthäufigste} Buchstabe}] in d ...", kr.match(2).getSnippetBrackets());
	assertEquals("... A der [{1:{2:einzige} Buchstabe}] im D ...", kr.match(3).getSnippetBrackets());
	assertEquals("... für den [offenen vorderen {1:{2:ungerundeten} Vokal}] a: A ...", kr.match(4).getSnippetBrackets());

	query = kq.seg("tt/l:Norwegen").toQuery();
	kr = ki.search(query, 0, (short) 5, true, (short) 2, false, (short) 5);

	assertEquals(3, kr.totalResults());
	assertEquals("... Lofoten in [Norwegen], unt ...", kr.match(0).getSnippetBrackets());
	assertEquals("WPD_AAA.00002", kr.match(0).getDocID());
	assertEquals("... es in [Norwegen] noch ...", kr.match(1).getSnippetBrackets());
	assertEquals("WPD_AAA.00002", kr.match(1).getDocID());
	assertEquals("... Orte in [Norwegen]: Å i ...", kr.match(2).getSnippetBrackets());
	assertEquals("WPD_AAA.00005", kr.match(2).getDocID());

	/*
	System.err.println(ki.getMatchInfo(kr.match(2).getID(), "tokens", "xip", "l", true, false).getSnippetHTML());
	*/

	query = kq.seg("tt/l:Vokal").without("mate/m:number:sg").toQuery();
	kr = ki.search(query, 0, (short) 5, true, (short) 2, false, (short) 5);
	assertEquals(1, kr.totalResults());
	assertEquals("... reich an [Vokalen] war, ...", kr.match(0).getSnippetBrackets());

	assertNotNull(kr.toJSON());


	/*
	System.err.println(ki.getMatchInfo(
	    "match-WPD!WPD_AAA.00004-p200-206",
	    "tokens",
	    "xip",
	    "c",
	    true,
	    false,
	    true
        ).toJSON());
*/
	//	ki.getMatch();
    };

    @Test
    public void queryJSONBsp18 () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();

	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    FieldDocument fd = ki.addDocFile(
	        getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp18.jsonld").getFile());

	KorapResult kr = ki.search(sqwi.toQuery(), 0, (short) 5, true, (short) 2, false, (short) 5);

	// Bug:
	// System.err.println(kr.toJSON());
    };

    public static String getString (String path) {
	StringBuilder contentBuilder = new StringBuilder();
	try {
	    BufferedReader in = new BufferedReader(new FileReader(path));
	    String str;
	    while ((str = in.readLine()) != null) {
		contentBuilder.append(str);
	    };
	    in.close();
	} catch (IOException e) {
	    fail(e.getMessage());
	}
	return contentBuilder.toString();
    };

    public static SpanQueryWrapperInterface jsonQuery (String jsonFile) {
	SpanQueryWrapperInterface sqwi;
	
	try {
	    String json = getString(jsonFile);
	    sqwi = new KorapQuery("tokens").fromJSON(json);
	}
	catch (QueryException e) {
	    fail(e.getMessage());
	    sqwi = new KorapQuery("tokens").seg("???");
	};
	return sqwi;
    };
};
