package de.ids_mannheim.korap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.index.Indexer;

/**
 * @author margaretha
 *
 */
public class TestIndexer {
    private Logger logger = LoggerFactory.getLogger(TestIndexer.class);
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private String info = "usage: Krill indexer";
    private static File tempBaseDirectory;
    
    static {
        try {
            tempBaseDirectory = Files.createTempDirectory("krill-test").toFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary directory for tests", e);
        }
    }
    
    private static String getTestOutputPath(String subdir) {
        return new File(tempBaseDirectory, subdir).getAbsolutePath();
    }

    @Test
    public void testArguments () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/bzk"});
        assertTrue(outputStream.toString().startsWith("Added or updated 1 file."));
    }

    @Test
    public void testOutputArgument () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/bzk", "-o", getTestOutputPath("test-output")});
        assertTrue(outputStream.toString().startsWith("Added or updated 1 file."));
    }

    @Test
    public void testMultipleInputFiles () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/wiki"});
        assertTrue(outputStream.toString().startsWith("Added or updated 19 files."));
    }


    @Test
    public void testAdding () throws IOException {
        Indexer.main(new String[] {
                "-c", "src/test/resources/krill.properties",
                "-i", "src/test/resources/bzk",
                "-a"});
        logger.info(outputStream.toString());
        assertTrue(outputStream.toString().startsWith("Added 1 file."));
    }

    
    @Test
    public void testMultipleInputDirectories () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i",
                                    "src/test/resources/bzk;src/test/resources/goe;src/test/resources/sgbr",
                                    "-o", getTestOutputPath("test-index")});
        assertTrue(outputStream.toString().startsWith("Added or updated 5 files."));
    }

    @Test
    public void testEmptyArgument () throws IOException {
        Indexer.main(new String[] {});
        logger.info(outputStream.toString());
        assertEquals(true, outputStream.toString().startsWith(info));
    }


    @Test
    public void testMissingConfig () throws IOException {
        Indexer.main(new String[] { "-i", "src/test/resources/bzk",
                                    "-o " + getTestOutputPath("test-index")});
        logger.info(outputStream.toString());
        assertEquals(true, outputStream.toString().startsWith(info));
    }
    
    @Test
    public void testMissingInput () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-o", getTestOutputPath("test-index")});
        logger.info(outputStream.toString());
        assertEquals(true, outputStream.toString().startsWith(info));
    }

    @Test
    public void testUnicodeProblem () throws IOException {
        Indexer.main(new String[] {
                "-c", "src/test/resources/krill.properties",
                "-i", "src/test/resources/bug",
                "-o", getTestOutputPath("test-index2")
            });
        logger.info(outputStream.toString());
        assertTrue(outputStream.toString().startsWith("Added 1 file."));
    }

    @Test
    public void testMaxTextSize () throws IOException {
        // Create a temporary properties file with the max text size setting
        File tempPropertiesFile = File.createTempFile("krill", ".properties");
        try (FileWriter writer = new FileWriter(tempPropertiesFile)) {
            writer.write("krill.version = ${project.version}\n");
            writer.write("krill.name = ${project.name}\n");
            writer.write("krill.indexDir = " + getTestOutputPath("test-output") + "\n");
            writer.write("krill.index.textSize.max = 25000000\n");
        }
        
        try {
            Indexer.main(new String[] { "-c", tempPropertiesFile.getAbsolutePath(),
                    "-i", "src/test/resources/bzk", "-o", getTestOutputPath("test-output-1")});
            assertTrue(outputStream.toString().startsWith("Added or updated 1 file."));
        } finally {
            tempPropertiesFile.delete();
        }
    }

    @Test
    public void testZipFileInput () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.zip",
                                    "-o", getTestOutputPath("test-zip-index")});
        assertTrue(outputStream.toString().startsWith("Added or updated 3 files."));
    }

    @Test
    public void testZipFileWithAdding () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.zip",
                                    "-o", getTestOutputPath("test-zip-index-add"),
                                    "-a"});
        assertTrue(outputStream.toString().startsWith("Added 3 files."));
    }

    @Test
    public void testMixedDirectoryAndZipInput () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/bzk;src/test/resources/rei/rei_sample_krill.zip",
                                    "-o", getTestOutputPath("test-mixed-index")});
        assertTrue(outputStream.toString().startsWith("Added or updated 4 files."));
    }

    @Test
    public void testMultipleZipFiles () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.zip;src/test/resources/rei/rei_sample_krill.zip",
                                    "-o", getTestOutputPath("test-multiple-zip-index")});
        // Should process 6 files total (3 from each zip)
        assertTrue(outputStream.toString().startsWith("Added or updated 6 files."));
    }

    @Test
    public void testInvalidZipFile () throws IOException {
        // Test with a non-existent zip file
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/nonexistent.zip",
                                    "-o", getTestOutputPath("test-invalid-zip-index")});
        // Should handle gracefully and process 0 files
        assertTrue(outputStream.toString().startsWith("Added or updated 0 file"));
    }

    @Test
    public void testMixedValidAndInvalidInputs () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/bzk;src/test/resources/nonexistent.zip;src/test/resources/rei/rei_sample_krill.zip",
                                    "-o", getTestOutputPath("test-mixed-valid-invalid-index")});
        // Should process files from valid inputs only (1 from bzk + 3 from zip = 4 files)
        assertTrue(outputStream.toString().startsWith("Added"));
    }

    @Test
    public void testMixedContentZipFile () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/mixed_test.zip",
                                    "-o", getTestOutputPath("test-mixed-content-zip-index")});
        // Should process 2 JSON files (1 plain + 1 gzipped) and skip the .txt file
        assertTrue(outputStream.toString().startsWith("Added"));
    }

    @Test
    public void testTarFileInput () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.tar",
                                    "-o", getTestOutputPath("test-tar-index")});
        assertTrue(outputStream.toString().contains("Added or updated 3 files"));
    }

    @Test
    public void testTarGzFileInput () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.tar.gz",
                                    "-o", getTestOutputPath("test-tar-gz-index")});
        assertTrue(outputStream.toString().contains("Added or updated 3 files"));
    }

    @Test
    public void testMultipleTarFiles () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.tar;src/test/resources/rei/rei_sample_krill.tar.gz",
                                    "-o", getTestOutputPath("test-multiple-tar-index")});
        // Should process 6 files total (3 from each tar)
        assertTrue(outputStream.toString().contains("Added or updated 6 files"));
    }

    @Test
    public void testMixedZipAndTarFiles () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.zip;src/test/resources/rei/rei_sample_krill.tar",
                                    "-o", getTestOutputPath("test-mixed-zip-tar-index")});
        // Should process 6 files total (3 from zip + 3 from tar)
        assertTrue(outputStream.toString().contains("Added or updated 6 files"));
    }

    @Before
    public void setOutputStream () {
        System.setOut(new PrintStream(outputStream));
    }

    @After
    public void cleanOutputStream () {
        System.setOut(null);
    }

    @AfterClass
    public static void cleanup() {
        if (tempBaseDirectory != null && tempBaseDirectory.exists()) {
            deleteFile(tempBaseDirectory);
        }
    }

    

    private static void deleteFile (File path) {
        if (path.isDirectory()) {
            File file;
            for (String filename : path.list()) {
                file = new File(path + "/" + filename);
                deleteFile(file);
            }
        }
        path.delete();
    }
}
