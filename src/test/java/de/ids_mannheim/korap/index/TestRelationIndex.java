package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.SpanRelationQuery;

    /*

within(x,y)

SpanRelationQuery->
rel("SUBJ", query1, query2)

1. return all words that are subjects of (that are linked by the “SUBJ” relation to) the string “beginnt”
xip/syntax-dep_rel:beginnt >[func=”SUBJ”] xip/syntax-dep_rel:.*
-> rel("SUBJ", highlight(query1), new TermQuery("s:beginnt"))


SUBJ ist modelliert mit offset für den gesamten Bereich

https://de.wikipedia.org/wiki/Dependenzgrammatik

im regiert Wasser
dass die Kinder im Wasser gespielt haben
3. im#16-18$
3. >:COORD#16-25$3,4
4. Wasser#19-25$
4. <:COORD#16-25$3,4

# okay: return all main verbs that have no “SUBJ” relation specified


# Not okay: 5. return all verbs with (at least?) 3 outgoing relations [think of ditransitives such as give]

xip/morph_pos:VERB & xip/token:.* & xip/token:.* & xip/token:.* & xip/token:.* & #1 _=_#2 & #2 >[func=$x] #3 & #2 >[func=$x]#4  &  #2 >[func=$x] #5

# Okay: return all verbs that have singular SUBJects and dative OBJects

xip/morph_pos:VERB & mpt/morph_msd:Sg & mpt/morph_msd:Dat & #1 >[func=”SUBJ”] #2 & #1 >[func=”OBJ”] #3

-> [p:VVFIN](>SUBJ[nr:sg] & >OBJ[c:dat])


     */

public class TestRelationIndex {
	private KorapIndex ki;
	private KorapResult kr;

	public TestRelationIndex() throws IOException {
		ki = new KorapIndex();
	}
	
	private FieldDocument createFieldDoc0(){
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base",
            "text",             
            "[(0-1)s:c|_1#0-1|>:xip/syntax-dep_rel$<i>6<s>1]" +
            "[(1-2)s:e|_2#1-2|<:xip/syntax-dep_rel$<i>9<s>1|>:xip/syntax-dep_rel$<i>4<s>1]" +             
            "[(2-3)s:c|_3#2-3]" +
            "[(3-4)s:c|s:b|_4#3-4|<:xip/syntax-dep_rel$<i>9<s>1]" + 
            "[(4-5)s:e|s:d|_5#4-5|<:xip/syntax-dep_rel$<i>1<s>1]" +
            "[(5-6)s:c|_6#5-6]" +
            "[(6-7)s:d|_7#6-7|<:xip/syntax-dep_rel$<i>1<s>1]" +
            "[(7-8)s:e|_8#7-8]" + 
            "[(8-9)s:e|s:b|_9#8-9]" + 
            "[(9-10)s:d|_10#9-10|>:xip/syntax-dep_rel$<i>1<s>2|>:xip/syntax-dep_rel$<i>3<s>1]");
        return fd;
    }
	
	private FieldDocument createFieldDoc1(){
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base",
            "text",             
            "[(0-1)s:c|_1#0-1|>:xip/syntax-dep_rel$<i>3<i>6<i>9<s>2|>:xip/syntax-dep_rel$<i>6<i>9<s>1]" +
            "[(1-2)s:e|_2#1-2]" +             
            "[(2-3)s:c|_3#2-3]" +
            "[(3-4)s:c|s:b|_4#3-4]" + 
            "[(4-5)s:e|s:d|_5#4-5]" +
            "[(5-6)s:c|_6#5-6]" +
            "[(6-7)s:d|_7#6-7|<:xip/syntax-dep_rel$<i>9<b>0<i>0<s>1|>:xip/syntax-dep_rel$<i>9<b>0<i>9<s>3|" +
            	"<:xip/syntax-dep_rel$<i>9<i>1<i>3<s>2]" +
            "[(7-8)s:e|_8#7-8]" + 
            "[(8-9)s:e|s:b|_9#8-9]" + 
            "[(9-10)s:d|_10#9-10|<:xip/syntax-dep_rel$<i>6<i>9<s>2]");
        return fd;
    }
	
	
	@Test
	public void testCase1() throws IOException {
		ki.addDoc(createFieldDoc0());
		ki.addDoc(createFieldDoc1());
		ki.commit();
		
		SpanRelationQuery sq = new SpanRelationQuery(
				new SpanTermQuery(new Term("base",">:xip/syntax-dep_rel")),true);
		kr = ki.search(sq,(short) 10);
		
		assertEquals(7, kr.getTotalResults());
		// token to token
		assertEquals(0,kr.getMatch(0).getLocalDocID());
		assertEquals(0,kr.getMatch(0).getStartPos());
		assertEquals(1,kr.getMatch(0).getEndPos());
		assertEquals(1,kr.getMatch(1).getStartPos());
		assertEquals(2,kr.getMatch(1).getEndPos());
		assertEquals(9,kr.getMatch(2).getStartPos());
		assertEquals(10,kr.getMatch(2).getEndPos());
		assertEquals(9,kr.getMatch(3).getStartPos());
		assertEquals(10,kr.getMatch(3).getEndPos());
		
		// token to span
		assertEquals(1,kr.getMatch(4).getLocalDocID());
		assertEquals(0,kr.getMatch(4).getStartPos());
		assertEquals(1,kr.getMatch(4).getEndPos());
		assertEquals(0,kr.getMatch(5).getStartPos());
		assertEquals(3,kr.getMatch(5).getEndPos());
		
		// span to span
		assertEquals(6,kr.getMatch(6).getStartPos());
		assertEquals(9,kr.getMatch(6).getEndPos());
		
		// check target
		
	}
	
	@Test
	public void testCase2() throws IOException {
		ki.addDoc(createFieldDoc0());
		ki.addDoc(createFieldDoc1());
		ki.commit();
		
		SpanRelationQuery sq = new SpanRelationQuery(
				new SpanTermQuery(new Term("base","<:xip/syntax-dep_rel")),true);
		kr = ki.search(sq,(short) 10);
		
		assertEquals(7, kr.getTotalResults());
		// token to token
		assertEquals(0,kr.getMatch(0).getLocalDocID());
		assertEquals(1,kr.getMatch(0).getStartPos());
		assertEquals(2,kr.getMatch(0).getEndPos());
		assertEquals(3,kr.getMatch(1).getStartPos());
		assertEquals(4,kr.getMatch(1).getEndPos());
		assertEquals(4,kr.getMatch(2).getStartPos());
		assertEquals(5,kr.getMatch(2).getEndPos());
		assertEquals(6,kr.getMatch(3).getStartPos());
		assertEquals(7,kr.getMatch(3).getEndPos());
		
		assertEquals(1,kr.getMatch(4).getLocalDocID());
		// span to token
		assertEquals(6,kr.getMatch(4).getStartPos());
		assertEquals(9,kr.getMatch(4).getEndPos());
		assertEquals(6,kr.getMatch(5).getStartPos());
		assertEquals(9,kr.getMatch(5).getEndPos());
		// span to span
		assertEquals(9,kr.getMatch(6).getStartPos());
		assertEquals(10,kr.getMatch(6).getEndPos());
	}
}
