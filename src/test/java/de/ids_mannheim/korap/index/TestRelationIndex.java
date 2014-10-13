package de.ids_mannheim.korap.index;

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
            "[(0-1)s:c|_1#0-1|>:xip/syntax-dep_rel$<i>7<s>1]" +
            "[(1-2)s:e|_2#1-2|<:xip/syntax-dep_rel$<i>10<s>1|>:xip/syntax-dep_rel$<i>5<s>1]" +             
            "[(2-3)s:c|_3#2-3]" +
            "[(3-4)s:c|s:b|_4#3-4|<:xip/syntax-dep_rel$<i>10<s>1]" + 
            "[(4-5)s:e|s:d|_5#4-5|<:xip/syntax-dep_rel$<i>2<s>1]" +
            "[(5-6)s:c|_6#5-6]" +
            "[(6-7)s:d|_7#6-7|<:xip/syntax-dep_rel$<i>1<s>1]" +
            "[(7-8)s:e|_8#7-8]" + 
            "[(8-9)s:e|s:b|_9#8-9]" + 
            "[(9-10)s:d|_10#9-10|>:xip/syntax-dep_rel$<i>2<s>2|>:xip/syntax-dep_rel$<i>1<s>1]");
        return fd;
    }
	
	/** Test token-token relation
	 * */
	@Test
	public void testCase1() throws IOException {
		ki.addDoc(createFieldDoc0());
		ki.commit();
		
		SpanRelationQuery sq = new SpanRelationQuery(
				new SpanTermQuery(new Term("base",">:xip/syntax-dep_rel")),true);
		ki.search(sq,(short) 10);
	}
}
