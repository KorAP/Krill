package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;
import org.apache.lucene.store.MMapDirectory;
import de.ids_mannheim.korap.KrillIndex;
import static de.ids_mannheim.korap.util.KrillProperties.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Paths;

/**
 * Standalone indexer tool for Krill.
 * Although the preferred index method
 * is using the standalone server system,
 * this tool may be more suitable for your needs
 * (especially as it is way faster).
 * 
 * Usage: java -jar Krill-Indexer.jar [--config propfile] [directories]*
 */
public class Indexer {
    KrillIndex index;
    int count;
    int commitCount;

    // private static String propFile = "krill.properties";
    private static String path = null;

    // Init logger
    private final static Logger log = LoggerFactory.getLogger(KrillIndex.class);


    /**
     * Construct a new indexer object.
     * 
     * @param prop
     *            A {@link Properties} object.
     * @throws IOException
     */
    public Indexer (Properties prop) throws IOException {
        if (this.path == null) {
            this.path = prop.getProperty("krill.indexDir");
        };

        System.out.println("Index to " + this.path);

        // Default to 1000 documents till the next commit
        String commitCount = prop.getProperty("krill.index.commit.count",
                "1000");

        // Create a new index object based on the directory
        this.index = new KrillIndex(new MMapDirectory(Paths.get(this.path)));
        this.count = 0;
        this.commitCount = Integer.parseInt(commitCount);
    };


    /**
     * Parse a directory for document files.
     * 
     * @param dir
     *            The {@link File} directory containing
     *            documents to index.
     */
    public void parse (File dir) {
        for (String file : dir.list()) {
            if (file.matches("^[^\\.].+?\\.json\\.gz$")) {
                String found = dir.getPath() + '/' + file;
                System.out.print("  Index " + found + " ... ");

                // Add file to the index
                try {
                    if (this.index.addDoc(new FileInputStream(found), true) == null) {
                        System.out.println("fail.");
                        continue;
                    };
                    System.out.println("done (" + count + ").");
                    this.count++;

                    // Commit in case the commit count is reached
                    if ((this.count % this.commitCount) == 0)
                        this.commit();
                }
                catch (FileNotFoundException e) {
                    System.out.println("not found!");
                };
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
            System.err.println("Unable to commit to index " + this.path);
        };
        System.out.println("done.");
    };


    /**
     * Main method.
     * 
     * @param argv
     *            Argument list,
     *            expecting the properties file
     *            and a list of directories
     * @throws IOException
     */
    public static void main (String[] argv) throws IOException {

        if (argv.length == 0) {
            String jar = new File(Indexer.class.getProtectionDomain()
                                  .getCodeSource().getLocation().getPath()).getName();
            
            System.out.println("Add documents from a directory to the Krill index.");
            System.out.println("Usage: java -jar " + jar
                               + " [--config propfile] [directories]*");
            Syste.out.println();
            System.err.println("  --config|-c    Configuration file");
            System.err.println("                 (defaults to " +
                               de.ids_mannheim.korap.util.KrillProperties.file +
                               ")");
            System.err.println("  --indexDir|-d  Index directory");
            System.err.println("                 (defaults to krill.indexDir"+
                               " in configuration)");
            System.err.println();
            return;
        };

        int i = 0;
        boolean last = false;
        String propFile = null;

        for (i = 0; i < argv.length; i += 2) {
            switch (argv[i]) {
                case "--config":
                case "-cfg":
                case "-c":
                    propFile = argv[i + 1];
                    break;
                case "--indexDir":
                case "-d":
                    path = argv[i + 1];
                    break;
                default:
                    last = true;
                    break;
            };

            if (last)
                break;
        };

        // Load properties
        /*
          InputStream fr = new FileInputStream(argv[0]);
          prop.load(fr);
        */
        Properties prop = loadProperties(propFile);

        // Get indexer object
        Indexer ki = new Indexer(prop);

        // Empty line
        System.out.println();

        // Iterate over list of directories
        for (String arg : Arrays.copyOfRange(argv, i, argv.length)) {
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
