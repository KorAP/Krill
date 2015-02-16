package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.getJSONQuery;
import static org.junit.Assert.assertEquals;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

public class TestSpanWithAttributeJSON {

	@Test
	public void testElementSingleAttribute() throws QueryException {
		String filepath = getClass().getResource(
				"/queries/attribute/element-single-attribute.jsonld").getFile();
		SpanQueryWrapper sqwi = getJSONQuery(filepath);
		SpanQuery sq = sqwi.toQuery();
		assertEquals(
				"spanElementWithAttribute(<tokens:head />, spanAttribute(tokens:type:top))",
				sq.toString());
	}

	@Test
	public void testElementSingleNotAttribute() throws QueryException {
		String filepath = getClass().getResource(
				"/queries/attribute/element-single-not-attribute.jsonld")
				.getFile();
		SpanQueryWrapper sqwi = getJSONQuery(filepath);
		SpanQuery sq = sqwi.toQuery();
		assertEquals(
				"spanElementWithAttribute(<tokens:head />, spanAttribute(!tokens:type:top))",
				sq.toString());
	}

	@Test
	public void testElementMultipleAndNotAttributes() throws QueryException {
		String filepath = getClass().getResource(
						"/queries/attribute/element-multiple-and-not-attributes.jsonld")
				.getFile();
		SpanQueryWrapper sqwi = getJSONQuery(filepath);
		SpanQuery sq = sqwi.toQuery();
		assertEquals(
				"spanElementWithAttribute(<tokens:div />, [spanAttribute(tokens:type:Zeitschrift), "
						+ "spanAttribute(!tokens:complete:Y), spanAttribute(tokens:n:0)])",
				sq.toString());
	}

	@Test
	public void testElementMultipleOrAttributes() throws QueryException {
		String filepath = getClass().getResource(
				"/queries/attribute/element-multiple-or-attributes.jsonld")
				.getFile();
		SpanQueryWrapper sqwi = getJSONQuery(filepath);
		SpanQuery sq = sqwi.toQuery();
		assertEquals(
				"spanOr([spanElementWithAttribute(<tokens:div />, spanAttribute(tokens:type:Zeitschrift)), "
						+ "spanElementWithAttribute(<tokens:div />, spanAttribute(tokens:complete:Y)), "
						+ "spanElementWithAttribute(<tokens:div />, spanAttribute(tokens:n:0))])",
				sq.toString());
	}

	@Test
	public void testAnyElementWithAttribute() throws QueryException {
		String filepath = getClass().getResource(
				"/queries/attribute/any-element-with-attribute.jsonld")
				.getFile();
		SpanQueryWrapper sqwi = getJSONQuery(filepath);
		// SpanQuery sq = sqwi.toQuery();
		assertEquals(null, sqwi);
	}

}
