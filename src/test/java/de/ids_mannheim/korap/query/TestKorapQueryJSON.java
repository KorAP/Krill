import java.util.*;
import java.io.*;

import org.apache.lucene.search.spans.SpanQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;

import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.util.QueryException;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestKorapQueryJSON {

    private String defaultFoundry = "mate/";

    @Test
    public void queryJSONBsp1 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp1.jsonld").getFile());

	// There is a repetition in here
	// ([base=foo]|[base=bar])[base=foobar]
	assertEquals(sqwi.toQuery().toString(),
            "spanOr([tokens:base:foo, spanQuantifier(spanNext(tokens:base:foo, tokens:base:bar)[1:100])])");
	assertTrue(sqwi.isOptional());
    };

    @Test
    public void queryJSONBsp1b () {

	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp1b.jsonld").getFile());

	// [base=foo]|([base=foo][base=bar]) meta author=Goethe&year=1815
	assertEquals(sqwi.toQuery().toString(), "spanOr([tokens:"+defaultFoundry+"l:foo, spanNext(tokens:"+defaultFoundry+"l:foo, tokens:"+defaultFoundry+"l:bar)])");
    };


    @Test
    public void queryJSONBsp2 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp2.jsonld").getFile());

	// ([base=foo]|[base=bar])[base=foobar]
	assertEquals(sqwi.toQuery().toString(), "spanNext(spanOr([tokens:"+defaultFoundry+"l:foo, tokens:"+defaultFoundry+"l:bar]), tokens:"+defaultFoundry+"l:foobar)");
    };

    @Test
    public void queryJSONBsp3 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp3.jsonld").getFile());

	// shrink({[base=Mann]})
	assertEquals(sqwi.toQuery().toString(), "shrink(0: {0: tokens:"+defaultFoundry+"l:Mann})");
    };

    @Test
    public void queryJSONBsp4 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp4.jsonld").getFile());

	// shrink({[base=foo]}[orth=bar])
	assertEquals(sqwi.toQuery().toString(), "shrink(0: spanNext({0: tokens:"+defaultFoundry+"l:foo}, tokens:s:bar))");
    };

    @Test
    public void queryJSONBsp5 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp5.jsonld").getFile());

	// shrink(1:[base=Der]{1:[base=Mann]}) 
	assertEquals(sqwi.toQuery().toString(), "shrink(1: spanNext(tokens:"+defaultFoundry+"l:Der, {1: tokens:"+defaultFoundry+"l:Mann}))");
    };

    @Test
    public void queryJSONBsp6 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp6.jsonld").getFile());

	// [base=katze]
	assertEquals(sqwi.toQuery().toString(), "tokens:"+defaultFoundry+"l:Katze");
    };

    @Test
    public void queryJSONBsp7 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp7.jsonld").getFile());

	// [!base=Katze]
	assertEquals("tokens:mate/l:Katze", sqwi.toQuery().toString());
	assertTrue(sqwi.isNegative());
    };

    @Test
    public void queryJSONBsp9 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp9.jsonld").getFile());

	// [base=Katze&orth=Katzen]
	assertEquals(sqwi.toQuery().toString(), "spanSegment(tokens:"+defaultFoundry+"l:Katze, tokens:s:Katzen)");
    };

    @Test
    public void queryJSONBsp9b () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp9b.jsonld").getFile());

	// [base=Katze&orth=Katzen]
	assertEquals(sqwi.toQuery().toString(), "spanSegment(tokens:mate/m:number:pl, tokens:tt/p:NN)");
    };


    @Test
    public void queryJSONBsp10 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp10.jsonld").getFile());

	// [base=Katze][orth=und][orth=Hunde]
	assertEquals(sqwi.toQuery().toString(), "spanNext(spanNext(tokens:"+defaultFoundry+"l:Katze, tokens:s:und), tokens:s:Hunde)");
    };

    @Test
    public void queryJSONBsp11 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp11.jsonld").getFile());

	// [base!=Katze | orth!=Katzen]
	/*
	  Imagine a([^b]|[^c])d
	  Matches abd and acd
	  Interpretation would be not(spanAnd(...))
	*/
	assertEquals(sqwi.toQuery().toString(), "spanOr([tokens:mate/l:Katze, tokens:s:Katzen])");
	assertTrue(sqwi.isNegative());
    };

    @Test
    public void queryJSONBsp12 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp12.jsonld").getFile());

	// contains(<np>,[base=Mann])
	assertEquals(sqwi.toQuery().toString(), "spanContain(<tokens:np />, tokens:"+defaultFoundry+"l:Mann)");
    };

    @Test
    public void queryJSONBsp13 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp13.jsonld").getFile());

	assertEquals(sqwi.toQuery().toString(), "spanStartsWith(<tokens:np />, tokens:mate/p:Det)");
    };

    @Test
    public void queryJSONBsp13b () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp13b.jsonld").getFile());

	// startswith(<np>,[pos=Det])
	assertEquals(sqwi.toQuery().toString(), "spanStartsWith(<tokens:np />, tokens:mate/p:Det)");
    };

    @Test
    public void queryJSONBsp14 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp14.jsonld").getFile());

	// 'vers{2,3}uch'
	assertEquals(sqwi.toQuery().toString(), "SpanMultiTermQueryWrapper(tokens:/s:vers{2,3}uch/)");
    };

    @Test
    public void queryJSONBsp15 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp15.jsonld").getFile());

	// [orth='vers.*ch']
	assertEquals(sqwi.toQuery().toString(), "SpanMultiTermQueryWrapper(tokens:/s:vers.*ch/)");
    };

    @Test
    public void queryJSONBsp16 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp16.jsonld").getFile());

	// [(base=bar|base=foo)&orth=foobar]
	assertEquals(sqwi.toQuery().toString(), "spanSegment(spanOr([tokens:"+defaultFoundry+"l:bar, tokens:"+defaultFoundry+"l:foo]), tokens:s:foobar)");
    };

    @Test
    public void queryJSONBsp17 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp17.jsonld").getFile());

	// within(<np>,[base=Mann])
	assertEquals(sqwi.toQuery().toString(), "spanContain(<tokens:np />, tokens:"+defaultFoundry+"l:Mann)");
    };

    @Test
    public void queryJSONDemo () throws QueryException {
	SpanQueryWrapperInterface sqwi = new KorapQuery("tokens").fromJSON("{ \"query\" : { \"@type\" : \"korap:token\", \"wrap\" : { \"@type\" : \"korap:term\", \"foundry\" : \"base\", \"layer\" : \"p\", \"key\" : \"foo\", \"match\" : \"match:eq\" }}}");

	assertEquals(sqwi.toQuery().toString(), "tokens:base/p:foo");
    };

    @Test
    public void queryJSONBspClass () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp-class.jsonld").getFile());

	// within(<np>,[base=Mann])
	assertEquals(sqwi.toQuery().toString(), "{0: spanNext(tokens:tt/p:ADJA, tokens:mate/p:NN)}");
    };


    @Test
    public void queryJSONcosmas3 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/cosmas3.json").getFile());

	// "das /+w1:3 Buch"
	assertEquals(sqwi.toQuery().toString(), "spanDistance(tokens:s:das, tokens:s:Buch, [(w[1:3], ordered, notExcluded)])");
    };

    @Test
    public void queryJSONcosmas4 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/cosmas4.json").getFile());

	// "das /+w1:3,s1:1 Buch"
	assertEquals(sqwi.toQuery().toString(), "spanMultipleDistance(tokens:s:das, tokens:s:Buch, [(w[1:3], ordered, notExcluded), (s[1:1], ordered, notExcluded)])");
    };

    @Test
    public void queryJSONcosmas4b () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/cosmas4b.json").getFile());

	// "das /+w1:3,s1 Buch"
	assertEquals(sqwi.toQuery().toString(), "spanMultipleDistance(tokens:s:das, tokens:s:Buch, [(w[1:3], ordered, notExcluded), (s[0:1], ordered, notExcluded)])");
    };

    @Test
    public void queryJSONcosmas10 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/cosmas10.json").getFile());

	// "Institut für $deutsche Sprache"
	assertEquals(sqwi.toQuery().toString(), "spanNext(spanNext(spanNext(tokens:s:Institut, tokens:s:für), tokens:i:deutsche), tokens:s:Sprache)");
    };

    @Test
    public void queryJSONcosmas10b () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/cosmas10b.json").getFile());

	// "Institut $FÜR $deutsche Sprache"
	assertEquals(sqwi.toQuery().toString(), "spanNext(spanNext(spanNext(tokens:s:Institut, tokens:i:für), tokens:i:deutsche), tokens:s:Sprache)");
    };

    @Test
    public void queryJSONcosmas16 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/cosmas16.json").getFile());

	// "$wegen #IN(L) <s>"
	assertEquals(sqwi.toQuery().toString(), "shrink(1: spanStartsWith(<tokens:s />, {1: tokens:i:wegen}))");
    };

    @Test
    public void queryJSONcosmas17 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/cosmas17.json").getFile());

	// "#BED($wegen , +sa)"
	assertEquals(sqwi.toQuery().toString(), "spanStartsWith(<tokens:s />, tokens:i:wegen)");
    };

    @Test
    public void queryJSONcosmas20 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/cosmas20.json").getFile());

	//     "MORPH(V) #IN(R) #ELEM(S)"
	// TODO: Uses defaultfoundry!
	assertEquals(sqwi.toQuery().toString(), "shrink(1: spanEndsWith(<tokens:s />, {1: tokens:mate/p:V}))");
    };


    @Test
    public void queryJSONrepetition () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp-repetition.jsonld").getFile());

	// der[cnx/p=A]{0,2}[tt/p=NN]
	assertEquals(sqwi.toQuery().toString(), "spanNext(spanOr([tokens:s:der, spanNext(tokens:s:der, spanQuantifier(tokens:cnx/p:A[1:2]))]), tokens:tt/p:NN)");
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
