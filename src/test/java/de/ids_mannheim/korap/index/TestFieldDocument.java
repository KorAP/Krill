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
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KorapDocument;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanMatchModifyClassQuery;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.analysis.MultiTermTokenStream;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;

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
	fd.addStored("layerInfo", "opennlp/p=pos");
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

	assertEquals(fd.doc.getField("layerInfo").name(), "layerInfo");
	assertEquals(fd.doc.getField("layerInfo").stringValue(), "opennlp/p=pos");

	assertEquals(fd.doc.getField("textClass").name(), "textClass");
	assertEquals(fd.doc.getField("textClass").stringValue(), "music entertainment");
    };

    @Test
    public void indexExample2 () throws Exception {

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
	for (String i : new String[] {"00001",
				      "00002",
				      "00003",
				      "00004",
				      "00005",
				      "00006",
				      "02439"}) {
	    FieldDocument fd = ki.addDocFile(
	        getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	KorapQuery kq = new KorapQuery("tokens");

	KorapSearch ks;
	KorapResult kr;

	// Start creating query
	// within(<s>, {1: {2: [mate/p=ADJA & mate/m=number:sg]}[opennlp/p=NN & tt/p=NN]})

	ks = new KorapSearch(kq.within(
              kq.tag("s"),
              kq._(1,
                kq.seq(
	          kq.seg("mate/p:ADJA")
                ).append(
		  kq.seg("opennlp/p:NN")
	        )
	      )
            ));
	
	ks.setCount(1);
	ks.setCutOff(true);

	ks.context.left.setCharacter(true).setLength(6);
	ks.context.right.setToken(true).setLength(6);

	assertEquals("... okal. [Der Buchstabe A hat in {1:deutschen Texten} eine durchschnittliche Häufigkeit von 6,51 %.] Er ist damit der sechsthäufigste Buchstabe ...", ks.run(ki).getMatch(0).getSnippetBrackets());
    };

    @Test
    public void queryJSONBsp18 () throws Exception {

	// Construct index
	KorapIndex ki = new KorapIndex();

	// Indexing test files
	for (String i : new String[] {"00001",
				      "00002",
				      "00003",
				      "00004",
				      "00005",
				      "00006",
				      "02439"}) {
	    FieldDocument fd = ki.addDocFile(
	        getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	SpanQueryWrapper sqwi = jsonQuery(getClass().getResource("/queries/bsp18.jsonld").getFile());

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

    public static SpanQueryWrapper jsonQuery (String jsonFile) {
	SpanQueryWrapper sqwi;
	
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
