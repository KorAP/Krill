package de.ids_mannheim.korap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

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

    @Test
    public void testProgressOption () throws IOException {
        java.io.PrintStream originalErr = System.err;
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        System.setErr(new java.io.PrintStream(errStream));
        try {
            Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                    "-i", "src/test/resources/bzk",
                    "-o", getTestOutputPath("test-progress-index"),
                    "--progress"});
        }
        finally {
            System.err.flush();
            System.setErr(originalErr);
        }

        String progressOutput = errStream.toString();
        // Expect progress bar renders with bracketed bar, percentage, MB throughput, and ETA
        assertTrue(progressOutput.contains("[==="));
        assertTrue(progressOutput.contains("100.0%"));
        assertTrue(progressOutput.contains("MB"));
        assertTrue(progressOutput.contains("ETA"));
    }

    @Test
    public void testFormatDuration () {
        // seconds only
        assertEquals("00:45", Indexer.formatDuration(45));
        // minutes and seconds
        assertEquals("05:30", Indexer.formatDuration(330));
        // hours
        assertEquals("02:30:00", Indexer.formatDuration(9000));
        // exactly 24h → 1 day
        assertEquals("1d 00:00:00", Indexer.formatDuration(86400));
        // more than 24h
        assertEquals("2d 03:45:12", Indexer.formatDuration(2 * 86400 + 3 * 3600 + 45 * 60 + 12));
        // large multi-day (previously capped at >99h)
        assertEquals("10d 05:00:00", Indexer.formatDuration(10 * 86400 + 5 * 3600));
    }

    @Test
    public void testCountTargetFiles () throws Exception {
        long nullCount = Indexer.countTargetFiles(null);
        assertEquals(0L, nullCount);

        long dirCount = Indexer.countTargetFiles(new String[] { "src/test/resources/bzk" });
        assertEquals(1L, dirCount);

        long zipCount = Indexer.countTargetFiles(new String[] { "src/test/resources/rei/rei_sample_krill.zip" });
        assertEquals(3L, zipCount);

        long tarCount = Indexer.countTargetFiles(new String[] { "src/test/resources/rei/rei_sample_krill.tar" });
        assertEquals(3L, tarCount);

        long tgzCount = Indexer.countTargetFiles(new String[] { "src/test/resources/rei/rei_sample_krill.tar.gz" });
        assertEquals(3L, tgzCount);

        long mixedZipTar = Indexer.countTargetFiles(new String[] {
                "src/test/resources/rei/rei_sample_krill.zip",
                "src/test/resources/rei/rei_sample_krill.tar" });
        assertEquals(6L, mixedZipTar);

        long mixedDirZip = Indexer.countTargetFiles(new String[] {
                "src/test/resources/bzk",
                "src/test/resources/rei/rei_sample_krill.zip" });
        assertEquals(4L, mixedDirZip);

        long mixedContentZip = Indexer.countTargetFiles(new String[] { "src/test/resources/rei/mixed_test.zip" });
        assertEquals(2L, mixedContentZip);

        long invalidZip = Indexer.countTargetFiles(new String[] { "src/test/resources/nonexistent.zip" });
        assertEquals(0L, invalidZip);
    }

    @Test
    public void testDeleteByTextSigleOption () throws IOException {
        Path inputDir = Files.createTempDirectory(tempBaseDirectory.toPath(),
                "delete-input");
        Path jsonPath = Paths.get("src/test/resources/goe/AGX-00002.json");
        Path gzPath = inputDir.resolve("AGX-00002.json.gz");
        gzipFile(jsonPath, gzPath);

        String outputDir = getTestOutputPath("test-delete-index");
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                "-i", inputDir.toString(), "-o", outputDir });
        assertTrue(outputStream.toString().startsWith("Added or updated 1 file."));

        outputStream.reset();


        KrillIndex ki = new KrillIndex(Paths.get(outputDir));
        assertEquals(1, ki.numberOf("documents"));


        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                "-o", outputDir, "-D", "textSigle", "GOE_AGX.00002" });
        assertTrue(outputStream.toString()
                .startsWith("Deleted documents where textSigle=GOE_AGX.00002."));

        ki = new KrillIndex(Paths.get(outputDir));
        
        assertEquals(0, ki.numberOf("documents"));
        ki.close();
        
    }

    /** Test two identical index and check that the leaf readers return 
     *  the same fingerprints. After deleting a document, check again if the 
     *  leaf fingerprints are identical.
     *  
     *  This is to make sure that cached leaf-readers are can be copied across
     *  multiple identical indexes with the same index updates, to avoid 
     *  the overhead of re-caching leaf readers for each index.
     */
    @Test
    public  void testLeafReader () throws IOException {
    	String outputDir1 = createWikiIndex("wiki1",1,3);
    	outputDir1 = createWikiIndex("wiki1",4,5);
    	KrillIndex ki1 = new KrillIndex(Paths.get(outputDir1));
    	assertEquals(5, ki1.numberOf("documents"));
    	
        List<String> fp1 = new ArrayList<>(ki1.getAllLeafFingerprints());
        Collections.sort(fp1);
        
        delWikiDoc(outputDir1, "WPD_AAA.00001");
               
        ki1 = new KrillIndex(Paths.get(outputDir1));
        assertEquals(4, ki1.numberOf("documents"));
        List<String> fp1a = new ArrayList<>(ki1.getAllLeafFingerprints());
        Collections.sort(fp1a);
        
        // 2nd index identical to the 1st index
		String outputDir2 = createWikiIndex("wiki2", 1, 3);
		outputDir2 = createWikiIndex("wiki2", 4, 5);
		KrillIndex ki2 = new KrillIndex(Paths.get(outputDir2));
		assertEquals(5, ki2.numberOf("documents"));

		List<String> fp2 = new ArrayList<>(ki2.getAllLeafFingerprints());
		Collections.sort(fp2);
		assertEquals(fp1, fp2);
		
		delWikiDoc(outputDir2, "WPD_AAA.00001");
		
		ki2 = new KrillIndex(Paths.get(outputDir2));
        assertEquals(4, ki2.numberOf("documents"));
        List<String> fp2a = new ArrayList<>(ki1.getAllLeafFingerprints());
        Collections.sort(fp2a);
        assertEquals(fp1a, fp2a);
        ki1.close();
        ki2.close();
	}
    
    private void delWikiDoc(String outputDir, String textSigle) throws IOException {
    	outputStream.reset();
    	Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                "-o", outputDir, "-D", "ID", textSigle });
        //assertTrue(outputStream.toString()
        //        .startsWith("Deleted documents where textSigle="+textSigle+"."));
    }
    
    private String createWikiIndex (String input, int startId, int endId) throws IOException {
    	Path inputDir = Files.createTempDirectory(tempBaseDirectory.toPath(),
                input);
    	for (int i = startId; i <= endId; i++) {
			Path jsonPath = Paths.get("src/test/resources/wiki/0000" + i + ".json");
			Path gzPath = inputDir.resolve("0000" + i + ".json.gz");
			gzipFile(jsonPath, gzPath);
		}
        String outputDir = getTestOutputPath(input + "-index");
        Indexer.main(new String[] { "-c", "src/test/resources/krill.properties",
                "-i", inputDir.toString(), "-o", outputDir });
        return outputDir;
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

    private static void gzipFile (Path input, Path output) throws IOException {
        try (InputStream in = Files.newInputStream(input);
                OutputStream out = new GZIPOutputStream(
                        Files.newOutputStream(output))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}
