package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static de.ids_mannheim.korap.TestSimple.*;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.MetaFields;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.Text;
import de.ids_mannheim.korap.util.QueryException;


/**
 * Tests to verify that primary data stored in index fields
 * ("tokens", "base") is never leaked through metadata APIs.
 *
 * The "tokens" and "base" fields use TextField.TYPE_STORED, so the
 * full primary text is persisted in Lucene alongside term vectors.
 * Without proper filtering, these fields would be serialized into
 * JSON responses, exposing the complete text to API consumers.
 */
@RunWith(JUnit4.class)
public class TestPrimaryDataProtection {

    private static final String PRIMARY_TEXT = "abc def ghi";

    private FieldDocument createDocWithTokensField () {
        FieldDocument fd = new FieldDocument();
        fd.addString("corpusSigle", "TST");
        fd.addString("docSigle", "TST-001");
        fd.addString("textSigle", "TST-001-0001");
        fd.addText("title", "Test Document");
        fd.addText("author", "Test Author");
        fd.setUID(42);
        fd.addTV("tokens", PRIMARY_TEXT,
                "[(0-3)s:abc|i:abc|_0#0-3|-:t$<i>3]"
                + "[(4-7)s:def|i:def|_1#4-7]"
                + "[(8-11)s:ghi|i:ghi|_2#8-11]");
        return fd;
    };


    private FieldDocument createDocWithBaseField () {
        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", "TST-002-0001");
        fd.addText("title", "Base Field Document");
        fd.addString("ID", "doc-base");
        fd.setUID(43);
        fd.addTV("base", PRIMARY_TEXT,
                "[(0-3)s:abc|_0$<i>0<i>3|-:t$<i>3]"
                + "[(4-7)s:def|_1$<i>4<i>7]"
                + "[(8-11)s:ghi|_2$<i>8<i>11]");
        return fd;
    };


    /**
     * Test that getDoc() does not expose the "tokens" field value.
     * getDoc() calls populateFields(doc) which must filter out "tokens".
     */
    @Test
    public void testGetDocDoesNotLeakTokensField () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createDocWithTokensField());
        ki.commit();

        Text text = ki.getDoc("42");
        String json = text.toJsonString();

        assertFalse(
            "JSON response from getDoc must not contain primary data",
            json.contains(PRIMARY_TEXT)
        );
        assertFalse(
            "JSON response from getDoc must not contain 'tokens' as a field key",
            json.contains("\"key\":\"tokens\"")
        );

        assertEquals("Test Document", text.getFieldValue("title"));
        assertEquals("Test Author", text.getFieldValue("author"));
    };


    /**
     * Test that getDoc() does not expose the "base" field value.
     */
    @Test
    public void testGetDocDoesNotLeakBaseField () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createDocWithBaseField());
        ki.commit();

        Text text = ki.getDoc("43");
        String json = text.toJsonString();

        assertFalse(
            "JSON response from getDoc must not contain primary data from 'base' field",
            json.contains(PRIMARY_TEXT)
        );
        assertFalse(
            "JSON response must not contain 'base' as a field key",
            json.contains("\"key\":\"base\"")
        );
    };


    /**
     * Test that getFields() with @all does not leak tokens/base fields.
     */
    @Test
    public void testGetFieldsAllDoesNotLeakPrimaryData () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createDocWithTokensField());
        ki.commit();

        MetaFields mf = ki.getFields("TST-001-0001");
        JsonNode res = mf.toJsonNode();
        String json = res.toString();

        assertFalse(
            "getFields(@all) must not contain primary data",
            json.contains(PRIMARY_TEXT)
        );
        assertFalse(
            "getFields(@all) must not contain 'tokens' field key",
            json.contains("\"key\":\"tokens\"")
        );
        assertFalse(
            "getFields(@all) must not contain 'base' field key",
            json.contains("\"key\":\"base\"")
        );

        Iterator<JsonNode> fieldIter = res.at("/document/fields").elements();
        while (fieldIter.hasNext()) {
            JsonNode field = fieldIter.next();
            String key = field.at("/key").asText();
            assertFalse(
                "No field should be named 'tokens'",
                key.equals("tokens")
            );
            assertFalse(
                "No field should be named 'base'",
                key.equals("base")
            );
        };
    };


    /**
     * Test that getFields() with an explicit field list that includes
     * "tokens" does not return the stored primary data.
     */
    @Test
    public void testGetFieldsExplicitTokensDoesNotLeak () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createDocWithTokensField());
        ki.commit();

        ArrayList<String> fields = new ArrayList<>();
        fields.add("tokens");
        fields.add("title");

        MetaFields mf = ki.getFields("TST-001-0001", fields);
        JsonNode res = mf.toJsonNode();
        String json = res.toString();

        assertFalse(
            "Explicitly requesting 'tokens' field must not leak primary data",
            json.contains(PRIMARY_TEXT)
        );
    };


    /**
     * Test that search results with @all fields do not leak tokens/base.
     */
    @Test
    public void testSearchResultsAllFieldsDoNotLeak () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createDocWithTokensField());
        ki.commit();

        QueryBuilder kq = new QueryBuilder("tokens");
        Krill ks = new Krill(kq.seg("i:abc").toQuery());
        ks.getMeta().addField("@all");
        ks.getMeta().setSnippets(true);

        Result kr = ks.apply(ki);
        assertEquals((long) 1, kr.getTotalResults());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode res = mapper.readTree(kr.toJsonString());
        String resultJson = kr.toJsonString();

        assertFalse(
            "Search results with @all fields must not leak primary data as a field value",
            resultJson.contains("\"key\":\"tokens\"")
        );

        Iterator<JsonNode> matches = res.at("/matches").elements();
        while (matches.hasNext()) {
            JsonNode match = matches.next();
            Iterator<JsonNode> fields = match.at("/fields").elements();
            while (fields.hasNext()) {
                JsonNode field = fields.next();
                String key = field.at("/key").asText();
                assertFalse(
                    "Match field should not be 'tokens'",
                    key.equals("tokens")
                );
                assertFalse(
                    "Match field should not be 'base'",
                    key.equals("base")
                );
            };
        };
    };


    /**
     * Test that search results with default fields do not leak primary data.
     */
    @Test
    public void testSearchResultsDefaultFieldsDoNotLeak () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createDocWithTokensField());
        ki.commit();

        QueryBuilder kq = new QueryBuilder("tokens");
        Krill ks = new Krill(kq.seg("i:abc").toQuery());
        ks.getMeta().setSnippets(true);

        Result kr = ks.apply(ki);
        assertEquals((long) 1, kr.getTotalResults());

        String resultJson = kr.toJsonString();
        assertFalse(
            "Default search results must not contain primary data as a field",
            resultJson.contains("\"key\":\"tokens\"")
        );
        assertFalse(
            "Default search results must not contain primary data as a field",
            resultJson.contains("\"key\":\"base\"")
        );
    };


    /**
     * Test that getFieldVector returns empty for tokens/base.
     */
    @Test
    public void testGetFieldVectorProtectsTokensAndBase () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createDocWithTokensField());
        ki.commit();

        de.ids_mannheim.korap.KrillCollection kc =
            new de.ids_mannheim.korap.KrillCollection(ki);

        List<String> tokenValues = ki.getFieldVector("tokens", kc);
        assertEquals(
            "getFieldVector for 'tokens' must return empty list",
            0, tokenValues.size()
        );

        List<String> baseValues = ki.getFieldVector("base", kc);
        assertEquals(
            "getFieldVector for 'base' must return empty list",
            0, baseValues.size()
        );
    };


    /**
     * Test that getMatchInfo does not expose tokens/base as metadata fields.
     */
    @Test
    public void testMatchInfoDoesNotLeakPrimaryData () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createDocWithTokensField());
        ki.commit();

        QueryBuilder kq = new QueryBuilder("tokens");
        Krill ks = new Krill(kq.seg("i:abc").toQuery());
        ks.getMeta().setSnippets(true);

        Result kr = ks.apply(ki);
        assertEquals((long) 1, kr.getTotalResults());

        Match match = kr.getMatch(0);
        String matchJson = match.toJsonString();

        assertFalse(
            "Match JSON must not contain 'tokens' as a metadata field key",
            matchJson.contains("\"key\":\"tokens\"")
        );
        assertFalse(
            "Match JSON must not contain 'base' as a metadata field key",
            matchJson.contains("\"key\":\"base\"")
        );
    };


    /**
     * Test that populateFields called with a document containing both
     * 'tokens' and 'base' fields does not include either in the result.
     */
    @Test
    public void testPopulateFieldsFiltersBothTokensAndBase () throws IOException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", "TST-003-0001");
        fd.addText("title", "Dual Field Document");
        fd.setUID(44);
        fd.addTV("tokens", "primary text in tokens",
                "[(0-7)s:primary|_0#0-7|-:t$<i>4]"
                + "[(8-12)s:text|_1#8-12]"
                + "[(13-15)s:in|_2#13-15]"
                + "[(16-22)s:tokens|_3#16-22]");
        fd.addTV("base", "primary text in base",
                "[(0-7)s:primary|_0$<i>0<i>7|-:t$<i>4]"
                + "[(8-12)s:text|_1$<i>8<i>12]"
                + "[(13-15)s:in|_2$<i>13<i>15]"
                + "[(16-20)s:base|_3$<i>16<i>20]");
        ki.addDoc(fd);
        ki.commit();

        Text text = ki.getDoc("44");
        String json = text.toJsonString();

        assertFalse(
            "Must not contain primary text from 'tokens' field",
            json.contains("primary text in tokens")
        );
        assertFalse(
            "Must not contain primary text from 'base' field",
            json.contains("primary text in base")
        );
        assertFalse(
            "Must not contain field key 'tokens'",
            json.contains("\"key\":\"tokens\"")
        );
        assertFalse(
            "Must not contain field key 'base'",
            json.contains("\"key\":\"base\"")
        );

        assertEquals("Dual Field Document", text.getFieldValue("title"));
    };


    /**
     * Test that a custom TV field (not named "tokens" or "base") with
     * stored primary data WILL appear in the output. This documents
     * the current behavior: only "tokens" and "base" are protected.
     *
     * If additional TV field names are used in the future to store
     * primary data, they must be added to the filter list.
     */
    @Test
    public void testCustomTvFieldIsNotFiltered () throws IOException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", "TST-004-0001");
        fd.addText("title", "Custom Field Document");
        fd.setUID(45);
        fd.addTV("customTokens", "leaked custom text",
                "[(0-6)s:leaked|_0#0-6|-:t$<i>3]"
                + "[(7-13)s:custom|_1#7-13]"
                + "[(14-18)s:text|_2#14-18]");
        ki.addDoc(fd);
        ki.commit();

        Text text = ki.getDoc("45");
        String json = text.toJsonString();

        assertTrue(
            "Custom TV field IS exposed (not filtered) - this is documented behavior. "
            + "Only 'tokens' and 'base' are filtered.",
            json.contains("leaked custom text")
                || json.contains("\"key\":\"customTokens\"")
        );
    };
};
