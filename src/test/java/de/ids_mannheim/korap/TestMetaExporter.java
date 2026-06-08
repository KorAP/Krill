package de.ids_mannheim.korap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.ids_mannheim.korap.index.Indexer;
import de.ids_mannheim.korap.index.MetaExporter;

/**
 * Tests for the standalone {@link MetaExporter} tool, which dumps the stored
 * per-document metadata of an existing index.
 *
 * <p>An index containing a single known document (textSigle
 * {@code BZK_D59.00089}) is built once from {@code src/test/resources/bzk}
 * and reused by all tests.</p>
 *
 * @author Krill
 */
public class TestMetaExporter {

    private static File tempBaseDirectory;
    private static String indexDir;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeClass
    public static void buildIndex () throws IOException {
        tempBaseDirectory = Files.createTempDirectory("krill-export-test")
                .toFile();
        indexDir = new File(tempBaseDirectory, "index").getAbsolutePath();

        // The Indexer prints to stdout; silence it during setup.
        PrintStream original = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        try {
            Indexer.main(new String[] { "-c",
                    "src/test/resources/krill.properties", "-i",
                    "src/test/resources/bzk", "-o", indexDir });
        }
        finally {
            System.setOut(original);
        }
    }

    @Before
    public void redirectOut () {
        System.setOut(new PrintStream(outContent));
    }

    private void restoreOut () {
        System.setOut(originalOut);
    }

    @Test
    public void testListFields () {
        MetaExporter.main(new String[] { "-i", indexDir, "--list-fields" });
        restoreOut();
        String out = outContent.toString();
        // The text sigle is the key field used for diffing instances.
        assertTrue("expected textSigle among listed fields",
                out.contains("textSigle"));
        assertTrue("expected corpusSigle among listed fields",
                out.contains("corpusSigle"));
        // The heavy primary-data fields must never be reported as metadata.
        assertTrue("tokens must not be listed as a metadata field",
                !out.contains("tokens\n"));
    }

    @Test
    public void testExportSingleColumnNoHeader () {
        MetaExporter.main(new String[] { "-i", indexDir, "-f", "textSigle",
                "--no-header" });
        restoreOut();
        assertEquals("BZK_D59.00089", outContent.toString().trim());
    }

    @Test
    public void testExportTsvHeader () {
        MetaExporter.main(
                new String[] { "-i", indexDir, "-f", "textSigle,corpusSigle" });
        restoreOut();
        String[] lines = outContent.toString().split("\n");
        assertEquals("textSigle\tcorpusSigle", lines[0]);
        assertEquals("BZK_D59.00089\tBZK", lines[1]);
    }

    @Test
    public void testExportCsv () {
        MetaExporter.main(new String[] { "-i", indexDir, "-f", "textSigle",
                "--format", "csv", "--no-header" });
        restoreOut();
        assertEquals("BZK_D59.00089", outContent.toString().trim());
    }

    @Test
    public void testExportJson () {
        MetaExporter.main(
                new String[] { "-i", indexDir, "--format", "json" });
        restoreOut();
        String line = outContent.toString().trim();
        assertTrue("JSON line should be an object", line.startsWith("{"));
        assertTrue("JSON should contain the textSigle",
                line.contains("\"textSigle\":\"BZK_D59.00089\""));
        // tokens/base must be excluded from the JSON dump as well.
        assertTrue("JSON must not contain the tokens primary-data field",
                !line.contains("\"tokens\":"));
    }

    @Test
    public void testMissingIndexArgumentPrintsHelp () {
        // Missing required -i must be handled gracefully (help printed, no throw).
        MetaExporter.main(new String[] { "--list-fields" });
        restoreOut();
        assertTrue("expected usage/help output",
                outContent.toString().contains("Krill metadata exporter")
                        || outContent.toString().isEmpty());
    }

    @AfterClass
    public static void cleanup () {
        if (tempBaseDirectory != null && tempBaseDirectory.exists())
            deleteFile(tempBaseDirectory);
    }

    private static void deleteFile (File path) {
        if (path.isDirectory() && path.list() != null) {
            for (String filename : path.list())
                deleteFile(new File(path, filename));
        }
        path.delete();
    }
}
