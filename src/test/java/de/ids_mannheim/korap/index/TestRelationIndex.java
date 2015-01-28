package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.SpanAttributeQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanRelationQuery;
import de.ids_mannheim.korap.query.SpanRelationPartQuery;
import de.ids_mannheim.korap.query.SpanSegmentQuery;
import de.ids_mannheim.korap.query.SpanTermWithIdQuery;
import de.ids_mannheim.korap.query.SpanWithAttributeQuery;

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
            "[(0-1)s:c$<s>1|_0#0-1|>:xip/syntax-dep_rel$<i>6<s>1<s>1<s>1]" +
            "[(1-2)s:e$<s>1|_1#1-2|<:xip/syntax-dep_rel$<i>9<s>1<s>1<s>1|>:xip/syntax-dep_rel$<i>4<s>1<s>1<s>1]" +             
            "[(2-3)s:c|_2#2-3]" +
            "[(3-4)s:c$<s>1|s:b$<s>2|_3#3-4|<:xip/syntax-dep_rel$<i>9<s>1<s>1<s>1]" + 
            "[(4-5)s:e$<s>1|s:d$<s>2|_4#4-5|<:xip/syntax-dep_rel$<i>1<s>1<s>1<s>1]" +
            "[(5-6)s:c|_5#5-6]" +
            "[(6-7)s:d$<s>1|_6#6-7|<:xip/syntax-dep_rel$<i>1<s>1<s>1<s>1]" +
            "[(7-8)s:e|_7#7-8]" + 
            "[(8-9)s:e|s:b|_8#8-9]" + 
            "[(9-10)s:d$<s>1|_9#9-10|>:xip/syntax-dep_rel$<i>1<s>2<s>1<s>1|>:xip/syntax-dep_rel$<i>3<s>1<s>1<s>1]");
        return fd;
    }
	
	private FieldDocument createFieldDoc1(){
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base",
            "text",             
            "[(0-1)s:c$<s>2|<>:p$#0-3$<i>3<s>1|_0#0-1|" +
            		">:xip/syntax-dep_rel$<i>3<i>6<i>9<s>2<s>1<s>1|" +
            		">:xip/syntax-dep_rel$<i>6<i>9<s>1<s>2<s>1|" +
            		"r@:func=subj$<s>2]" +
            "[(1-2)s:e|_1#1-2|<>:p#1-3$<i>3<s>1]" +             
            "[(2-3)s:c|_2#2-3]" +
            "[(3-4)s:c|s:b|_3#3-4]" + 
            "[(4-5)s:e|s:d|_4#4-5]" +
            "[(5-6)s:c|_5#5-6]" +
            "[(6-7)s:d$<s>2|<>:p$#6-9$<i>9<s>1|_6#6-7|" +
            	"<:xip/syntax-dep_rel$<i>9<b>0<i>1<s>1<s>1<s>2|" +
            	">:xip/syntax-dep_rel$<i>9<b>0<i>9<s>3<s>1<s>1|" +
            	"<:xip/syntax-dep_rel$<i>9<i>1<i>3<s>2<s>1<s>1|" +
            	"r@:func=obj$<s>2]" +
            "[(7-8)s:e|_7#7-8]" + 
            "[(8-9)s:e|s:b|_8#8-9]" + 
            "[(9-10)s:d$<s>1|_9#9-10|<:xip/syntax-dep_rel$<i>6<i>9<s>2<s>1<s>1]");
        return fd;
    }
	
	private FieldDocument createFieldDoc2(){
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base",
            "Ich kaufe die Blümen für meine Mutter.",             
            "[(0-3)s:Ich|_0#0-3|pos:NN$<s>1|<>:s#0-38$<i>7<s>2|<>:np#0-3$<i>1<s>3|" +
            	">:child-of$<i>0<i>7<s>1<s>3<s>2|" +
            	">:child-of$<i>0<i>1<s>6<s>1<s>3|" +
            	"<:child-of$<i>1<b>0<i>1<s>3<s>3<s>1|" +
            	"<:child-of$<i>7<i>0<i>1<s>4<s>2<s>3|" +
            	"<:child-of$<i>7<i>1<i>7<s>5<s>2<s>2|" +
            	"<:dep$<i>2<s>2<s>1<s>1|" +
            	"r@:func=sbj$<s>1]" +
            	
            "[(1-2)s:kaufe|_1#4-9|pos:V$<s>1|<>:vp#4-38$<i>7<s>2|" +
            	">:child-of$<i>7<i>0<i>7<s>1<s>2<s>2|" +
            	">:child-of$<i>1<i>7<s>2<s>1<s>2|" +
            	"<:child-of$<i>7<b>0<i>2<s>5<s>2<s>1|" +
            	"<:child-of$<i>7<i>2<i>7<s>6<s>2<s>4|" +
            	">:dep$<i>1<s>3<s>1<s>1|" +
            	">:dep$<i>4<s>4<s>1<s>1]" +
            	
            "[(2-3)s:die|_2#10-13|pos:ART$<s>1|tt:DET$<s>2|<>:np#10-20$<i>4<s>3|<>:np#10-38$<i>7<s>4|" +
            	">:child-of$<i>4<i>2<i>7<s>1<s>3<s>4|" +
            	">:child-of$<i>2<i>4<s>2<s>1<s>3|" +
            	">:child-of$<i>7<i>1<i>7<s>2<s>4<s>2|" +
            	"<:child-of$<i>4<b>0<i>3<s>3<s>3<s>1|" +
            	"<:child-of$<i>4<b>0<i>4<s>4<s>3<s>1|" +
            	"<:child-of$<i>7<i>2<i>4<s>5<s>4<s>3|" +
            	"<:child-of$<i>7<i>4<i>7<s>6<s>4<s>2|" +
            	"<:dep$<i>4<s>3<s>1<s>1|" +
            	"r@:func=obj$<s>1" +
            	"]" +
            
            "[(3-4)s:Blümen|_3#14-20|pos:NN$<s>1|" +
            	">:child-of$<i>2<i>4<s>1<s>1<s>3|" +            	
            	"<:dep$<i>2<s>2<s>1<s>2|" +
            	">:dep$<i>3<s>3<s>1<s>1|" +
            	">:dep$<i>5<s>4<s>1<s>1|" +
            	"r@:func=head$<s>2]" +
            	
            "[(4-5)s:für|_4#21-24|pos:PREP$<s>1|<>:pp#21-38$<i>7<s>2|" +
            	">:child-of$<i>4<i>7<s>1<s>1<s>2|" +
            	">:child-of$<i>7<i>2<i>7<s>2<s>2<s>4|" +
            	"<:child-of$<i>7<b>0<i>5<s>4<s>2<s>1|" +
            	"<:child-of$<i>7<i>5<i>7<s>5<s>2<s>2|" +
            	"<:dep$<i>4<s>1<s>1<s>1|" +
            	">:dep$<i>7<s>3<s>1<s>1" +
            	"]" +
            
            "[(5-6)s:meine|_5#25-30|pos:ART$<s>1|<>:np#25-38$<i>7<s>2|" +
            	">:child-of$<i>5<i>7<s>1<s>1<s>2|" +
            	">:child-of$<i>7<i>4<i>7<s>2<s>2<s>2|" +
            	"<:child-of$<i>7<b>0<i>6<s>4<s>2<s>1|" +
            	"<:child-of$<i>7<b>0<i>7<s>5<s>2<s>1|" +
            	"<:dep$<i>7<s>3<s>1<s>1" +
            	"]" +
            "[(6-7)s:Mutter.|_6#31-38|pos:NN$<s>1|" +
            	">:child-of$<i>5<i>7<s>1<s>1<s>2|" +
            	">:dep$<i>6<s>2<s>1<s>1|" +
            	"<:dep$<i>5<s>3<s>1<s>1|" +
            	"r@:func=head$<s>3]");
        
        return fd;
    }
	
	/** Relations: token to token, token to span, span to span
	 * */
	@Test
	public void testCase1() throws IOException {
		ki.addDoc(createFieldDoc0());
		ki.addDoc(createFieldDoc1());
		ki.commit();
		
		SpanRelationQuery sq = new SpanRelationQuery(
				new SpanTermQuery(new Term("base",">:xip/syntax-dep_rel")),true);
		kr = ki.search(sq,(short) 10);
		
		assertEquals((long) 7, kr.getTotalResults());
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
	
	/** Relation span to token
	 * */
	@Test
	public void testCase2() throws IOException {
		ki.addDoc(createFieldDoc0());
		ki.addDoc(createFieldDoc1());
		ki.commit();
		
		SpanRelationQuery sq = new SpanRelationQuery(
				new SpanTermQuery(new Term("base","<:xip/syntax-dep_rel")),true);
		kr = ki.search(sq,(short) 10);
		
		assertEquals((long) 7, kr.getTotalResults());
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
	
	/** Relations with attributes
	 * */
	@Test
	public void testCase3() throws IOException {
		ki.addDoc(createFieldDoc2());
		ki.commit();
		
		// child-of relations
		SpanRelationQuery srq = new SpanRelationQuery(
				new SpanTermQuery(new Term("base",">:child-of")),true);
		kr = ki.search(srq,(short) 20);
		
		assertEquals((long) 13, kr.getTotalResults());
		
		// child-of with attr func=sbj
		SpanWithAttributeQuery wq = 
			new SpanWithAttributeQuery(srq, 
				new SpanAttributeQuery( 
					new SpanTermQuery(new Term("base", "r@:func=sbj")),
					true), 
				true
		);
		
		kr = ki.search(wq,(short) 10);		
		assertEquals((long) 1, kr.getTotalResults());
		assertEquals(0,kr.getMatch(0).getStartPos()); // token
		assertEquals(1,kr.getMatch(0).getEndPos());
		
		// child-of without attr func=sbj
		wq = 
			new SpanWithAttributeQuery(srq, 
				new SpanAttributeQuery( 
					new SpanTermQuery(new Term("base", "r@:func=sbj")),
					true, true), 
				true
		);
		kr = ki.search(wq,(short) 20);
		assertEquals((long) 12, kr.getTotalResults());
		
		// child-of with attr func-obj
		wq = new SpanWithAttributeQuery(srq, 
				new SpanAttributeQuery( 
					new SpanTermQuery( new Term("base", "r@:func=obj")),
					true), 
				true
		);
		
		kr = ki.search(wq,(short) 10);
		
		assertEquals((long) 1, kr.getTotalResults());
		assertEquals(2,kr.getMatch(0).getStartPos()); // element
		assertEquals(4,kr.getMatch(0).getEndPos());
			
		// target of a dependency relation		
		srq = new SpanRelationQuery(
				new SpanTermQuery(new Term("base","<:dep")),true);
		kr = ki.search(srq,(short) 10);
		
		assertEquals((long) 6, kr.getTotalResults());
		
		// target of a dependency relation, which is also a head
		wq = new SpanWithAttributeQuery(srq, 
					new SpanAttributeQuery( 
						new SpanTermQuery( new Term("base", "r@:func=head")),
						true), 
					true
			);
		
		kr = ki.search(wq,(short) 20);
		
		assertEquals((long) 2, kr.getTotalResults());
		assertEquals(3,kr.getMatch(0).getStartPos());
		assertEquals(4,kr.getMatch(0).getEndPos());
		assertEquals(6,kr.getMatch(1).getStartPos());
		assertEquals(7,kr.getMatch(1).getEndPos());
		
		
	}
	
	/** Match left return left
	 *  Match right return right
	 * */
	@Test
    public void testCase10() throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();
        
        // return all children that are NP
        SpanRelationPartQuery rv = new SpanRelationPartQuery(
                new SpanRelationQuery(
                        new SpanTermQuery(new Term("base",">:child-of")),true
                ), 
                new SpanElementQuery("base","np"), 
                false, false, true); 
                
        kr = ki.search(rv,(short) 10);
        
        assertEquals(4, kr.getTotalResults());
        assertEquals(0,kr.getMatch(0).getStartPos());
        assertEquals(1,kr.getMatch(0).getEndPos());
        assertEquals(2,kr.getMatch(1).getStartPos());
        assertEquals(4,kr.getMatch(1).getEndPos());
        assertEquals(2,kr.getMatch(2).getStartPos());
        assertEquals(7,kr.getMatch(2).getEndPos());
        assertEquals(5,kr.getMatch(3).getStartPos());
        assertEquals(7,kr.getMatch(3).getEndPos());
        
        // return all parents that are NP
        rv = new SpanRelationPartQuery(
                new SpanRelationQuery(
                        new SpanTermQuery(new Term("base",">:child-of")),true
                ), 
                new SpanElementQuery("base","np"), 
                true, true, true); 
                
        kr = ki.search(rv,(short) 10);

        assertEquals(7, kr.getTotalResults());
        assertEquals(0,kr.getMatch(0).getStartPos());
        assertEquals(1,kr.getMatch(0).getEndPos());
        assertEquals(2,kr.getMatch(1).getStartPos());
        assertEquals(4,kr.getMatch(1).getEndPos());
        assertEquals(2,kr.getMatch(2).getStartPos());
        assertEquals(4,kr.getMatch(2).getEndPos());
        assertEquals(2,kr.getMatch(3).getStartPos());
        assertEquals(7,kr.getMatch(3).getEndPos());
        assertEquals(2,kr.getMatch(4).getStartPos());
        assertEquals(7,kr.getMatch(4).getEndPos());
        assertEquals(5,kr.getMatch(5).getStartPos());
        assertEquals(7,kr.getMatch(5).getEndPos());
        assertEquals(5,kr.getMatch(6).getStartPos());
        assertEquals(7,kr.getMatch(6).getEndPos());     
        
//       for (KorapMatch km : kr.getMatches()){        
//            System.out.println(km.getStartPos() +","+km.getEndPos()
//                    //  +" "+km.getSnippetBrackets()
//            );
//       }  
    

   }
	
	
	/** Relation with variable
	 * 	match right, return left
	 * 	sort by right, then sort by left
	 * @throws IOException 
	 * */
	@Test
	public void testCase4() throws IOException {
		ki.addDoc(createFieldDoc2());
		ki.commit();
		
		//return all children of np
		SpanRelationPartQuery rv = new SpanRelationPartQuery(
				new SpanRelationQuery(
						new SpanTermQuery(new Term("base",">:child-of")),true
				), 
				new SpanElementQuery("base","np"), 
				true, false, true); 
				
		kr = ki.search(rv,(short) 10);
		
		assertEquals((long) 7, kr.getTotalResults());
		assertEquals(0,kr.getMatch(0).getStartPos());
		assertEquals(1,kr.getMatch(0).getEndPos());
		assertEquals(2,kr.getMatch(1).getStartPos());
		assertEquals(3,kr.getMatch(1).getEndPos());
		assertEquals(2,kr.getMatch(2).getStartPos());
		assertEquals(4,kr.getMatch(2).getEndPos());
		assertEquals(3,kr.getMatch(3).getStartPos());
		assertEquals(4,kr.getMatch(3).getEndPos());
		assertEquals(4,kr.getMatch(4).getStartPos());
		assertEquals(7,kr.getMatch(4).getEndPos());
		assertEquals(5,kr.getMatch(5).getStartPos());
		assertEquals(6,kr.getMatch(5).getEndPos());
		assertEquals(6,kr.getMatch(6).getStartPos());
		assertEquals(7,kr.getMatch(6).getEndPos());		
		// sorting left problem (solved)
		
		//return all children of np that are articles
		SpanSegmentQuery rv2 = new SpanSegmentQuery(rv, new SpanTermQuery(new Term("base","pos:ART")));		
		kr = ki.search(rv2,(short) 10);
				
		assertEquals((long) 2, kr.getTotalResults());
		assertEquals(2,kr.getMatch(0).getStartPos());
		assertEquals(3,kr.getMatch(0).getEndPos());
		assertEquals(5,kr.getMatch(1).getStartPos());
		assertEquals(6,kr.getMatch(1).getEndPos());
		
		// return all nps whose children are articles
		SpanRelationPartQuery rv3 = 
			new SpanRelationPartQuery(rv, 
				new SpanTermWithIdQuery(new Term("base","pos:ART"), true), 
				false, true, true); 
		kr = ki.search(rv3,(short) 10);
		
		/*for (KorapMatch km : kr.getMatches()){		
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
				+km.getSnippetBrackets()
					);
		}	*/
		assertEquals((long) 2, kr.getTotalResults());
		assertEquals(2,kr.getMatch(0).getStartPos());
		assertEquals(4,kr.getMatch(0).getEndPos());
		assertEquals(5,kr.getMatch(1).getStartPos());
		assertEquals(7,kr.getMatch(1).getEndPos());
	}
	
	/** Using the opposite relation
	 * 	match left, return right
	 * 	sort by left, then sort by right
	 * */
	@Test
	public void testCase5() throws IOException {
		ki.addDoc(createFieldDoc2());
		ki.commit();
		
		SpanRelationQuery spanRelationQuery =new SpanRelationQuery(
				new SpanTermQuery(new Term("base","<:child-of")),true
		);
		
		//return all children of np
		SpanRelationPartQuery rv =new SpanRelationPartQuery(
				spanRelationQuery, 
				new SpanElementQuery("base","np"), 
				false, true, true);
		kr = ki.search(rv,(short) 10);
		
		/*for (KorapMatch km : kr.getMatches()){
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
				+km.getSnippetBrackets());
			}*/
		
		assertEquals((long) 7, kr.getTotalResults());
		assertEquals(0,kr.getMatch(0).getStartPos());
		assertEquals(1,kr.getMatch(0).getEndPos());
		assertEquals(2,kr.getMatch(1).getStartPos());
		assertEquals(3,kr.getMatch(1).getEndPos());
		assertEquals(2,kr.getMatch(2).getStartPos());
		assertEquals(4,kr.getMatch(2).getEndPos());
		assertEquals(3,kr.getMatch(3).getStartPos());
		assertEquals(4,kr.getMatch(3).getEndPos());
		assertEquals(4,kr.getMatch(4).getStartPos());
		assertEquals(7,kr.getMatch(4).getEndPos());
		assertEquals(5,kr.getMatch(5).getStartPos());
		assertEquals(6,kr.getMatch(5).getEndPos());
		assertEquals(6,kr.getMatch(6).getStartPos());
		assertEquals(7,kr.getMatch(6).getEndPos());	
	}
	
	/** Match right, return left
	 * 	sort by right, then sort by left
	 * */
	@Test
	public void testCase6() throws IOException {
		ki.addDoc(createFieldDoc2());
		ki.commit();
		
		SpanRelationQuery spanRelationQuery =new SpanRelationQuery(
				new SpanTermQuery(new Term("base","<:child-of")),true
		);
		
		//return all parents of np
		SpanRelationPartQuery rv =new SpanRelationPartQuery(
				spanRelationQuery, 
				new SpanElementQuery("base","np"), 
				true, false, true);
		kr = ki.search(rv,(short) 10);
		
		/*for (KorapMatch km : kr.getMatches()){
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
				+km.getSnippetBrackets());
			}*/
		assertEquals((long) 4, kr.getTotalResults());
		assertEquals(0,kr.getMatch(0).getStartPos());
		assertEquals(7,kr.getMatch(0).getEndPos());
		assertEquals(1,kr.getMatch(1).getStartPos());
		assertEquals(7,kr.getMatch(1).getEndPos());
		assertEquals(2,kr.getMatch(2).getStartPos());
		assertEquals(7,kr.getMatch(2).getEndPos());
		assertEquals(4,kr.getMatch(3).getStartPos());
		assertEquals(7,kr.getMatch(3).getEndPos());
		// id problem same like testcase7 (solved)
	}
	
	/** Match left, return right
	 * 	sort by left, then sort by right
	 * */
	@Test
	public void testCase7() throws IOException {
		ki.addDoc(createFieldDoc2());
		ki.commit();
		
		//return all parents of np
		SpanRelationPartQuery rv =new SpanRelationPartQuery(
				new SpanRelationQuery(
						new SpanTermQuery(new Term("base",">:child-of")),true
				), 
				new SpanElementQuery("base","np"), 
				false, true, true);
		kr = ki.search(rv,(short) 10);
		/*for (KorapMatch km : kr.getMatches()){
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
				+km.getSnippetBrackets());
			}*/
		assertEquals((long) 4, kr.getTotalResults());
		assertEquals(0,kr.getMatch(0).getStartPos());
		assertEquals(7,kr.getMatch(0).getEndPos());
		assertEquals(1,kr.getMatch(1).getStartPos());
		assertEquals(7,kr.getMatch(1).getEndPos());
		assertEquals(2,kr.getMatch(2).getStartPos());
		assertEquals(7,kr.getMatch(2).getEndPos());
		assertEquals(4,kr.getMatch(3).getStartPos());
		assertEquals(7,kr.getMatch(3).getEndPos());
		// id problem (solved)
		
		// return all children of relation targets/ right side		
		SpanRelationPartQuery rv3 = new SpanRelationPartQuery(				
				new SpanRelationQuery(
						new SpanTermQuery(new Term("base",">:child-of")),true
				),
				rv, true, false, true);
				
		kr = ki.search(rv3,(short) 10);
		
		/*for (KorapMatch km : kr.getMatches()){		
		System.out.println(km.getStartPos() +","+km.getEndPos()+" "
			+km.getSnippetBrackets());
		}*/		
		assertEquals((long) 8, kr.getTotalResults());
	}
	
	/** Relations whose source/target do not embed 
	 * 	its counterparts.
	 * */	
	@Test
	public void testCase8() throws IOException {
		ki.addDoc(createFieldDoc2());
		ki.commit();
		
		// match right
		
		//return source of dep relations to pos:NN
		SpanRelationPartQuery rv =new SpanRelationPartQuery(
				new SpanRelationQuery(
						new SpanTermQuery(new Term("base",">:dep")),true
				), 
				new SpanTermWithIdQuery(new Term("base","pos:NN"), true), 
				true, false, true);
		kr = ki.search(rv,(short) 10);
		assertEquals((long) 3, kr.getTotalResults());
		assertEquals(1,kr.getMatch(0).getStartPos());
		assertEquals(2,kr.getMatch(0).getEndPos());
		assertEquals(1,kr.getMatch(1).getStartPos());
		assertEquals(2,kr.getMatch(1).getEndPos());
		assertEquals(4,kr.getMatch(2).getStartPos());
		assertEquals(5,kr.getMatch(2).getEndPos());
		
		//return target of dep relations from pos:NN
		rv =new SpanRelationPartQuery(
				new SpanRelationQuery(
						new SpanTermQuery(new Term("base","<:dep")),true
				), 
				new SpanTermWithIdQuery(new Term("base","pos:NN"),true), 
				true, false, true);
		kr = ki.search(rv,(short) 10);
		assertEquals((long) 3, kr.getTotalResults());
		assertEquals(2,kr.getMatch(0).getStartPos());
		assertEquals(3,kr.getMatch(0).getEndPos());
		assertEquals(4,kr.getMatch(1).getStartPos());
		assertEquals(5,kr.getMatch(1).getEndPos());
		assertEquals(5,kr.getMatch(2).getStartPos());
		assertEquals(6,kr.getMatch(2).getEndPos());

		// matchleft
		
		//return target of dep relations from pos:NN
		rv =new SpanRelationPartQuery(
				new SpanRelationQuery(
						new SpanTermQuery(new Term("base",">:dep")),true
				), 
				new SpanTermWithIdQuery(new Term("base","pos:NN"),true), 
				false, true, true);
		kr = ki.search(rv,(short) 10);
			
		assertEquals((long) 3, kr.getTotalResults());
		assertEquals(2,kr.getMatch(0).getStartPos());
		assertEquals(3,kr.getMatch(0).getEndPos());
		assertEquals(4,kr.getMatch(1).getStartPos());
		assertEquals(5,kr.getMatch(1).getEndPos());
		assertEquals(5,kr.getMatch(2).getStartPos());
		assertEquals(6,kr.getMatch(2).getEndPos());
		
		//return source of dep relations to pos:NN		
		rv =new SpanRelationPartQuery(
				new SpanRelationQuery(
						new SpanTermQuery(new Term("base","<:dep")),true
				), 
				new SpanTermWithIdQuery(new Term("base","pos:NN"),true), 
				false, true, true);
		kr = ki.search(rv,(short) 10);
		
		/*for (KorapMatch km : kr.getMatches()){		
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
				+km.getSnippetBrackets());
			}*/
		
		assertEquals((long) 3, kr.getTotalResults());
		assertEquals(1,kr.getMatch(0).getStartPos());
		assertEquals(2,kr.getMatch(0).getEndPos());
		assertEquals(1,kr.getMatch(1).getStartPos());
		assertEquals(2,kr.getMatch(1).getEndPos());
		assertEquals(4,kr.getMatch(2).getStartPos());
		assertEquals(5,kr.getMatch(2).getEndPos());
	}
	
	/** Window
	 * */
	@Test
	public void testCase9() throws IOException {
		ki.addDoc(createFieldDoc2());
		ki.commit();
				
		SpanRelationPartQuery rv = new SpanRelationPartQuery(
				new SpanRelationQuery(
						new SpanTermQuery(new Term("base",">:child-of")),true
				), 
				new SpanElementQuery("base","np"), 
				6, true, false, true); 
				
		kr = ki.search(rv,(short) 10);
		assertEquals((long) 7, kr.getTotalResults());
		
		rv =new SpanRelationPartQuery(
				new SpanRelationQuery(
						new SpanTermQuery(new Term("base","<:dep")),true
				), 
				new SpanTermWithIdQuery(new Term("base","pos:NN"),true), 
				3, false, true, true);
		kr = ki.search(rv,(short) 10);
		
		/*for (KorapMatch km : kr.getMatches()){
			System.out.println(km.getStartPos() +","+km.getEndPos()
				+" "+km.getSnippetBrackets()
				);
		}*/
		
		assertEquals((long) 3, kr.getTotalResults());
		assertEquals(1,kr.getMatch(0).getStartPos());
		assertEquals(2,kr.getMatch(0).getEndPos());
		assertEquals(1,kr.getMatch(1).getStartPos());
		assertEquals(2,kr.getMatch(1).getEndPos());
		assertEquals(4,kr.getMatch(2).getStartPos());
		assertEquals(5,kr.getMatch(2).getEndPos());
		
	}
}

