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
	assertEquals("spanNear([field1:a, field1:b], -1, false)", sq.toString());

	sq = new KorapQuery("field2").seg("a", "b").with("c").toQuery();
	assertEquals("spanNear([spanNear([field2:a, field2:b], -1, false), field2:c], -1, false)", sq.toString());
    };

    @Test
    public void korapQueryRegexSegment () {
	KorapQuery kq = new KorapQuery("field1");
	SpanQuery sq = kq.seg("a").with(kq.re("b.*c")).toQuery();
	assertEquals("spanNear([field1:a, SpanMultiTermQueryWrapper(field1:/b.*c/)], -1, false)", sq.toString());

	kq = new KorapQuery("field2");
	sq = kq.seg(kq.re("a.*")).with("b").toQuery();
	assertEquals("spanNear([SpanMultiTermQueryWrapper(field2:/a.*/), field2:b], -1, false)", sq.toString());
    };

    @Test
    public void korapQueryRegexSegment2 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seg("a").with(kq.or("b").or("c")).toQuery();
	assertEquals("spanNear([field:a, spanOr([field:b, field:c])], -1, false)", sq.toString());

	kq = new KorapQuery("field");
	sq = kq.seg("a").with(kq.or("b", "c")).toQuery();
	assertEquals("spanNear([field:a, spanOr([field:b, field:c])], -1, false)", sq.toString());


	kq = new KorapQuery("field");
	// [ a & (b | /c.*d/) ]
	sq = kq.seg("a").with(kq.or("b").or(kq.re("c.*d"))).toQuery();
	assertEquals("spanNear([field:a, spanOr([field:b, SpanMultiTermQueryWrapper(field:/c.*d/)])], -1, false)", sq.toString());
    };

    @Test
    public void korapQuerySequenceSegment () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("a").with(kq.or("b", "c"))).append("d").append(kq.re("e.?f")).toQuery();
	assertEquals("spanNext(spanNext(spanNear([field:a, spanOr([field:b, field:c])], -1, false), field:d), SpanMultiTermQueryWrapper(field:/e.?f/))", sq.toString());
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
	SpanQuery sq = kq.within(kq.tag("s"), kq.tag("np")).toQuery();
	assertEquals("spanWithin(<field:s />, <field:np />)", sq.toString());
    };

    @Test
    public void KorapTagQuery6 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("tree"), kq.within(kq.tag("s"), kq.tag("np")), kq.re("hey.*")).toQuery();
	assertEquals("spanNext(spanNext(field:tree, spanWithin(<field:s />, <field:np />)), SpanMultiTermQueryWrapper(field:/hey.*/))", sq.toString());
    };


    @Test
    public void KorapClassQuery () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("tree"), kq._(1, kq.within(kq.tag("s"), kq.tag("np"))), kq.re("hey.*")).toQuery();
	assertEquals("spanNext(spanNext(field:tree, {1: spanWithin(<field:s />, <field:np />)}), SpanMultiTermQueryWrapper(field:/hey.*/))", sq.toString());
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
	SpanQuery sq = kq.seq(kq.seg("tree"), kq.within(kq.tag("s"), kq._(kq.tag("np"))), kq.re("hey.*")).toQuery();
	assertEquals("spanNext(spanNext(field:tree, spanWithin(<field:s />, {0: <field:np />})), SpanMultiTermQueryWrapper(field:/hey.*/))", sq.toString());
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
	assertEquals("shrink(1: {1: spanNext(<field:np />, {0: spanNot(field:test, field:no)})})", sq.toString());
    };

    @Test
    public void KorapShrinkQuery4 () {
	KorapQuery kq = new KorapQuery("field");
	SpanQuery sq = kq.seq(kq.seg("try1"), kq.shrink(1, kq._(1, kq.seg("try2"))), kq.seg("try3")).toQuery();
	assertEquals("spanNext(spanNext(field:try1, shrink(1: {1: field:try2})), field:try3)", sq.toString());
    };

    // kq.seg("a").append(kq.ANY).append("b:c");
    // kq.repeat(kq.seg("a", "b"), 5)
};
