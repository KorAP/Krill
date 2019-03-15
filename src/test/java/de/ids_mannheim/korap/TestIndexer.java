package de.ids_mannheim.korap;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
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
    private File outputDirectory = new File("test-index");

    @Test
    public void testArguments () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                "-i", "src/test/resources/bzk" });
        assertEquals("Added or updated 1 file.", outputStream.toString());
    }

    @Test
    public void testOutputArgument () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                "-i", "src/test/resources/bzk", "-o", "test-output"});
        assertEquals("Added or updated 1 file.", outputStream.toString());
    }

    @Test
    public void testMultipleInputFiles () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                "-i", "src/test/resources/wiki" });
        assertEquals("Added or updated 18 files.", outputStream.toString());
    }


    @Test
    public void testAdding () throws IOException {
        Indexer.main(new String[] {
                "-c", "src/test/resources/krill.properties",
                "-i", "src/test/resources/bzk",
                "-a" });
        logger.info(outputStream.toString());
        assertEquals(outputStream.toString(), "Added 1 file.");
    }

    
    @Test
    public void testMultipleInputDirectories () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                "-i",
                "src/test/resources/bzk;src/test/resources/goe;src/test/resources/sgbr",
                "-o", "test-index" });
        assertEquals("Added or updated 5 files.", outputStream.toString());
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
                "-o test-index" });
        logger.info(outputStream.toString());
        assertEquals(true, outputStream.toString().startsWith(info));
    }
    
    @Test
    public void testMissingInput () throws IOException {
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                "-o", "test-index" });
        logger.info(outputStream.toString());
        assertEquals(true, outputStream.toString().startsWith(info));
    }

    @Before
    public void setOutputStream () {
        System.setOut(new PrintStream(outputStream));
    }

    @After
    public void cleanOutputStream () {
        System.setOut(null);
    }

    @Before
    public void cleanOutputDirectory () {

        if (outputDirectory.exists()) {
            logger.debug("Output directory exists");
            deleteFile(outputDirectory);
        }
    }

    private void deleteFile (File path) {
        if (path.isDirectory()) {
            File file;
            for (String filename : path.list()) {
                file = new File(path + "/" + filename);
                deleteFile(file);
                logger.debug(file.getAbsolutePath());
            }
        }
        path.delete();
    }
}
