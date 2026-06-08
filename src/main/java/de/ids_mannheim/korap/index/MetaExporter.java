package de.ids_mannheim.korap.index;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Bits;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Standalone tool to export the per-document metadata stored in an existing
 * Krill (Lucene) index, without going through the web service API.
 *
 * <p>All metadata in a Krill index is kept as Lucene <i>stored fields</i>; only
 * the primary-data fields {@code tokens} and {@code base} carry the heavy
 * annotation streams and are skipped here. This tool iterates over every live
 * (non-deleted) document and prints the requested fields as TSV, CSV or
 * line-delimited JSON.</p>
 *
 * <p>Typical use: dump the {@code textSigle} column of two index instances and
 * diff the sorted lists to find which texts are missing from / surplus in one
 * of them.</p>
 *
 * <pre>
 * Usage:
 *
 * # List which metadata fields exist in the index
 * java -cp Krill.jar de.ids_mannheim.korap.index.MetaExporter \
 *      -i /path/to/index --list-fields
 *
 * # Dump just the text sigles (one per line)
 * java -cp Krill.jar de.ids_mannheim.korap.index.MetaExporter \
 *      -i /path/to/index -f textSigle
 *
 * # Dump several columns as TSV with a header row
 * java -cp Krill.jar de.ids_mannheim.korap.index.MetaExporter \
 *      -i /path/to/index -f textSigle,author,title,pubDate
 *
 * # Dump all stored metadata fields of every document as JSON lines
 * java -cp Krill.jar de.ids_mannheim.korap.index.MetaExporter \
 *      -i /path/to/index --format json
 * </pre>
 *
 * @author Krill
 */
public class MetaExporter {

    // Primary-data fields that are never metadata and may be huge.
    private static final Set<String> SKIP_FIELDS =
            new LinkedHashSet<>(Arrays.asList("tokens", "base"));

    private final DirectoryReader reader;

    public MetaExporter (String indexPath) throws IOException {
        this.reader = DirectoryReader.open(
                new MMapDirectory(Paths.get(indexPath)));
    }

    public void close () throws IOException {
        this.reader.close();
    }

    /**
     * Collect the sorted union of all stored metadata field names across the
     * whole index.
     */
    public Set<String> collectFieldNames () throws IOException {
        Set<String> names = new TreeSet<>();
        for (LeafReaderContext lrc : reader.leaves()) {
            LeafReader lr = lrc.reader();
            Bits liveDocs = lr.getLiveDocs();
            int max = lr.maxDoc();
            for (int i = 0; i < max; i++) {
                if (liveDocs != null && !liveDocs.get(i))
                    continue;
                Document doc = lr.document(i);
                for (IndexableField f : doc.getFields()) {
                    String name = f.name();
                    if (!SKIP_FIELDS.contains(name))
                        names.add(name);
                }
            }
        }
        return names;
    }

    private enum Format {
        TSV, CSV, JSON
    }

    /**
     * Export the selected fields of every live document.
     *
     * @param fields
     *            ordered list of field names to output; if {@code null} the
     *            sorted union of all stored fields is used.
     * @param format
     *            output format.
     * @param header
     *            whether to emit a header row (TSV/CSV only).
     * @param multiSep
     *            separator used to join multi-valued fields (TSV/CSV only).
     */
    public long export (List<String> fields, Format format, boolean header,
            String multiSep, Writer out) throws IOException {

        // Resolve field list lazily for tabular output if not given
        if (fields == null && format != Format.JSON) {
            fields = new ArrayList<>(collectFieldNames());
        }

        char sep = (format == Format.CSV) ? ',' : '\t';
        ObjectMapper mapper = (format == Format.JSON) ? new ObjectMapper() : null;

        if (format != Format.JSON && header) {
            writeRow(out, fields, format, sep, null);
        }

        long count = 0;
        for (LeafReaderContext lrc : reader.leaves()) {
            LeafReader lr = lrc.reader();
            Bits liveDocs = lr.getLiveDocs();
            int max = lr.maxDoc();
            for (int i = 0; i < max; i++) {
                if (liveDocs != null && !liveDocs.get(i))
                    continue;
                Document doc = lr.document(i);

                if (format == Format.JSON) {
                    writeJson(out, mapper, doc, fields);
                }
                else {
                    List<String> values = new ArrayList<>(fields.size());
                    for (String name : fields) {
                        String[] vs = doc.getValues(name);
                        values.add(vs.length == 0 ? ""
                                : String.join(multiSep, vs));
                    }
                    writeRow(out, values, format, sep, null);
                }
                count++;
            }
        }
        out.flush();
        return count;
    }

    private void writeJson (Writer out, ObjectMapper mapper, Document doc,
            List<String> fields) throws IOException {
        ObjectNode node = mapper.createObjectNode();
        if (fields != null) {
            for (String name : fields) {
                String[] vs = doc.getValues(name);
                if (vs.length == 1)
                    node.put(name, vs[0]);
                else if (vs.length > 1) {
                    for (String v : vs)
                        node.withArray(name).add(v);
                }
            }
        }
        else {
            // All stored fields except the primary-data ones
            for (IndexableField f : doc.getFields()) {
                String name = f.name();
                if (SKIP_FIELDS.contains(name))
                    continue;
                String v = f.stringValue();
                if (v == null)
                    continue;
                if (node.has(name)) {
                    node.withArray(name).add(v);
                }
                else {
                    node.put(name, v);
                }
            }
        }
        out.write(mapper.writeValueAsString(node));
        out.write('\n');
    }

    private void writeRow (Writer out, List<String> values, Format format,
            char sep, String unused) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0)
                sb.append(sep);
            sb.append(escape(values.get(i), format, sep));
        }
        sb.append('\n');
        out.write(sb.toString());
    }

    private String escape (String value, Format format, char sep) {
        if (value == null)
            value = "";
        if (format == Format.CSV) {
            boolean needQuote = value.indexOf(sep) >= 0
                    || value.indexOf('"') >= 0
                    || value.indexOf('\n') >= 0
                    || value.indexOf('\r') >= 0;
            if (needQuote) {
                return '"' + value.replace("\"", "\"\"") + '"';
            }
            return value;
        }
        // TSV: strip embedded tabs/newlines so the row stays intact
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    public static void main (String[] argv) {
        Options options = new Options();
        options.addOption(Option.builder("i").longOpt("index")
                .desc("index directory to read (required)").hasArg()
                .argName("index dir").required().build());
        options.addOption(Option.builder("f").longOpt("fields")
                .desc("comma-separated list of metadata fields/columns to "
                        + "export. Defaults to all stored fields.")
                .hasArg().argName("field,field,...").build());
        options.addOption(Option.builder("o").longOpt("output")
                .desc("output file (defaults to stdout).").hasArg()
                .argName("file").build());
        options.addOption(Option.builder().longOpt("format")
                .desc("output format: tsv (default), csv or json (one JSON "
                        + "object per line).")
                .hasArg().argName("tsv|csv|json").build());
        options.addOption(Option.builder().longOpt("no-header")
                .desc("do not print a header row (tsv/csv).").build());
        options.addOption(Option.builder().longOpt("multi-sep")
                .desc("separator for multi-valued fields in tsv/csv "
                        + "(default '|').")
                .hasArg().argName("sep").build());
        options.addOption(Option.builder().longOpt("list-fields")
                .desc("only list the metadata field names present in the "
                        + "index and exit.")
                .build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, argv);
        }
        catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    "Krill metadata exporter\n java -cp Krill.jar "
                            + "de.ids_mannheim.korap.index.MetaExporter "
                            + "-i <index dir> [-f <fields>] "
                            + "[--format tsv|csv|json] [-o <file>] "
                            + "[--list-fields]",
                    options);
            System.err.println("\n" + e.getMessage());
            return;
        }

        String indexPath = cmd.getOptionValue("i");
        MetaExporter exporter = null;
        try {
            exporter = new MetaExporter(indexPath);

            if (cmd.hasOption("list-fields")) {
                PrintStream ps = System.out;
                for (String name : exporter.collectFieldNames()) {
                    ps.println(name);
                }
                return;
            }

            Format format = Format.TSV;
            String fmt = cmd.getOptionValue("format");
            if (fmt != null) {
                switch (fmt.toLowerCase()) {
                    case "tsv": format = Format.TSV; break;
                    case "csv": format = Format.CSV; break;
                    case "json": format = Format.JSON; break;
                    default:
                        System.err.println("Unknown format: " + fmt);
                        return;
                }
            }

            List<String> fields = null;
            if (cmd.hasOption("f")) {
                fields = new ArrayList<>();
                for (String part : cmd.getOptionValue("f").split(",")) {
                    String t = part.trim();
                    if (!t.isEmpty())
                        fields.add(t);
                }
            }

            boolean header = !cmd.hasOption("no-header");
            String multiSep = cmd.getOptionValue("multi-sep", "|");

            OutputStream os;
            boolean closeOut = false;
            if (cmd.hasOption("o")) {
                os = Files.newOutputStream(Paths.get(cmd.getOptionValue("o")));
                closeOut = true;
            }
            else {
                os = System.out;
            }

            Writer out = new BufferedWriter(
                    new OutputStreamWriter(os, StandardCharsets.UTF_8));
            long n = exporter.export(fields, format, header, multiSep, out);
            out.flush();
            if (closeOut)
                out.close();

            System.err.println("Exported " + n + " documents.");
        }
        catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (exporter != null) {
                try {
                    exporter.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
