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

    @Ignore
    public void queryJSONBsp1 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp1.json").getFile());

	// ([base=foo]|[base=bar])[base=foobar]
	assertEquals(sqwi.toQuery().toString(), "");
    };

    @Test
    public void queryJSONBsp1b () {

	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp1b.json").getFile());

	// [base=foo]|([base=foo][base=bar]) meta author=Goethe&year=1815
	assertEquals(sqwi.toQuery().toString(), "spanOr([tokens:base:foo, spanNext(tokens:base:foo, tokens:base:bar)])");
    };


    @Test
    public void queryJSONBsp2 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp2.json").getFile());

	// ([base=foo]|[base=bar])[base=foobar]
	assertEquals(sqwi.toQuery().toString(), "spanNext(spanOr([tokens:base:foo, tokens:base:bar]), tokens:base:foobar)");
    };

    @Test
    public void queryJSONBsp3 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp3.json").getFile());

	// shrink({[base=Mann]})
	assertEquals(sqwi.toQuery().toString(), "shrink(0: {0: tokens:base:Mann})");
    };

    @Test
    public void queryJSONBsp4 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp4.json").getFile());

	// shrink({[base=foo]}[orth=bar])
	assertEquals(sqwi.toQuery().toString(), "shrink(0: spanNext({0: tokens:base:foo}, tokens:orth:bar))");
    };

    @Test
    public void queryJSONBsp5 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp5.json").getFile());

	// shrink(1:[base=Der]{1:[base=Mann]}) 
	assertEquals(sqwi.toQuery().toString(), "shrink(1: spanNext(tokens:base:Der, {1: tokens:base:Mann}))");
    };

    @Test
    public void queryJSONBsp6 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp6.json").getFile());

	// [base=katze]
	assertEquals(sqwi.toQuery().toString(), "tokens:base:Katze");
    };

    @Ignore
    public void queryJSONBsp7 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp7.json").getFile());

	// [!base=Katze]
	assertEquals(sqwi.toQuery().toString(), "");
    };

    @Ignore
    public void queryJSONBsp8 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp8.json").getFile());

	// [!base=Katze]
	assertEquals(sqwi.toQuery().toString(), "");
    };

    @Test
    public void queryJSONBsp9 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp9.json").getFile());

	// [base=Katze&orth=Katzen]
	assertEquals(sqwi.toQuery().toString(), "spanNear([tokens:base:Katze, tokens:orth:Katzen], -1, false)");
    };

    @Test
    public void queryJSONBsp10 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp10.json").getFile());

	// [base=Katze][orth=und][orth=Hunde]
	assertEquals(sqwi.toQuery().toString(), "spanNext(spanNext(tokens:base:Katze, tokens:orth:und), tokens:orth:Hunde)");
    };

    @Ignore
    public void queryJSONBsp11 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp11.json").getFile());

	// [!(base=Katze&orth=Katzen)]
	assertEquals(sqwi.toQuery().toString(), "");
    };

    @Test
    public void queryJSONBsp12 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp12.json").getFile());

	// contains(<np>,[base=Mann])
	assertEquals(sqwi.toQuery().toString(), "spanWithin(<tokens:np />, tokens:base:Mann)");
    };

    @Ignore
    public void queryJSONBsp13 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp13.json").getFile());

	// startswith(<np>,[!pos=Det])
	assertEquals(sqwi.toQuery().toString(), "");
    };

    @Test
    public void queryJSONBsp13b () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp13b.json").getFile());

	// startswith(<np>,[!pos=Det])
	assertEquals(sqwi.toQuery().toString(), "spanWithin(<tokens:np />, tokens:pos:Det, 1)");
    };

    @Ignore
    public void queryJSONBsp14 () {
	SpanQueryWrapperInterface sqwi = jsonQuery(getClass().getResource("/queries/bsp14.json").getFile());

	// 'vers{2,3}uch'
	assertEquals(sqwi.toQuery().toString(), "");
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
