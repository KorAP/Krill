package de.ids_mannheim.korap;

import static org.junit.Assert.assertEquals;

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

    @Test
    public void testArguments () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/bzk"});
        assertEquals("Added or updated 1 file.\n", outputStream.toString());
    }

    @Test
    public void testOutputArgument () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/bzk", "-o", "test-output"});
        assertEquals("Added or updated 1 file.\n", outputStream.toString());
    }

    @Test
    public void testMultipleInputFiles () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i", "src/test/resources/wiki"});
        assertEquals("Added or updated 19 files.\n", outputStream.toString());
    }


    @Test
    public void testAdding () throws IOException {
        Indexer.main(new String[] {
                "-c", "src/test/resources/krill.properties",
                "-i", "src/test/resources/bzk",
                "-a"});
        logger.info(outputStream.toString());
        assertEquals(outputStream.toString(), "Added 1 file.\n");
    }

    
    @Test
    public void testMultipleInputDirectories () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                                    "-i",
                                    "src/test/resources/bzk;src/test/resources/goe;src/test/resources/sgbr",
                                    "-o", "test-index"});
        assertEquals("Added or updated 5 files.\n", outputStream.toString());
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
        assertEquals(outputStream.toString(), "Added 1 file.\n");
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
        assertEquals("Added or updated 1 file.\n", outputStream.toString());
        
        tempPropertiesFile.delete();
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
        if (outputDirectory.exists()) {
            deleteFile(outputDirectory);
        }
        if (outputDirectory2.exists()) {
            deleteFile(outputDirectory2);
        }
        if (outputDirectory3.exists()) {
            deleteFile(outputDirectory3);
        }
        if (outputDirectory4.exists()) {
            deleteFile(outputDirectory4);
        }
    }

    
    @Before
    public void cleanOutputDirectory () {

        if (outputDirectory.exists()) {
            logger.debug("Output directory exists");
            deleteFile(outputDirectory);
        }
        if (outputDirectory2.exists()) {
            logger.debug("Output directory 2 exists");
            deleteFile(outputDirectory2);
        }
        if (outputDirectory3.exists()) {
            logger.debug("Output directory 3 exists");
            deleteFile(outputDirectory3);
        }
        if (outputDirectory4.exists()) {
            logger.debug("Output directory 4 exists");
            deleteFile(outputDirectory4);
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
