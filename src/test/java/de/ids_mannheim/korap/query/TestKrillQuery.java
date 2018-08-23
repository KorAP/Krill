package de.ids_mannheim.korap.query;

import java.util.*;
import org.apache.lucene.search.spans.SpanQuery;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.QueryBuilder;

import de.ids_mannheim.korap.util.QueryException;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


/**
 * @author diewald
 */
@RunWith(JUnit4.class)
public class TestKrillQuery {

    // TODO: Better rename this to Builder

    @Test
    public void korapQuerySegment () throws QueryException {
        SpanQuery sq = new QueryBuilder("field1").seg("a").with("b").toQuery();
        assertEquals("spanSegment(field1:a, field1:b)", sq.toString());

        sq = new QueryBuilder("field2").seg("a", "b").with("c").toQuery();
        assertEquals("spanSegment(spanSegment(field2:a, field2:b), field2:c)",
                sq.toString());
    };


    @Test
    public void korapQueryRegexSegment () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field1");
        SpanQuery sq = kq.seg("a").with(kq.re("b.*c")).toQuery();
        assertEquals(
                "spanSegment(field1:a, SpanMultiTermQueryWrapper(field1:/b.*c/))",
                sq.toString());

        kq = new QueryBuilder("field2");
        sq = kq.seg(kq.re("a.*")).with("b").toQuery();
        assertEquals(
                "spanSegment(SpanMultiTermQueryWrapper(field2:/a.*/), field2:b)",
                sq.toString());
    };


    @Test
    public void korapQueryRegexSegment2 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seg("a").with(kq.or("b").or("c")).toQuery();
        assertEquals("spanSegment(field:a, spanOr([field:b, field:c]))",
                sq.toString());

        kq = new QueryBuilder("field");
        sq = kq.seg("a").with(kq.or("b", "c")).toQuery();
        assertEquals("spanSegment(field:a, spanOr([field:b, field:c]))",
                sq.toString());

        kq = new QueryBuilder("field");
        // [ a & (b | /c.*d/) ]
        sq = kq.seg("a").with(kq.or("b").or(kq.re("c.*d"))).toQuery();
        assertEquals(
                "spanSegment(field:a, spanOr([field:b, SpanMultiTermQueryWrapper(field:/c.*d/)]))",
                sq.toString());
    };


    @Test
    public void korapQuerySequenceSegment () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("a").with(kq.or("b", "c"))).append("d")
                .append(kq.re("e.?f")).toQuery();
        assertEquals(
                "spanNext(spanNext(spanSegment(field:a, spanOr([field:b, field:c])), field:d), SpanMultiTermQueryWrapper(field:/e.?f/))",
                sq.toString());
    };


    @Test
    public void KorapTagQuery () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.tag("np").toQuery();
        assertEquals("<field:np />", sq.toString());
    };


    @Test
    public void KorapTagQuery2 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.or(kq.tag("np"), kq.tag("vp")).toQuery();
        assertEquals("spanOr([<field:np />, <field:vp />])", sq.toString());
    };


    @Test
    public void KorapTagQuery3 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.tag("np"), kq.tag("vp")).toQuery();
        assertEquals("spanNext(<field:np />, <field:vp />)", sq.toString());
    };


    @Test
    public void KorapTagQuery4 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.tag("np"), kq.tag("vp")).append("test")
                .toQuery();
        assertEquals(
                "spanNext(spanNext(<field:np />, <field:vp />), field:test)",
                sq.toString());
    };


    @Test
    public void KorapTagQuery5 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.contains(kq.tag("s"), kq.tag("np")).toQuery();
        assertEquals("spanContain(<field:s />, <field:np />)", sq.toString());
    };


    @Test
    public void KorapTagQuery6 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("tree"),
                kq.contains(kq.tag("s"), kq.tag("np")), kq.re("hey.*"))
                .toQuery();
        assertEquals(
                "spanNext(spanNext(field:tree, spanContain(<field:s />, <field:np />)), SpanMultiTermQueryWrapper(field:/hey.*/))",
                sq.toString());
    };


    @Test
    public void KorapClassQuery () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("tree"),
                kq.nr(1, kq.contains(kq.tag("s"), kq.tag("np"))),
                kq.re("hey.*")).toQuery();
        assertEquals(
                "spanNext(spanNext(field:tree, {1: spanContain(<field:s />, <field:np />)}), SpanMultiTermQueryWrapper(field:/hey.*/))",
                sq.toString());
    };


    @Test
    public void KorapClassQuery2 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.nr(kq.seg("base:test")).toQuery();
        assertEquals("{1: field:base:test}", sq.toString());
    };


    @Test
    public void KorapClassQuery3 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("tree"),
                kq.contains(kq.tag("s"), kq.nr(kq.tag("np"))), kq.re("hey.*"))
                .toQuery();
        assertEquals(
                "spanNext(spanNext(field:tree, spanContain(<field:s />, {1: <field:np />})), SpanMultiTermQueryWrapper(field:/hey.*/))",
                sq.toString());
    };


    @Test
    public void KorapShrinkQuery () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.focus(kq.tag("np")).toQuery();
        assertEquals("focus(1: <field:np />)", sq.toString());
    };


    @Test
    public void KorapShrinkQuery1 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.focus(1, kq.tag("np")).toQuery();
        assertEquals("focus(1: <field:np />)", sq.toString());
    };


    @Test
    public void KorapShrinkQuery2 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.focus(1, kq.nr(1, kq.tag("np"))).toQuery();
        assertEquals("focus(1: {1: <field:np />})", sq.toString());
    };


    @Test
    public void KorapShrinkQuery3 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.focus(
                1,
                kq.nr(1,
                        kq.seq(kq.tag("np"),
                                kq.nr(kq.seg("test").without("no")))))
                .toQuery();
        assertEquals(
                "focus(1: {1: spanNext(<field:np />, {1: spanNot(field:test, field:no, 0, 0)})})",
                sq.toString());
    };


    @Test
    public void KorapShrinkQuery4 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("try1"),
                kq.focus(1, kq.nr(1, kq.seg("try2"))), kq.seg("try3"))
                .toQuery();
        assertEquals(
                "spanNext(spanNext(field:try1, focus(1: {1: field:try2})), field:try3)",
                sq.toString());
    };


    @Test
    public void KorapSequenceQuery1 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2")).toQuery();
        assertEquals("spanNext(field:try1, field:try2)", sq.toString());
    };


    @Test
    public void KorapSequenceQuery2 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2"))
                .withConstraint(2, 3).toQuery();
        assertEquals(
                "spanDistance(field:try1, field:try2, [(w[2:3], ordered, notExcluded)])",
                sq.toString());
    };


    @Test
    public void KorapSequenceQuery3 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2"))
                .withConstraint(2, 3, "s").toQuery();
        assertEquals(
                "spanElementDistance(field:try1, field:try2, [(s[2:3], ordered, notExcluded)])",
                sq.toString());
    };


    @Test
    public void KorapSequenceQuery4 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2"))
                .withConstraint(2, 3, "s").withConstraint(5, 6, "w").toQuery();
        assertEquals(
                "spanMultipleDistance(field:try1, field:try2, [(s[2:3], ordered, notExcluded), (w[5:6], ordered, notExcluded)])",
                sq.toString());
    };


    @Test
    public void KorapSequenceQuery5 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2"))
                .withConstraint(2, 3, true).toQuery();
        assertEquals(
                "spanDistance(field:try1, field:try2, [(w[2:3], ordered, excluded)])",
                sq.toString());
    };


    @Test
    public void KorapSequenceQuery6 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2"))
                .withConstraint(2, 3, "s", true).toQuery();
        assertEquals(
                "spanElementDistance(field:try1, field:try2, [(s[2:3], ordered, excluded)])",
                sq.toString());
    };


    @Test
    public void KorapSequenceQuery7 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2"))
                .withConstraint(5, 6).withConstraint(2, 3, "s", true).toQuery();
        assertEquals(
                "spanMultipleDistance(field:try1, field:try2, [(w[5:6], ordered, notExcluded), (s[2:3], ordered, excluded)])",
                sq.toString());
    };


    @Test
    public void KorapSequenceQuery8 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("try1")).append(kq.seg("try2"))
                .append("try3").withConstraint(5, 6)
                .withConstraint(2, 3, "s", true).toQuery();
        assertEquals(
                "spanMultipleDistance(spanMultipleDistance(field:try1, field:try2, [(w[5:6], ordered, notExcluded), (s[2:3], ordered, excluded)]), field:try3, [(w[5:6], ordered, notExcluded), (s[2:3], ordered, excluded)])",
                sq.toString());
    };


    @Test
    public void KorapSequenceWithEmptyRepetitionQuery () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.seq(kq.seg("try"))
                .append(kq.repeat(kq.empty(), 0, 100)).toQuery();
        assertEquals(
                "focus(254: spanContain(<field:base/s:t />, {254: spanExpansion(field:try, []{0, 100}, right)}))",
                sq.toString());
    };



    @Test
    public void KorapWithinQuery1 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.contains(kq.seg("test"), kq.seg("test2")).toQuery();
        assertEquals("spanContain(field:test, field:test2)", sq.toString());
    };


    @Test
    public void KorapWithinQuery2 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.overlaps(kq.seg("test"), kq.seg("test2")).toQuery();
        assertEquals("spanOverlap(field:test, field:test2)", sq.toString());
    };


    @Test
    public void KorapWithinQuery3 () throws QueryException {
        QueryBuilder kq = new QueryBuilder("field");
        SpanQuery sq = kq.startswith(kq.seg("test"), kq.seg("test2")).toQuery();
        assertEquals("spanStartsWith(field:test, field:test2)", sq.toString());
    };

    // kq.seg("a").append(kq.ANY).append("b:c");
    // kq.repeat(kq.seg("a", "b"), 5)
};
