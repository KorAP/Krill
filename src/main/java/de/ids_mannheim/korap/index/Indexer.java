package de.ids_mannheim.korap.index;
import java.util.*;
import java.io.*;
import org.apache.lucene.store.MMapDirectory;
import de.ids_mannheim.korap.KrillIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a runnable indexer tool for
 * Krill. Although the preferred index method
 * is using the standalone server system,
 * this tool may be more suitable for your needs
 * (especially as it is way faster).
 *
 * Usage: java -jar Krill-X.XX.jar [propfile] [directories]*
 */
public class Indexer {
    KrillIndex index;
    String indexDir;
    int count;
    int commitCount;

    // Init logger
    private final static Logger log =
        LoggerFactory.getLogger(KrillIndex.class);


    /**
     * Construct a new indexer object.
     *
     * @param prop A {@link Properties} object with
     *        at least the following information:
     *        <tt>lucene.indexDir</tt>.
     * @throws IOException
     */
    public Indexer (Properties prop) throws IOException {
        this.indexDir = prop.getProperty("lucene.indexDir");

        System.out.println("Index to " + this.indexDir);
	
        // Default to 1000 documents till the next commit
        String commitCount = prop.getProperty("lucene.index.commit.count", "1000");

        // Create a new index object based on the directory
        this.index = new KrillIndex(new MMapDirectory(new File(indexDir)));
        this.count = 0;
        this.commitCount = Integer.parseInt(commitCount);
    };

    /**
     * Parse a directory for document files.
     *
     * @param dir The {@link File} directory containing
     * documents to index.
     */
    public void parse (File dir) {
        for (String file : dir.list()) {
            if (file.matches("^[^\\.].+?\\.json\\.gz$")) {
                String found = dir.getPath() + '/' + file;
                System.out.print("  Index " + found + " ... ");

                // Add file to the index
                if (this.index.addDocFile(found, true) == null) {
                    System.out.println("fail.");
                    continue;
                };
                System.out.println("done (" + count + ").");
                this.count++;

                // Commit in case the commit count is reached
                if ((this.count % this.commitCount) == 0)
                    this.commit();
            };
        };
    };

    /**
     * Commit changes to the index.
     */
    public void commit () {
        System.out.println("-----");
        System.out.print("  Commit ... ");
        try {
            this.index.commit();
        }
        catch (IOException e) {
            System.err.println("Unable to commit to index " + this.indexDir);
        };
        System.out.println("done.");
    };

    /**
     * Main method.
     *
     * @param argv Argument list,
     *        expecting the properties file
     *        and a list of directories
     * @throws IOException
     */
    public static void main (String[] argv) throws IOException {
        Properties prop = new Properties();

        // Needed at least 2 parameters
        if (argv.length < 2) {

            String jar = new File(Indexer.class.getProtectionDomain()
                                  .getCodeSource()
                                  .getLocation()
                                  .getPath()).getName();
            System.out.println("Usage: java -jar " + jar +
                               " [propfile] [directories]*");
            return;
        };

        // Load properties
        InputStream fr = new FileInputStream(argv[0]);
        prop.load(fr);

        // Get indexer object
        Indexer ki = new Indexer(prop);

        // Empty line
        System.out.println();

        // Iterate over list of directories
        for (String arg :  Arrays.copyOfRange(argv, 1, argv.length)) {
            File f = new File(arg);
            if (f.isDirectory())
                ki.parse(f);
        };

        // Final commit
        ki.commit();

        // Finish indexing
        System.out.println("-----");
        System.out.println("  Indexed " + ki.count + " files.");
        System.out.println();
    };
};
