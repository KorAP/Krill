package de.ids_mannheim.korap.query;

import java.util.*;
import org.apache.lucene.search.spans.SpanQuery;
import de.ids_mannheim.korap.KorapQuery;


import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestKorapQuery {

    @Test
    public void korapQuerySegment () {
	SpanQuery sq = new KorapQuery("field1").seg("a").with("b").toQuery();
	assertEquals("spanSegment(field1:a, field1:b)", sq.toString());

	sq = new KorapQuery("field2").seg("a", "b").with("c").toQuery();
	assertEquals("spanSegment(spanSegment(field2:a, field2:b), field2:c)", sq.toString());
    };

    @Test
    public void korapQueryRegexSegment () {
	KorapQuery kq = new KorapQuery("field1");
	SpanQuery sq = kq.seg("a").with(kq.re("b.*c")).toQuery();
	assertEquals("spanSegment(field1:a, SpanMultiTermQueryWrapper(field1:/b.*c/))", sq.toString());

	kq = new KorapQuery("field2");
	sq = kq.seg(kq.re("a.*")).with("b").toQuery();
	assertEquals("spanSegment(SpanMultiTermQueryWrapper(field2:/a.*/), field2:b)", sq.toString());
    };

    @Test
    public void korapQueryRegexSegment2 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seg("a").with(kq.or("b").or("c")).toQuery();
	assertEquals("spanSegment(field:a, spanOr([field:b, field:c]))", sq.toString());

	kq = new KorapQuery("field");
	sq = kq.seg("a").with(kq.or("b", "c")).toQuery();
	assertEquals("spanSegment(field:a, spanOr([field:b, field:c]))", sq.toString());


	kq = new KorapQuery("field");
	// [ a & (b | /c.*d/) ]
	sq = kq.seg("a").with(kq.or("b").or(kq.re("c.*d"))).toQuery();
	assertEquals("spanSegment(field:a, spanOr([field:b, SpanMultiTermQueryWrapper(field:/c.*d/)]))", sq.toString());
    };

    @Test
    public void korapQuerySequenceSegment () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("a").with(kq.or("b", "c"))).append("d").append(kq.re("e.?f")).toQuery();
	assertEquals("spanNext(spanNext(spanSegment(field:a, spanOr([field:b, field:c])), field:d), SpanMultiTermQueryWrapper(field:/e.?f/))", sq.toString());
    };

    @Test
    public void KorapTagQuery () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.tag("np").toQuery();
	assertEquals("<field:np />", sq.toString());
    };

    @Test
    public void KorapTagQuery2 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.or(kq.tag("np"), kq.tag("vp")).toQuery();
	assertEquals("spanOr([<field:np />, <field:vp />])", sq.toString());
    };

    @Test
    public void KorapTagQuery3 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.tag("np"), kq.tag("vp")).toQuery();
	assertEquals("spanNext(<field:np />, <field:vp />)", sq.toString());
    };

    @Test
    public void KorapTagQuery4 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.tag("np"), kq.tag("vp")).append("test").toQuery();
	assertEquals("spanNext(spanNext(<field:np />, <field:vp />), field:test)", sq.toString());
    };

    @Test
    public void KorapTagQuery5 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.contains(kq.tag("s"), kq.tag("np")).toQuery();
	assertEquals("spanContain(<field:s />, <field:np />)", sq.toString());
    };

    @Test
    public void KorapTagQuery6 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("tree"), kq.contains(kq.tag("s"), kq.tag("np")), kq.re("hey.*")).toQuery();
	assertEquals("spanNext(spanNext(field:tree, spanContain(<field:s />, <field:np />)), SpanMultiTermQueryWrapper(field:/hey.*/))", sq.toString());
    };


    @Test
    public void KorapClassQuery () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("tree"), kq._(1, kq.contains(kq.tag("s"), kq.tag("np"))), kq.re("hey.*")).toQuery();
	assertEquals("spanNext(spanNext(field:tree, {1: spanContain(<field:s />, <field:np />)}), SpanMultiTermQueryWrapper(field:/hey.*/))", sq.toString());
    };

    @Test
    public void KorapClassQuery2 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq._(kq.seg("base:test")).toQuery();
	assertEquals("{0: field:base:test}", sq.toString());
    };

    @Test
    public void KorapClassQuery3 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("tree"), kq.contains(kq.tag("s"), kq._(kq.tag("np"))), kq.re("hey.*")).toQuery();
	assertEquals("spanNext(spanNext(field:tree, spanContain(<field:s />, {0: <field:np />})), SpanMultiTermQueryWrapper(field:/hey.*/))", sq.toString());
    };

    @Test
    public void KorapShrinkQuery () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.shrink(kq.tag("np")).toQuery();
	assertEquals("shrink(0: <field:np />)", sq.toString());
    };

    @Test
    public void KorapShrinkQuery1 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.shrink(1, kq.tag("np")).toQuery();
	assertEquals("shrink(1: <field:np />)", sq.toString());
    };

    @Test
    public void KorapShrinkQuery2 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.shrink(1, kq._(1, kq.tag("np"))).toQuery();
	assertEquals("shrink(1: {1: <field:np />})", sq.toString());
    };

    @Test
    public void KorapShrinkQuery3 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.shrink(1, kq._(1, kq.seq(kq.tag("np"), kq._(kq.seg("test").without("no"))))).toQuery();
	assertEquals("shrink(1: {1: spanNext(<field:np />, {0: spanNot(field:test, field:no, 0, 0)})})", sq.toString());
    };

    @Test
    public void KorapShrinkQuery4 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("try1"), kq.shrink(1, kq._(1, kq.seg("try2"))), kq.seg("try3")).toQuery();
	assertEquals("spanNext(spanNext(field:try1, shrink(1: {1: field:try2})), field:try3)", sq.toString());
    };


    @Test
    public void KorapSequenceQuery1 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2")).toQuery();
	assertEquals("spanNext(field:try1, field:try2)", sq.toString());
    };

    @Test
    public void KorapSequenceQuery2 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2")).withConstraint(2,3).toQuery();
	assertEquals("spanDistance(field:try1, field:try2, [(w[2:3], ordered, notExcluded)])", sq.toString());
    };

    @Test
    public void KorapSequenceQuery3 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2")).withConstraint(2,3, "s").toQuery();
	assertEquals("spanElementDistance(field:try1, field:try2, [(s[2:3], ordered, notExcluded)])", sq.toString());
    };

    @Test
    public void KorapSequenceQuery4 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2")).withConstraint(2,3,"s").withConstraint(5,6,"w").toQuery();
	assertEquals("spanMultipleDistance(field:try1, field:try2, [(s[2:3], ordered, notExcluded), (w[5:6], ordered, notExcluded)])", sq.toString());
    };

    @Test
    public void KorapSequenceQuery5 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2")).withConstraint(2,3,true).toQuery();
	assertEquals("spanDistance(field:try1, field:try2, [(w[2:3], ordered, excluded)])", sq.toString());
    };

    @Test
    public void KorapSequenceQuery6 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2")).withConstraint(2,3,"s", true).toQuery();
	assertEquals("spanElementDistance(field:try1, field:try2, [(s[2:3], ordered, excluded)])", sq.toString());
    };

    @Test
    public void KorapSequenceQuery7 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2")).withConstraint(5,6).withConstraint(2,3,"s",true).toQuery();
	assertEquals("spanMultipleDistance(field:try1, field:try2, [(w[5:6], ordered, notExcluded), (s[2:3], ordered, excluded)]])", sq.toString());
    };

    @Test
    public void KorapSequenceQuery8 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2")).append("try3").withConstraint(5,6).withConstraint(2,3,"s",true).toQuery();
	assertEquals("spanMultipleDistance(spanMultipleDistance(field:try1, field:try2, [(w[5:6], ordered, notExcluded), (s[2:3], ordered, excluded)]]), field:try3, [(w[5:6], ordered, notExcluded), (s[2:3], ordered, excluded)]])", sq.toString());
    };


    @Test
    public void KorapWithinQuery1 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.contains(kq.seg("test"), kq.seg("test2")).toQuery();
	assertEquals("spanContain(field:test, field:test2)", sq.toString());
    };

    @Test
    public void KorapWithinQuery2 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.overlaps(kq.seg("test"), kq.seg("test2")).toQuery();
	assertEquals("spanOverlap(field:test, field:test2)", sq.toString());
    };

    @Test
    public void KorapWithinQuery3 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.startswith(kq.seg("test"), kq.seg("test2")).toQuery();
	assertEquals("spanStartsWith(field:test, field:test2)", sq.toString());
    };
   
    // kq.seg("a").append(kq.ANY).append("b:c");
    // kq.repeat(kq.seg("a", "b"), 5)
};
