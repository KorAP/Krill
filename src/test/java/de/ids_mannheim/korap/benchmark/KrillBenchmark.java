package de.ids_mannheim.korap.benchmark;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.util.BytesRef;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillMeta;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.MetaFields;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

/**
 * Benchmark for {@link KrillIndex}.
 *
 * Measures
 * <ul>
 * <li>the response time of {@link KrillIndex#search(Krill)}
 * (called through {@link Krill#apply(KrillIndex)}),</li>
 * <li>the total number of results (and texts) the search
 * retrieves,</li>
 * <li>the response time of {@link KrillIndex#getMatchInfo} for
 * matches found by these searches,</li>
 * <li>the response time of {@link KrillIndex#numberOf(String)},
 * and</li>
 * <li>the response time of {@link KrillIndex#getFields(String)} and
 * {@link KrillIndex#getFields(String, List)} for texts of the
 * index.</li>
 * </ul>
 *
 * The report is printed and written to an output file
 * (<tt>--output</tt>, by default
 * <tt>target/krill-benchmark-&lt;timestamp&gt;.txt</tt>).
 *
 * KrillIndex keeps a per-instance cache of match counts per index
 * segment, so the first run of a query is much slower than any
 * repetition. Both are therefore reported separately:
 * <ul>
 * <li><b>cold</b>: every round uses a fresh KrillIndex (empty count
 * cache, new index reader). The operating system's file cache is
 * <i>not</i> dropped.</li>
 * <li><b>warm</b>: all rounds share one KrillIndex.</li>
 * </ul>
 *
 * Run from the project root (see also misc/benchmark.sh):
 *
 * <pre>
 * mvn -q test-compile exec:java \
 *   -Dexec.classpathScope=test \
 *   -Dexec.mainClass=de.ids_mannheim.korap.benchmark.KrillBenchmark \
 *   -Dexec.args="--index /path/to/index --queries src/test/resources/queries/benchmark1.jsonld"
 * </pre>
 *
 * Without <tt>--index</tt> a small in-memory index is built from
 * the Wikipedia samples in src/test/resources/wiki. Without
 * <tt>--queries</tt> a built-in query set is used that targets the
 * annotations of these samples (s, tt/p, mate/m, base/s).
 */
public class KrillBenchmark {

    // Sample documents for the in-memory index
    // (the last three have a textSigle, as needed for getFields)
    private static final String[] SAMPLE_DOCS = { "00001", "00002", "00003",
            "00004", "00005", "00006", "02439", "WPD17-H81-63495",
            "WUD17-C94-39360", "wpd15-u43-34816" };

    private static final String FIELD = "tokens";

    // Options
    private String indexPath = null;
    private final List<String> queryPaths = new ArrayList<>();
    private int rounds = 20;
    private int coldRounds = 3;
    private int warmup = 5;
    private Integer count = null;
    private Long timeout = null;
    private boolean snippets = true;
    private int matchesPerQuery = 5;
    private String foundry = null;
    private String layer = null;
    private String csvPath = null;
    private String outputPath = null;
    private List<String> numberOfTypes = List.of("documents");
    private int texts = 10;
    private List<String> metaFields = List.of("textSigle", "title",
            "author");

    // Everything printed to the report
    private final StringBuilder report = new StringBuilder();

    // The directory all KrillIndex instances are opened on
    private Directory directory;


    /** A named query, built anew for every round. */
    private record NamedQuery (String name, Supplier<Krill> supplier) {};

    /** A variant of {@link KrillIndex#getMatchInfo} to measure. */
    private interface MatchInfoCall {
        Match get (KrillIndex ki, String id) throws QueryException;
    };

    /** Collected response times in nanoseconds. */
    private static final class Times {
        private long[] values = new long[16];
        private int size = 0;


        void add (long nanos) {
            if (size == values.length)
                values = Arrays.copyOf(values, size * 2);
            values[size++] = nanos;
        };


        int size () {
            return size;
        };


        private long[] sorted () {
            long[] s = Arrays.copyOf(values, size);
            Arrays.sort(s);
            return s;
        };


        // Nearest-rank percentile in milliseconds
        double percentile (double p) {
            if (size == 0)
                return Double.NaN;
            long[] s = sorted();
            int rank = (int) Math.ceil(p / 100.0 * size);
            return s[Math.max(0, Math.min(size - 1, rank - 1))] / 1e6;
        };


        double min () {
            return size == 0 ? Double.NaN : sorted()[0] / 1e6;
        };


        double max () {
            return size == 0 ? Double.NaN : sorted()[size - 1] / 1e6;
        };


        double median () {
            if (size == 0)
                return Double.NaN;
            long[] s = sorted();
            return (size % 2 == 1) ? s[size / 2] / 1e6
                    : (s[size / 2 - 1] + s[size / 2]) / 2e6;
        };


        double mean () {
            if (size == 0)
                return Double.NaN;
            double sum = 0;
            for (int i = 0; i < size; i++)
                sum += values[i];
            return sum / size / 1e6;
        };
    };

    /** One row of the report. */
    private static final class Row {
        String type; // "search", "matchinfo", "numberOf" or "fields"
        String name;
        long totalResults = -1;
        long totalResources = -1;
        int matches = 0; // number of match ids (matchinfo only)
        int errors = 0;
        String note = "";
        Times cold = new Times(); // search only
        Times call = new Times(); // the KrillIndex call
        Times response = new Times(); // the call + JSON serialization
    };


    public static void main (String[] argv) throws Exception {
        KrillBenchmark bm = new KrillBenchmark();
        if (!bm.parseArgs(argv))
            return;
        bm.run();
    };


    private void run () throws Exception {
        this.directory = (indexPath != null) ? openDiskIndex()
                : buildSampleIndex();

        List<NamedQuery> queries = queryPaths.isEmpty() ? builtinQueries()
                : loadQueries();
        if (queries.isEmpty()) {
            System.err.println("No queries to run.");
            return;
        };

        // The index shared by all warm rounds
        KrillIndex ki = new KrillIndex(directory);
        if (ki.reader() == null) {
            System.err.println("Unable to open index " + indexPath);
            return;
        };

        println("Krill benchmark - " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        println("  index:     "
                + (indexPath != null ? indexPath : "in-memory wiki sample")
                + " (" + ki.reader().numDocs() + " docs, "
                + ki.reader().leaves().size() + " segments)");
        println("  queries:   " + queries.size()
                + (queryPaths.isEmpty() ? " (built-in)" : ""));
        println("  rounds:    " + coldRounds + " cold, " + warmup
                + " warm-up, " + rounds + " warm");
        println("  snippets:  " + snippets);
        println("  java:      " + System.getProperty("java.version")
                + ", max heap "
                + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
        println();

        // Let the JIT see every query once before anything is measured
        // (on a throw-away index, so the shared count cache stays empty)
        KrillIndex warmupIndex = new KrillIndex(directory);
        for (NamedQuery q : queries) {
            for (int i = 0; i < Math.min(warmup, 2); i++)
                search(q, warmupIndex).toJsonString();
        };
        warmupIndex.closeReader();

        List<Row> searchRows = new ArrayList<>();
        // Match ids per query name
        Map<String, List<String>> matchIds = new LinkedHashMap<>();

        for (NamedQuery q : queries) {
            System.err.println("search: " + q.name());
            searchRows.add(benchSearch(q, ki, matchIds));
        };
        printSearch(searchRows);

        List<Row> infoRows = new ArrayList<>();
        if (matchesPerQuery > 0) {
            int total = 0;
            for (List<String> ids : matchIds.values())
                total += ids.size();

            if (total == 0) {
                println(
                        "No matches found - match info benchmark skipped.");
            }
            else {
                infoRows = benchMatchInfo(ki, matchIds);
                printMatchInfo(infoRows, total);
            };
        };

        Set<String> textSigles = textSigles(ki);
        List<Row> metaRows = benchNumberOf(ki);
        metaRows.addAll(benchFields(ki, textSigles));
        printMeta(metaRows, textSigles.size());

        if (csvPath != null) {
            infoRows.addAll(metaRows);
            writeCsv(searchRows, infoRows);
            println("CSV written to " + csvPath);
        };

        writeReport();

        ki.closeReader();
    };


    // ------------------------------------------------------------
    // Search
    // ------------------------------------------------------------

    // Build the query and apply the command line overrides
    private Krill prepare (NamedQuery q) {
        Krill ks = q.supplier().get();
        KrillMeta meta = ks.getMeta();
        if (count != null)
            meta.setCount(count);
        if (timeout != null)
            meta.setTimeOut(timeout);
        if (!snippets)
            meta.setSnippets(false);
        return ks;
    };


    private Result search (NamedQuery q, KrillIndex ki) {
        return prepare(q).apply(ki);
    };


    private Row benchSearch (NamedQuery q, KrillIndex ki,
            Map<String, List<String>> matchIds) throws IOException {
        Row row = new Row();
        row.type = "search";
        row.name = q.name();

        Set<String> notes = new LinkedHashSet<>();

        // Cold: fresh KrillIndex per round
        for (int i = 0; i < coldRounds; i++) {
            KrillIndex fresh = new KrillIndex(directory);
            fresh.reader(); // Opening the reader is not part of the search
            if (!measureSearch(q, fresh, row, row.cold, null, notes)) {
                fresh.closeReader();
                return row;
            };
            fresh.closeReader();
        };

        // Warm: shared KrillIndex
        for (int i = 0; i < warmup; i++)
            search(q, ki).toJsonString();

        for (int i = 0; i < rounds; i++) {
            if (!measureSearch(q, ki, row, row.call, row.response, notes))
                return row;
        };

        // Remember some matches for the match info benchmark
        if (matchesPerQuery > 0) {
            List<String> ids = new ArrayList<>();
            for (Match m : search(q, ki).getMatches()) {
                if (ids.size() >= matchesPerQuery)
                    break;
                ids.add(m.getID());
            };
            matchIds.put(q.name(), ids);
        };

        row.note = String.join("; ", notes);
        return row;
    };


    // Run and time a single search. Returns false on query errors.
    private boolean measureSearch (NamedQuery q, KrillIndex ki, Row row,
            Times call, Times response, Set<String> notes) {
        Krill ks = prepare(q);

        long t0 = System.nanoTime();
        Result kr = ks.apply(ki);
        long t1 = System.nanoTime();
        String json = kr.toJsonString();
        long t2 = System.nanoTime();

        if (kr.hasErrors()) {
            row.errors++;
            row.note = "ERROR " + kr.getErrors().toJsonString();
            return false;
        };

        // Keep the serialization from being optimized away
        if (json.isEmpty())
            notes.add("empty response");

        call.add(t1 - t0);
        if (response != null)
            response.add(t2 - t0);

        if (kr.hasTimeExceeded())
            notes.add("timeout (totals are incomplete)");

        // The total has to be the same in every round, cached or not
        if (row.totalResults == -1) {
            row.totalResults = kr.getTotalResults();
            row.totalResources = kr.getTotalResources();
        }
        else if (row.totalResults != kr.getTotalResults()
                || row.totalResources != kr.getTotalResources()) {
            notes.add("INCONSISTENT totals (also saw "
                    + kr.getTotalResults() + "/" + kr.getTotalResources()
                    + ")");
        };
        return true;
    };


    // ------------------------------------------------------------
    // Match info
    // ------------------------------------------------------------

    private Map<String, MatchInfoCall> matchInfoCalls () {
        final String f = this.foundry;
        final String l = this.layer;
        final String scope = (f == null && l == null) ? "all annotations"
                : ((f == null ? "*" : f) + "/" + (l == null ? "*" : l));

        Map<String, MatchInfoCall> calls = new LinkedHashMap<>();

        // What Kustvakt requests for a plain match: KWIC only
        calls.put("snippet only", (ki, id) -> ki.getMatch(id));

        // Token annotations
        calls.put("tokens: " + scope, (ki, id) -> ki.getMatchInfo(id, FIELD,
                true, f, l, false, true, false));

        // Token and span annotations
        calls.put("tokens+spans: " + scope, (ki, id) -> ki.getMatchInfo(id,
                FIELD, true, f, l, true, true, false));

        // Token and span annotations, match extended to the sentence
        calls.put("tokens+spans, sentence: " + scope,
                (ki, id) -> ki.getMatchInfo(id, FIELD, true, f, l, true, true,
                        true));
        return calls;
    };


    private List<Row> benchMatchInfo (KrillIndex ki,
            Map<String, List<String>> matchIds) {
        List<Row> rows = new ArrayList<>();

        for (Map.Entry<String, MatchInfoCall> call : matchInfoCalls()
                .entrySet()) {
            System.err.println("match info: " + call.getKey());

            // Aggregated over all queries
            Row all = new Row();
            all.type = "matchinfo";
            all.name = call.getKey();
            rows.add(all);

            for (Map.Entry<String, List<String>> e : matchIds.entrySet()) {
                if (e.getValue().isEmpty())
                    continue;

                // Per query the matches stem from (CSV only)
                Row row = new Row();
                row.type = "matchinfo";
                row.name = call.getKey();
                row.note = e.getKey();
                row.matches = e.getValue().size();
                all.matches += row.matches;
                rows.add(row);

                for (String id : e.getValue()) {
                    for (int i = 0; i < warmup; i++)
                        measureMatchInfo(call.getValue(), ki, id, null, null);

                    for (int i = 0; i < rounds; i++) {
                        if (!measureMatchInfo(call.getValue(), ki, id, row,
                                all)) {
                            row.errors++;
                            all.errors++;
                            break;
                        };
                    };
                };
            };
        };
        return rows;
    };


    // Run and time a single match info request.
    // Returns false if the match could not be retrieved.
    private boolean measureMatchInfo (MatchInfoCall call, KrillIndex ki,
            String id, Row row, Row all) {
        try {
            long t0 = System.nanoTime();
            Match km = call.get(ki, id);
            long t1 = System.nanoTime();
            String json = km.toJsonString();
            long t2 = System.nanoTime();

            if (km.hasErrors() || json.isEmpty())
                return false;

            if (row != null) {
                row.call.add(t1 - t0);
                row.response.add(t2 - t0);
                all.call.add(t1 - t0);
                all.response.add(t2 - t0);
            };
            return true;
        }
        catch (QueryException e) {
            return false;
        }
    };


    // ------------------------------------------------------------
    // Statistics and metadata fields
    // ------------------------------------------------------------

    // KrillIndex.numberOf(type), e.g. numberOf("documents")
    private List<Row> benchNumberOf (KrillIndex ki) throws IOException {
        List<Row> rows = new ArrayList<>();

        for (String type : numberOfTypes) {
            System.err.println("numberOf: " + type);
            Row row = new Row();
            row.type = "numberOf";
            row.name = "numberOf(\"" + type + "\")";
            rows.add(row);

            // Cold: fresh KrillIndex per round
            for (int i = 0; i < coldRounds; i++) {
                KrillIndex fresh = new KrillIndex(directory);
                fresh.reader();
                long t0 = System.nanoTime();
                long n = fresh.numberOf(type);
                row.cold.add(System.nanoTime() - t0);
                fresh.closeReader();
                if (n < 0)
                    row.errors++;
            };

            // Warm: shared KrillIndex
            for (int i = 0; i < warmup; i++)
                ki.numberOf(type);

            for (int i = 0; i < rounds; i++) {
                long t0 = System.nanoTime();
                long n = ki.numberOf(type);
                long t1 = System.nanoTime();

                // -1 means the index could not be read
                if (n < 0) {
                    row.errors++;
                    continue;
                };
                row.call.add(t1 - t0);

                if (row.totalResults == -1)
                    row.totalResults = n;
                else if (row.totalResults != n)
                    row.note = "INCONSISTENT values (also saw " + n + ")";
            };
        };
        return rows;
    };


    // Text sigles to request metadata fields for, evenly spread
    // over the textSigle terms of the index
    private Set<String> textSigles (KrillIndex ki) throws IOException {
        Set<String> sigles = new LinkedHashSet<>();
        if (texts == 0)
            return sigles;

        Terms terms = MultiTerms.getTerms(ki.reader(), "textSigle");
        if (terms == null)
            return sigles;

        // The number of terms is unknown for indices with
        // multiple segments - the document count is close enough
        long size = terms.size();
        if (size < 0)
            size = ki.reader().numDocs();
        long step = Math.max(1, size / texts);

        TermsEnum iter = terms.iterator();
        BytesRef term;
        for (long i = 0; (term = iter.next()) != null
                && sigles.size() < texts; i++) {
            if (i % step == 0)
                sigles.add(term.utf8ToString());
        };
        return sigles;
    };


    // KrillIndex.getFields(textSigle) and
    // KrillIndex.getFields(textSigle, fields)
    private List<Row> benchFields (KrillIndex ki, Set<String> textSigles) {
        List<Row> rows = new ArrayList<>();
        if (textSigles.isEmpty())
            return rows;

        final List<String> fields = new ArrayList<>(metaFields);

        Map<String, java.util.function.Function<String, MetaFields>> calls = new LinkedHashMap<>();
        calls.put("getFields(id)", id -> ki.getFields(id));
        calls.put("getFields(id, [" + String.join(",", fields) + "])",
                id -> ki.getFields(id, fields));

        for (var call : calls.entrySet()) {
            System.err.println("fields: " + call.getKey());
            Row row = new Row();
            row.type = "fields";
            row.name = call.getKey();
            row.matches = textSigles.size();
            rows.add(row);

            for (String sigle : textSigles) {
                for (int i = 0; i < warmup; i++)
                    call.getValue().apply(sigle).toJsonString();

                for (int i = 0; i < rounds; i++) {
                    long t0 = System.nanoTime();
                    MetaFields mf = call.getValue().apply(sigle);
                    long t1 = System.nanoTime();
                    String json = mf.toJsonString();
                    long t2 = System.nanoTime();

                    // E.g. 630: Document not found
                    if (mf.hasErrors() || json.isEmpty()) {
                        row.errors++;
                        row.note = sigle + ": "
                                + mf.getErrors().toJsonString();
                        break;
                    };
                    row.call.add(t1 - t0);
                    row.response.add(t2 - t0);
                };
            };
        };
        return rows;
    };


    // ------------------------------------------------------------
    // Index and queries
    // ------------------------------------------------------------

    private Directory openDiskIndex () throws IOException {
        Path path = Paths.get(indexPath);
        if (!Files.isDirectory(path))
            throw new IOException("Not an index directory: " + indexPath);
        return new MMapDirectory(path);
    };


    private Directory buildSampleIndex () throws IOException {
        Directory dir = new ByteBuffersDirectory();
        KrillIndex ki = new KrillIndex(dir);
        for (String doc : SAMPLE_DOCS) {
            String name = "/wiki/" + doc + ".json.gz";
            InputStream is = getClass().getResourceAsStream(name);
            if (is == null)
                throw new IOException(name + " not found on the classpath"
                        + " - run with the test classpath or pass --index");
            ki.addDoc(is, true);
        };
        ki.commit();
        // Releases the writer, the directory stays usable
        ki.close();
        return dir;
    };


    private static NamedQuery builtin (String name,
            Supplier<de.ids_mannheim.korap.query.wrap.SpanQueryWrapper> q) {
        return new NamedQuery(name, () -> new Krill(q.get()));
    };


    // Queries for the annotations of the wiki sample documents
    private List<NamedQuery> builtinQueries () {
        List<NamedQuery> qs = new ArrayList<>();

        qs.add(builtin("term [s:der]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg("s:der");
        }));

        qs.add(builtin("annotation [mate/m:gender:masc]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg("mate/m:gender:masc");
        }));

        qs.add(builtin("segment [tt/p:NN & mate/m:number:pl]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg("tt/p:NN").with("mate/m:number:pl");
        }));

        qs.add(builtin("regex [s:.*ung]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seg(qb.re("s:.*ung"));
        }));

        qs.add(builtin("sequence [tt/p:ART][tt/p:ADJA][tt/p:NN]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seq(qb.seg("tt/p:ART"), qb.seg("tt/p:ADJA"),
                    qb.seg("tt/p:NN"));
        }));

        qs.add(builtin("repetition [tt/p:ADJA]{2,3}[tt/p:NN]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seq(qb.repeat(qb.seg("tt/p:ADJA"), 2, 3),
                    qb.seg("tt/p:NN"));
        }));

        qs.add(builtin("class {1:[tt/p:ADJA]}[s:Sprache]", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.seq(qb.nr(1, qb.seg("tt/p:ADJA")), qb.seg("s:Sprache"));
        }));

        qs.add(builtin("contains(<base/s:s>, [tt/p:NE][tt/p:NE])", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.contains(qb.tag("base/s:s"),
                    qb.seq(qb.seg("tt/p:NE"), qb.seg("tt/p:NE")));
        }));

        qs.add(builtin("startswith(<base/s:s>, [tt/p:ART])", () -> {
            QueryBuilder qb = new QueryBuilder(FIELD);
            return qb.startswith(qb.tag("base/s:s"), qb.seg("tt/p:ART"));
        }));

        return qs;
    };


    // Load KoralQuery files (*.json, *.jsonld)
    private List<NamedQuery> loadQueries () throws IOException {
        List<Path> files = new ArrayList<>();
        for (String p : queryPaths) {
            Path path = Paths.get(p);
            if (Files.isDirectory(path)) {
                try (Stream<Path> s = Files.list(path)) {
                    s.filter(f -> Files.isRegularFile(f))
                            .filter(f -> f.toString().endsWith(".json")
                                    || f.toString().endsWith(".jsonld"))
                            .sorted().forEach(files::add);
                };
            }
            else if (Files.isRegularFile(path)) {
                files.add(path);
            }
            else {
                throw new IOException("Query file not found: " + p);
            };
        };

        List<NamedQuery> qs = new ArrayList<>();
        for (Path f : files) {
            final String json = new String(Files.readAllBytes(f),
                    StandardCharsets.UTF_8);
            qs.add(new NamedQuery(f.getFileName().toString(),
                    () -> new Krill(json)));
        };
        return qs;
    };


    // ------------------------------------------------------------
    // Reports
    // ------------------------------------------------------------

    private void println (String line) {
        System.out.println(line);
        report.append(line).append(System.lineSeparator());
    };


    private void println () {
        println("");
    };


    private void printf (String format, Object ... args) {
        String text = String.format(Locale.ROOT, format, args);
        System.out.print(text);
        report.append(text);
    };


    private static String ms (double v) {
        if (Double.isNaN(v))
            return "-";
        return String.format(Locale.ROOT, v < 10 ? "%.3f" : "%.1f", v);
    };


    private static String cut (String s, int width) {
        return s.length() <= width ? s : s.substring(0, width - 1) + "~";
    };


    private void printSearch (List<Row> rows) {
        int w = 12;
        for (Row r : rows)
            w = Math.max(w, Math.min(r.name.length(), 44));

        String fmt = "%-" + w + "s %12s %8s | %9s | %9s %9s %9s %9s %9s | %9s%n";
        println(
                "Search - response times in ms, json = search + JSON serialization");
        printf(fmt, "query", "results", "texts", "cold med",
                "warm min", "median", "mean", "p95", "max", "json med");
        println("-".repeat(w + 105));

        List<String> notes = new ArrayList<>();
        for (Row r : rows) {
            printf(fmt, cut(r.name, w),
                    r.totalResults < 0 ? "-" : r.totalResults,
                    r.totalResources < 0 ? "-" : r.totalResources,
                    ms(r.cold.median()), ms(r.call.min()),
                    ms(r.call.median()), ms(r.call.mean()),
                    ms(r.call.percentile(95)), ms(r.call.max()),
                    ms(r.response.median()));
            if (!r.note.isEmpty())
                notes.add("  ! " + r.name + ": " + r.note);
        };
        for (String n : notes)
            println(n);
        println();
    };


    private void printMatchInfo (List<Row> rows, int total) {
        int w = 12;
        for (Row r : rows)
            w = Math.max(w, Math.min(r.name.length(), 44));

        String fmt = "%-" + w + "s %8s %7s | %9s %9s %9s %9s %9s | %9s%n";
        println("Match info - response times in ms over " + total
                + " matches, json = getMatchInfo + JSON serialization");
        printf(fmt, "request", "calls", "failed", "min", "median",
                "mean", "p95", "max", "json med");
        println("-".repeat(w + 80));

        for (Row r : rows) {
            // Rows per source query are only written to the CSV
            if (!r.note.isEmpty())
                continue;
            printf(fmt, cut(r.name, w), r.call.size(), r.errors,
                    ms(r.call.min()), ms(r.call.median()), ms(r.call.mean()),
                    ms(r.call.percentile(95)), ms(r.call.max()),
                    ms(r.response.median()));
        };
        println();
    };


    private void printMeta (List<Row> rows, int texts) {
        int w = 12;
        for (Row r : rows)
            w = Math.max(w, Math.min(r.name.length(), 44));

        String fmt = "%-" + w
                + "s %12s %7s %7s | %9s | %9s %9s %9s %9s %9s | %9s%n";
        println("Statistics and metadata fields - response times in ms, getFields over "
                + texts + " texts, json = call + JSON serialization");
        printf(fmt, "request", "value", "calls", "failed", "cold med",
                "warm min", "median", "mean", "p95", "max", "json med");
        println("-".repeat(w + 113));

        List<String> notes = new ArrayList<>();
        for (Row r : rows) {
            printf(fmt, cut(r.name, w),
                    r.totalResults < 0 ? "-" : r.totalResults, r.call.size(),
                    r.errors, ms(r.cold.median()), ms(r.call.min()),
                    ms(r.call.median()), ms(r.call.mean()),
                    ms(r.call.percentile(95)), ms(r.call.max()),
                    ms(r.response.median()));
            if (!r.note.isEmpty())
                notes.add("  ! " + r.name + ": " + r.note);
        };
        for (String n : notes)
            println(n);
        if (texts == 0)
            println("  ! No texts with a textSigle - getFields benchmark skipped.");
        println();
    };


    // Write everything reported so far to the output file
    private void writeReport () throws IOException {
        Path path;
        if (outputPath != null) {
            path = Paths.get(outputPath);
        }
        else {
            String name = "krill-benchmark-" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                    + ".txt";
            // Maven's build directory is ignored by git
            path = Files.isDirectory(Paths.get("target"))
                    ? Paths.get("target", name)
                    : Paths.get(name);
        };

        if (path.toAbsolutePath().getParent() != null)
            Files.createDirectories(path.toAbsolutePath().getParent());
        Files.write(path, report.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("Report written to " + path.toAbsolutePath());
    };


    private static String csv (String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    };


    private static String num (double v) {
        return Double.isNaN(v) ? "" : String.format(Locale.ROOT, "%.4f", v);
    };


    private void writeCsv (List<Row> searchRows, List<Row> infoRows)
            throws IOException {
        File file = new File(csvPath);
        try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
            out.println("type,name,source_query,total_results,total_texts,"
                    + "items,rounds,failed,cold_median_ms,min_ms,median_ms,"
                    + "mean_ms,p95_ms,max_ms,json_median_ms,note");

            List<Row> rows = new ArrayList<>(searchRows);
            rows.addAll(infoRows);

            for (Row r : rows) {
                boolean info = r.type.equals("matchinfo");
                out.println(String.join(",", r.type, csv(r.name),
                        csv(info ? (r.note.isEmpty() ? "*" : r.note) : ""),
                        (info || r.totalResults < 0) ? ""
                                : String.valueOf(r.totalResults),
                        (info || r.totalResources < 0) ? ""
                                : String.valueOf(r.totalResources),
                        r.matches > 0 ? String.valueOf(r.matches) : "",
                        String.valueOf(r.call.size()),
                        String.valueOf(r.errors), num(r.cold.median()),
                        num(r.call.min()), num(r.call.median()),
                        num(r.call.mean()), num(r.call.percentile(95)),
                        num(r.call.max()), num(r.response.median()),
                        csv(info ? "" : r.note)));
            };
        };
    };


    // ------------------------------------------------------------
    // Options
    // ------------------------------------------------------------

    private boolean parseArgs (String[] argv) {
        try {
            for (int i = 0; i < argv.length; i++) {
                switch (argv[i]) {
                    case "-i", "--index" -> indexPath = argv[++i];
                    case "-q", "--queries" -> queryPaths.add(argv[++i]);
                    case "-r", "--rounds" -> rounds = Integer
                            .parseInt(argv[++i]);
                    case "--cold" -> coldRounds = Integer.parseInt(argv[++i]);
                    case "--warmup" -> warmup = Integer.parseInt(argv[++i]);
                    case "-c", "--count" -> count = Integer
                            .parseInt(argv[++i]);
                    case "--timeout" -> timeout = Long.parseLong(argv[++i]);
                    case "--no-snippets" -> snippets = false;
                    case "-m", "--matches" -> matchesPerQuery = Integer
                            .parseInt(argv[++i]);
                    case "--foundry" -> foundry = argv[++i];
                    case "--layer" -> layer = argv[++i];
                    case "--csv" -> csvPath = argv[++i];
                    case "-o", "--output" -> outputPath = argv[++i];
                    case "--number-of" -> numberOfTypes = List
                            .of(argv[++i].split(","));
                    case "--texts" -> texts = Integer.parseInt(argv[++i]);
                    case "--fields" -> metaFields = List
                            .of(argv[++i].split(","));
                    case "-h", "--help" -> {
                        usage();
                        return false;
                    }
                    default -> {
                        System.err.println("Unknown option: " + argv[i]);
                        usage();
                        return false;
                    }
                };
            };
        }
        catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            System.err.println("Invalid arguments: " + e.getMessage());
            usage();
            return false;
        };

        if (rounds < 1 || coldRounds < 0 || warmup < 0
                || matchesPerQuery < 0 || texts < 0) {
            System.err.println("Invalid number of rounds or matches");
            return false;
        };
        return true;
    };


    private static void usage () {
        System.err.println(String.join("\n",
                "Usage: KrillBenchmark [options]", "",
                "  -i, --index <dir>     Lucene index directory",
                "                        (default: in-memory index of the wiki test samples)",
                "  -q, --queries <path>  KoralQuery file (*.json, *.jsonld) or a directory",
                "                        of such files; may be repeated",
                "                        (default: built-in queries for the wiki samples)",
                "  -r, --rounds <n>      Measured warm rounds per query (default: 20)",
                "      --cold <n>        Cold rounds per query, each on a fresh KrillIndex",
                "                        (default: 3)",
                "      --warmup <n>      Unmeasured rounds before the warm rounds (default: 5)",
                "  -c, --count <n>       Matches per page; overrides the query's meta",
                "      --timeout <ms>    Search timeout; overrides the query's meta",
                "      --no-snippets     Search without snippets",
                "  -m, --matches <n>     Matches per query to fetch match info for;",
                "                        0 skips the match info benchmark (default: 5)",
                "      --foundry <name>  Restrict match info annotations to a foundry",
                "      --layer <name>    Restrict match info annotations to a layer",
                "      --number-of <t>   Comma separated types for numberOf()",
                "                        (default: documents)",
                "      --texts <n>       Texts to fetch metadata fields for;",
                "                        0 skips the getFields benchmark (default: 10)",
                "      --fields <f>      Comma separated fields for getFields(id, fields)",
                "                        (default: textSigle,title,author)",
                "  -o, --output <file>   Report file (default:",
                "                        target/krill-benchmark-<timestamp>.txt)",
                "      --csv <file>      Also write all results to a CSV file",
                "  -h, --help            Show this help"));
    };
};
