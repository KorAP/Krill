package de.ids_mannheim.korap.golden;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;

/**
 * Golden-result regression test.
 *
 * Runs a fixed set of queries against a fixed in-memory index of
 * the Wikipedia samples in src/test/resources/wiki and compares
 * the outcome with a stored snapshot
 * (src/test/resources/golden/golden-results.json):
 * <ul>
 * <li>total results and total resources per query,</li>
 * <li>a digest over the ids and snippets of <i>all</i> matches, in
 * the order they are returned (so a changed match order or a
 * changed payload/highlight is detected),</li>
 * <li>id and snippet of the first matches in clear text,</li>
 * <li>match info snippets (annotations and spans) of the first
 * matches,</li>
 * <li>numberOf() for the whole index and for virtual corpora.</li>
 * </ul>
 *
 * The test is meant as a safety net for changes that must not alter
 * search results, such as a Lucene upgrade. It deliberately uses
 * the Krill API only and no Lucene classes, so it compiles
 * unchanged against any Lucene version.
 *
 * To (re)create the snapshot, run
 *
 * <pre>
 * mvn test -Dtest=TestGoldenResults -Dkrill.golden.update=true
 * </pre>
 *
 * and commit src/test/resources/golden/golden-results.json.
 * Only do this on a state of the code that is known to be correct.
 *
 * On a mismatch the actual results are written to
 * target/golden-results.actual.json to be diffed with the snapshot.
 */
@RunWith(JUnit4.class)
public class TestGoldenResults {

    private static final String FIELD = "tokens";

    private static final String RESOURCE = "/golden/golden-results.json";

    private static final Path SNAPSHOT = Paths
            .get("src/test/resources/golden/golden-results.json");

    private static final Path ACTUAL = Paths
            .get("target/golden-results.actual.json");

    private static final String UPDATE_PROPERTY = "krill.golden.update";

    // The same documents the benchmark uses, added in this order
    // and committed once, so the segment layout is fixed
    private static final String[] SAMPLE_DOCS = { "00001", "00002", "00003",
            "00004", "00005", "00006", "02439", "WPD17-H81-63495",
            "WUD17-C94-39360", "wpd15-u43-34816" };

    // Matches per page (the maximum Krill allows)
    private static final int PAGE = 50;

    // Upper limit of matches that go into the digests per query
    private static final int MAX_MATCHES = 5000;

    // Matches per query stored in clear text
    private static final int CLEAR_MATCHES = 10;

    // Matches per query to get match infos for
    private static final int INFO_MATCHES = 3;

    private static final String KORAL_CONTEXT = "\"@context\":\"http://korap.ids-mannheim.de/ns/koral/0.3/context.jsonld\"";

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static KrillIndex ki;

    /** A named query, built anew for every search. */
    private record GoldenQuery (String name, Supplier<Krill> supplier) {};


    @BeforeClass
    public static void buildIndex () throws IOException {
        ki = new KrillIndex();
        for (String doc : SAMPLE_DOCS) {
            String name = "/wiki/" + doc + ".json.gz";
            InputStream is = TestGoldenResults.class
                    .getResourceAsStream(name);
            assertNotNull(name + " not found on the classpath", is);
            ki.addDoc(is, true);
        };
        ki.commit();
    };


    @AfterClass
    public static void closeIndex () throws IOException {
        if (ki != null)
            ki.close();
    };


    @Test
    public void resultsMatchSnapshot () throws Exception {
        // Normalize number types by a round trip through JSON
        String actualJson = mapper.writeValueAsString(collect());
        JsonNode actual = mapper.readTree(actualJson);

        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            Files.createDirectories(SNAPSHOT.toAbsolutePath().getParent());
            Files.write(SNAPSHOT, actualJson.getBytes(StandardCharsets.UTF_8));
            System.out.println("Golden results written to "
                    + SNAPSHOT.toAbsolutePath());
            printSummary(actual);
            return;
        };

        JsonNode expected;
        try (InputStream is = getClass().getResourceAsStream(RESOURCE)) {
            if (is == null) {
                fail("No golden results found at " + RESOURCE
                        + " - create them on a known good state with:"
                        + " mvn test -Dtest=TestGoldenResults -D"
                        + UPDATE_PROPERTY + "=true");
                return;
            };
            expected = mapper.readTree(is);
        };

        List<String> diffs = new ArrayList<>();
        diff("", expected, actual, diffs);

        if (!diffs.isEmpty()) {
            Files.createDirectories(ACTUAL.toAbsolutePath().getParent());
            Files.write(ACTUAL, actualJson.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            sb.append(diffs.size()).append(
                    " difference(s) to the golden results (actual results in ")
                    .append(ACTUAL).append("):");
            for (int i = 0; i < Math.min(diffs.size(), 25); i++)
                sb.append("\n  ").append(diffs.get(i));
            if (diffs.size() > 25)
                sb.append("\n  ...");
            fail(sb.toString());
        };
    };


    // ------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------

    private static GoldenQuery builder (String name,
            Supplier<SpanQueryWrapper> q) {
        return new GoldenQuery(name, () -> new Krill(q.get()));
    };


    private static GoldenQuery koral (String name, String query) {
        final String json = "{" + KORAL_CONTEXT + ",\"query\":" + query
                + "}";
        return new GoldenQuery(name, () -> new Krill(json));
    };


    // A query restricted to a virtual corpus
    private static GoldenQuery inCorpus (String name,
            Supplier<SpanQueryWrapper> q,
            java.util.function.Function<CollectionBuilder, CollectionBuilder.Interface> vc) {
        return new GoldenQuery(name, () -> {
            Krill ks = new Krill(q.get());
            KrillCollection kc = new KrillCollection(ki);
            kc.fromBuilder(vc.apply(kc.build()));
            ks.setCollection(kc);
            return ks;
        });
    };


    private static String token (String foundry, String layer, String key) {
        return "{\"@type\":\"koral:token\",\"wrap\":{\"@type\":\"koral:term\","
                + "\"foundry\":\"" + foundry + "\",\"layer\":\"" + layer
                + "\",\"key\":\"" + key + "\",\"match\":\"match:eq\"}}";
    };


    private static String distance (String key, int min, int max,
            boolean inOrder, String first, String second) {
        return "{\"@type\":\"koral:group\",\"operation\":\"operation:sequence\","
                + "\"inOrder\":" + inOrder + ",\"distances\":["
                + "{\"@type\":\"koral:distance\",\"key\":\"" + key
                + "\",\"boundary\":{\"@type\":\"koral:boundary\",\"min\":"
                + min + ",\"max\":" + max + "}}],\"operands\":[" + first
                + "," + second + "]}";
    };


    private static List<GoldenQuery> queries () {
        List<GoldenQuery> qs = new ArrayList<>();

        // --- Terms and segments
        qs.add(builder("term [s:der]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg("s:der");
        }));

        qs.add(builder("annotation [mate/m:gender:masc]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg("mate/m:gender:masc");
        }));

        qs.add(builder("segment [tt/p:NN & mate/m:number:pl]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg("tt/p:NN").with("mate/m:number:pl");
        }));

        qs.add(builder("alternation [s:der | s:die]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg(qb.or("s:der", "s:die"));
        }));

        // --- Multi term queries
        qs.add(builder("regex [s:.*ung]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg(qb.re("s:.*ung"));
        }));

        qs.add(builder("regex case insensitive [i:sprach.*]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg(qb.re("s:sprach.*", true));
        }));

        // --- Sequences, repetitions, classes
        qs.add(builder("sequence [tt/p:ART][tt/p:ADJA][tt/p:NN]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seq(qb.seg("tt/p:ART"), qb.seg("tt/p:ADJA"),
                    qb.seg("tt/p:NN"));
        }));

        qs.add(builder("repetition [tt/p:ADJA]{2,3}[tt/p:NN]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seq(qb.repeat(qb.seg("tt/p:ADJA"), 2, 3),
                    qb.seg("tt/p:NN"));
        }));

        qs.add(builder("optionality [tt/p:ART][tt/p:ADJA]?[tt/p:NN]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seq(qb.seg("tt/p:ART"), qb.opt(qb.seg("tt/p:ADJA")),
                    qb.seg("tt/p:NN"));
        }));

        qs.add(builder("class {1:[tt/p:ADJA]}[s:Sprache]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seq(qb.nr(1, qb.seg("tt/p:ADJA")), qb.seg("s:Sprache"));
        }));

        qs.add(builder("focus(1: {1:[tt/p:ADJA]}[tt/p:NN])", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.focus(1,
                    qb.seq(qb.nr(1, qb.seg("tt/p:ADJA")), qb.seg("tt/p:NN")));
        }));

        // --- Elements and positions
        qs.add(builder("element <base/s:s>", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.tag("base/s:s");
        }));

        qs.add(builder("contains(<base/s:s>, [tt/p:NE][tt/p:NE])", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.contains(qb.tag("base/s:s"),
                    qb.seq(qb.seg("tt/p:NE"), qb.seg("tt/p:NE")));
        }));

        qs.add(builder("startswith(<base/s:s>, [tt/p:ART])", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.startswith(qb.tag("base/s:s"), qb.seg("tt/p:ART"));
        }));

        qs.add(builder("endswith(<base/s:s>, [tt/p:NN])", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.endswith(qb.tag("base/s:s"), qb.seg("tt/p:NN"));
        }));

        qs.add(builder("matches(<base/s:s>, [tt/p:NN]{1,3})", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.matches(qb.tag("base/s:s"),
                    qb.repeat(qb.seg("tt/p:NN"), 1, 3));
        }));

        // --- Distances and expansion (deserialized from KoralQuery)
        qs.add(koral("token distance [tt/p:ART] /+w1:3 [tt/p:NN]",
                distance("w", 1, 3, true, token("tt", "p", "ART"),
                        token("tt", "p", "NN"))));

        qs.add(koral("unordered token distance [tt/p:NE] /w1:2 [tt/p:VVFIN]",
                distance("w", 1, 2, false, token("tt", "p", "NE"),
                        token("tt", "p", "VVFIN"))));

        qs.add(koral("sentence distance [tt/p:NE] /+s1:1 [tt/p:NE]",
                distance("s", 1, 1, true, token("tt", "p", "NE"),
                        token("tt", "p", "NE"))));

        qs.add(koral("expansion [tt/p:ART][][tt/p:NN]",
                "{\"@type\":\"koral:group\",\"operation\":\"operation:sequence\","
                        + "\"operands\":[" + token("tt", "p", "ART")
                        + ",{\"@type\":\"koral:token\"},"
                        + token("tt", "p", "NN") + "]}"));

        // --- Virtual corpora
        qs.add(inCorpus("[s:der] in textClass=reisen", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg("s:der");
        }, cb -> cb.term("textClass", "reisen")));

        qs.add(inCorpus("[s:der] in pubDate since 2010", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg("s:der");
        }, cb -> cb.since("pubDate", "2010-01-01")));

        qs.add(inCorpus("[s:der] in textClass=reisen & author=/.*an.*/", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg("s:der");
        }, cb -> cb.andGroup().with(cb.term("textClass", "reisen"))
                .with(cb.re("author", ".*an.*"))));

        qs.add(inCorpus("[s:der] in textClass=reisen | textClass=wissenschaft",
                () -> {
                    QueryBuilder qb = new QueryBuilder(FIELD);
                    return qb.seg("s:der");
                }, cb -> cb.orGroup().with(cb.term("textClass", "reisen"))
                        .with(cb.term("textClass", "wissenschaft"))));

        return qs;
    };


    // ------------------------------------------------------------
    // Collecting the results
    // ------------------------------------------------------------

    private ObjectNode collect () throws Exception {
        ObjectNode root = mapper.createObjectNode();

        // Sizes of the whole index
        ObjectNode index = root.putObject("index");
        for (String type : new String[] { "documents", "tokens", "sentences",
                "paragraphs" })
            index.put(type, ki.numberOf(type));

        // Sizes of virtual corpora
        ObjectNode corpora = root.putObject("corpora");
        corpora.set("textClass=reisen",
                corpusSizes(cb -> cb.term("textClass", "reisen")));
        corpora.set("pubDate since 2010",
                corpusSizes(cb -> cb.since("pubDate", "2010-01-01")));
        corpora.set("pubDate till 2009",
                corpusSizes(cb -> cb.till("pubDate", "2009-12-31")));
        corpora.set("author=/.*an.*/",
                corpusSizes(cb -> cb.re("author", ".*an.*")));

        ArrayNode queries = root.putArray("queries");
        for (GoldenQuery q : queries())
            queries.add(run(q));

        return root;
    };


    private ObjectNode corpusSizes (
            java.util.function.Function<CollectionBuilder, CollectionBuilder.Interface> vc)
            throws IOException {
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(vc.apply(kc.build()));
        ObjectNode node = mapper.createObjectNode();
        for (String type : new String[] { "documents", "tokens", "sentences",
                "paragraphs" })
            node.put(type, kc.numberOf(type));
        return node;
    };


    private ObjectNode run (GoldenQuery q) throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("name", q.name());

        MessageDigest ids = MessageDigest.getInstance("SHA-256");
        MessageDigest snippets = MessageDigest.getInstance("SHA-256");
        ArrayNode first = mapper.createArrayNode();
        List<String> infoIds = new ArrayList<>();

        int seen = 0;
        while (seen < MAX_MATCHES) {
            Krill ks = q.supplier().get();
            ks.getMeta().setCount(PAGE);
            ks.getMeta().setStartIndex(seen);
            Result kr = ks.apply(ki);

            if (kr.hasErrors()) {
                node.put("error", kr.getErrors().toJsonString());
                return node;
            };

            // The totals are those of the first page - they have to
            // be the same on every page
            if (seen == 0) {
                node.put("totalResults", kr.getTotalResults());
                node.put("totalResources", kr.getTotalResources());
            }
            else if (node.get("totalResults").asLong() != kr
                    .getTotalResults()) {
                node.put("inconsistentTotalResults", kr.getTotalResults());
            };

            List<Match> matches = kr.getMatches();
            for (Match m : matches) {
                String id = String.valueOf(m.getID());
                String snippet = String.valueOf(m.getSnippetBrackets());

                ids.update((id + "\n").getBytes(StandardCharsets.UTF_8));
                snippets.update((id + "\t" + snippet + "\n")
                        .getBytes(StandardCharsets.UTF_8));

                if (first.size() < CLEAR_MATCHES) {
                    ObjectNode mn = first.addObject();
                    mn.put("id", id);
                    mn.put("snippet", snippet);
                };
                if (infoIds.size() < INFO_MATCHES)
                    infoIds.add(id);
            };

            seen += matches.size();
            if (matches.size() < PAGE)
                break;
        };

        node.put("matches", seen);
        node.put("idDigest", HexFormat.of().formatHex(ids.digest()));
        node.put("snippetDigest",
                HexFormat.of().formatHex(snippets.digest()));
        node.set("first", first);

        // Match infos: token annotations and spans
        ArrayNode infos = node.putArray("matchInfo");
        for (String id : infoIds) {
            ObjectNode in = infos.addObject();
            in.put("id", id);
            in.put("tt/p", matchInfo(id, "tt", "p", false));
            in.put("base/s with spans", matchInfo(id, "base", "s", true));
        };

        return node;
    };


    private String matchInfo (String id, String foundry, String layer,
            boolean spans) {
        try {
            Match m = ki.getMatchInfo(id, FIELD, foundry, layer, spans, true);
            return String.valueOf(m.getSnippetHTML());
        }
        catch (Exception e) {
            return "EXCEPTION " + e.getClass().getSimpleName() + ": "
                    + e.getMessage();
        }
    };


    // ------------------------------------------------------------
    // Reporting
    // ------------------------------------------------------------

    // Collect the paths at which two JSON trees differ
    private static void diff (String path, JsonNode expected, JsonNode actual,
            List<String> diffs) {
        if (expected == null || actual == null) {
            diffs.add(path + ": "
                    + (expected == null ? "unexpected" : "missing"));
        }
        else if (expected.isObject() && actual.isObject()) {
            List<String> names = new ArrayList<>();
            for (Iterator<String> it = expected.fieldNames(); it.hasNext();)
                names.add(it.next());
            for (Iterator<String> it = actual.fieldNames(); it.hasNext();) {
                String name = it.next();
                if (!names.contains(name))
                    names.add(name);
            };
            for (String name : names)
                diff(path + "/" + label(name, expected.get(name)),
                        expected.get(name), actual.get(name), diffs);
        }
        else if (expected.isArray() && actual.isArray()) {
            int size = Math.max(expected.size(), actual.size());
            for (int i = 0; i < size; i++)
                diff(path + "[" + label(String.valueOf(i), expected.get(i))
                        + "]", expected.get(i), actual.get(i), diffs);
        }
        else if (!expected.equals(actual)) {
            diffs.add(path + ": expected " + shorten(expected) + " but was "
                    + shorten(actual));
        };
    };


    // Name queries in paths, so a difference is easy to locate
    private static String label (String key, JsonNode node) {
        if (node != null && node.isObject() && node.has("name"))
            return key + " \"" + node.get("name").asText() + "\"";
        return key;
    };


    private static String shorten (JsonNode node) {
        String s = node.toString();
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    };


    private static void printSummary (JsonNode results) {
        System.out.println("  index: " + results.get("index"));
        for (JsonNode q : results.get("queries")) {
            String status;
            if (q.has("error"))
                status = "ERROR " + q.get("error").asText();
            else {
                status = q.get("totalResults").asLong() + " results in "
                        + q.get("totalResources").asLong() + " texts";
                if (q.get("totalResults").asLong() == 0)
                    status += "  <-- NO RESULTS, the query tests nothing";
            };
            System.out.println("  " + q.get("name").asText() + ": " + status);
        };
    };
};
