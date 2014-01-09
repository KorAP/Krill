import java.util.*;
import java.io.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapResult;

import de.ids_mannheim.korap.index.FieldDocument;

@RunWith(JUnit4.class)
public class TestMatchIdentifier {

    @Test
    public void indexExample1 () throws IOException {
	KorapIndex ki = new KorapIndex();

	// abcabcabac
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "abcabcabac",
		 "[(0-1)s:a|i:a|_0#0-1|-:t$<i>10]" +
		 "[(1-2)s:b|i:b|_1#1-2]" +
		 "[(2-3)s:c|i:c|_2#2-3]" +
		 "[(3-4)s:a|i:a|_3#3-4]" +
		 "[(4-5)s:b|i:b|_4#4-5]" +
		 "[(5-6)s:c|i:c|_5#5-6]" +
		 "[(6-7)s:a|i:a|_6#6-7]" +
		 "[(7-8)s:b|i:b|_7#7-8]" +
		 "[(8-9)s:a|i:a|_8#8-9]" +
		 "[(9-10)s:c|i:c|_9#9-10]");
	ki.addDoc(fd);

	ki.commit();

	KorapQuery kq = new KorapQuery("base");
	KorapSearch ks = new KorapSearch(kq._(2,kq.seq(kq.seg("s:b")).append(kq._(kq.seg("s:a")))));
	KorapResult kr = ki.search(ks);

	assertEquals("totalResults", 1, kr.totalResults());
	assertEquals("StartPos (0)", 7, kr.match(0).startPos);
	assertEquals("EndPos (0)", 9, kr.match(0).endPos);

	assertEquals("SnippetBrackets (0)", "... bcabca[{2:b{a}}]c", kr.match(0).snippetBrackets());

	assertEquals("ID (0)", "match-0p7-9(0)8-8(2)7-8c7-9(0)8-9(2)7-9", kr.match(0).getID());
    };


    @Test
    public void indexExample2 () throws IOException {
	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	        getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();
	// System.err.println(ki.getMatch("test").toJSON());
    };

};
