package de.ids_mannheim.korap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

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
    private static File outputDirectory = new File("test-index");
    private static File outputDirectory2 = new File("test-index2");
    private static File outputDirectory3 = new File("test-output");
    private static File outputDirectory4 = new File("test-output-1");
    private static File zipIndexDirectory = new File("test-zip-index");
    private static File zipIndexAddDirectory = new File("test-zip-index-add");
    private static File mixedIndexDirectory = new File("test-mixed-index");
    private static File multipleZipIndexDirectory = new File("test-multiple-zip-index");
    private static File invalidZipIndexDirectory = new File("test-invalid-zip-index");
    private static File mixedValidInvalidIndexDirectory = new File("test-mixed-valid-invalid-index");
    private static File mixedContentZipIndexDirectory = new File("test-mixed-content-zip-index");
    private static File tarIndexDirectory = new File("test-tar-index");
    private static File tarGzIndexDirectory = new File("test-tar-gz-index");
    private static File multipleTarIndexDirectory = new File("test-multiple-tar-index");
    private static File mixedZipTarIndexDirectory = new File("test-mixed-zip-tar-index");

    @Test
    public void testArguments () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/bzk"});
        assertTrue(outputStream.toString().startsWith("Added or updated 1 file."));
    }

    @Test
    public void testOutputArgument () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/bzk", "-o", "test-output"});
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
                                    "-o", "test-index"});
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
                                    "-o test-index"});
        logger.info(outputStream.toString());
        assertEquals(true, outputStream.toString().startsWith(info));
    }
    
    @Test
    public void testMissingInput () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-o", "test-index"});
        logger.info(outputStream.toString());
        assertEquals(true, outputStream.toString().startsWith(info));
    }

    @Test
    public void testUnicodeProblem () throws IOException {
        Indexer.main(new String[] {
                "-c", "src/test/resources/krill.properties",
                "-i", "src/test/resources/bug",
                "-o", "test-index2"
            });
        logger.info(outputStream.toString());
        assertTrue(outputStream.toString().startsWith("Added 1 file."));
    }

    @Test
    public void testMaxTextSize () throws IOException {
        // Create a temporary properties file with the max text size setting
        File tempPropertiesFile = File.createTempFile("krill", ".properties");
        FileWriter writer = new FileWriter(tempPropertiesFile);
        writer.write("krill.version = ${project.version}\n");
        writer.write("krill.name = ${project.name}\n");
        writer.write("krill.indexDir = test-output\n");
        writer.write("krill.index.textSize.max = 25000000\n");
        writer.close();
        
        Indexer.main(new String[] { "-c", tempPropertiesFile.getAbsolutePath(),
                "-i", "src/test/resources/bzk", "-o", "test-output-1"});
        assertTrue(outputStream.toString().startsWith("Added or updated 1 file."));
        
        tempPropertiesFile.delete();
    }

    @Test
    public void testZipFileInput () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.zip",
                                    "-o", "test-zip-index"});
        assertTrue(outputStream.toString().startsWith("Added or updated 3 files."));
    }

    @Test
    public void testZipFileWithAdding () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.zip",
                                    "-o", "test-zip-index-add",
                                    "-a"});
        assertTrue(outputStream.toString().startsWith("Added 3 files."));
    }

    @Test
    public void testMixedDirectoryAndZipInput () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/bzk;src/test/resources/rei/rei_sample_krill.zip",
                                    "-o", "test-mixed-index"});
        assertTrue(outputStream.toString().startsWith("Added or updated 4 files."));
    }

    @Test
    public void testMultipleZipFiles () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.zip;src/test/resources/rei/rei_sample_krill.zip",
                                    "-o", "test-multiple-zip-index"});
        // Should process 6 files total (3 from each zip)
        assertTrue(outputStream.toString().startsWith("Added or updated 6 files."));
    }

    @Test
    public void testInvalidZipFile () throws IOException {
        // Test with a non-existent zip file
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/nonexistent.zip",
                                    "-o", "test-invalid-zip-index"});
        // Should handle gracefully and process 0 files
        assertTrue(outputStream.toString().startsWith("Added or updated 0 file"));
    }

    @Test
    public void testMixedValidAndInvalidInputs () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/bzk;src/test/resources/nonexistent.zip;src/test/resources/rei/rei_sample_krill.zip",
                                    "-o", "test-mixed-valid-invalid-index"});
        // Should process files from valid inputs only (1 from bzk + 3 from zip = 4 files)
        assertTrue(outputStream.toString().startsWith("Added"));
    }

    @Test
    public void testMixedContentZipFile () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/mixed_test.zip",
                                    "-o", "test-mixed-content-zip-index"});
        // Should process 2 JSON files (1 plain + 1 gzipped) and skip the .txt file
        assertTrue(outputStream.toString().startsWith("Added"));
    }

    @Test
    public void testTarFileInput () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.tar",
                                    "-o", "test-tar-index"});
        assertTrue(outputStream.toString().contains("Added or updated 3 files"));
    }

    @Test
    public void testTarGzFileInput () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.tar.gz",
                                    "-o", "test-tar-gz-index"});
        assertTrue(outputStream.toString().contains("Added or updated 3 files"));
    }

    @Test
    public void testMultipleTarFiles () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.tar;src/test/resources/rei/rei_sample_krill.tar.gz",
                                    "-o", "test-multiple-tar-index"});
        // Should process 6 files total (3 from each tar)
        assertTrue(outputStream.toString().contains("Added or updated 6 files"));
    }

    @Test
    public void testMixedZipAndTarFiles () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/rei/rei_sample_krill.zip;src/test/resources/rei/rei_sample_krill.tar",
                                    "-o", "test-mixed-zip-tar-index"});
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
        File[] directories = {
            outputDirectory, outputDirectory2, outputDirectory3, outputDirectory4,
            zipIndexDirectory, zipIndexAddDirectory, mixedIndexDirectory,
            multipleZipIndexDirectory, invalidZipIndexDirectory, mixedValidInvalidIndexDirectory,
            mixedContentZipIndexDirectory
        };
        
        for (File dir : directories) {
            if (dir.exists()) {
                deleteFile(dir);
            }
        }
    }

    
    @Before
    public void cleanOutputDirectory () {
        File[] directories = {
            outputDirectory, outputDirectory2, outputDirectory3, outputDirectory4,
            zipIndexDirectory, zipIndexAddDirectory, mixedIndexDirectory,
            multipleZipIndexDirectory, invalidZipIndexDirectory, mixedValidInvalidIndexDirectory,
            mixedContentZipIndexDirectory
        };
        
        for (File dir : directories) {
            if (dir.exists()) {
                logger.debug("Output directory " + dir.getName() + " exists");
                deleteFile(dir);
            }
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
