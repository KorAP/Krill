import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.KorapCollection;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestKorapCollectionJSON {

    @Test
    public void metaQuery1 () {
	String metaQuery = getString(getClass().getResource("/queries/metaquery.json").getFile());
	KorapCollection kc = new KorapCollection(metaQuery);

	assertEquals("filter with QueryWrapperFilter(+textClass:wissenschaft)", kc.getFilter(0).toString());
	assertEquals("filter with QueryWrapperFilter(+pubPlace:Erfurt +author:Hesse)", kc.getFilter(1).toString());
	assertEquals("extend with QueryWrapperFilter(+pubDate:[20110429 TO 20131231] +textClass:freizeit)", kc.getFilter(2).toString());
	assertEquals(3, kc.getCount());
    };


    @Test
    public void metaQuery2 () {
	String metaQuery = getString(getClass().getResource("/queries/metaquery2.json").getFile());
	KorapCollection kc = new KorapCollection(metaQuery);
	assertEquals(1,kc.getCount());
	assertEquals("filter with QueryWrapperFilter(+author:Hesse +pubDate:[0 TO 20131205])",kc.getFilter(0).toString());
    };

    @Test
    public void metaQuery3 () {
	String metaQuery = getString(getClass().getResource("/queries/metaquery4.json").getFile());
	KorapCollection kc = new KorapCollection(metaQuery);
	assertEquals(1,kc.getCount());
	assertEquals("filter with QueryWrapperFilter(+pubDate:[20000101 TO 20131231])",kc.getFilter(0).toString());
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

};
