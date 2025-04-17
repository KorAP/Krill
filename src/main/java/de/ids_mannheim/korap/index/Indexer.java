package de.ids_mannheim.korap.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.store.MMapDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.util.KrillProperties;

import static com.fasterxml.jackson.core.StreamReadConstraints.DEFAULT_MAX_STRING_LEN;

/**
 * Standalone indexer tool for Krill.
 * Although the preferred index method
 * is using the standalone server system,
 * this tool may be more suitable for your needs
 * (especially as it is way faster).
 * <br><br>
 * Input directories should contain files in the json.gz format. Files
 * of other formats will be skipped or not indexed. The output
 * directory can be specified in the config file. See
 * src/main/resources/krill.properties.info to create a config file.
 * 
 * <pre>
 * Usage:
 * 
 * java -jar Krill-Indexer.jar -c [propfile] -i [input directories] -o
 * [output directory]
 * 
 * java -jar Krill-Indexer.jar --config [propfile] --input [input
 * directories] --output [output directory]
 * </pre>
 * 
 * 
 * @author diewald, margaretha
 *
 */
public class Indexer {
    private KrillIndex index;
    private int count;
    private int commitCount;

    private static String path = null;
    private static boolean addInsteadOfUpsert = false;
    private Pattern jsonFilePattern;

    // Init logger
    private final static Logger log = LoggerFactory.getLogger(Indexer.class);
    private static final boolean DEBUG = false;

    /**
     * Construct a new indexer object.
     * 
     * @param prop
     *            A {@link Properties} object.
     * @throws IOException
     */
    public Indexer (Properties prop) throws IOException {
        if (path == null) {
            path = prop.getProperty("krill.indexDir");
        }

        log.info("Output directory: " + path);

        // Default to 1000 documents till the next commit
        String commitCount = prop.getProperty("krill.index.commit.count",
                "1000");

        // Create a new index object based on the directory
        this.index = new KrillIndex(new MMapDirectory(Paths.get(path)));
        this.count = 0;
        this.commitCount = Integer.parseInt(commitCount);

        jsonFilePattern = Pattern.compile(".*\\.json\\.gz$");
    }


    /**
     * Parse a directory for document files.
     * 
     * @param dir
     *            The {@link File} directory containing
     *            documents to index.
     */
    private void parse (File dir) {
        Matcher matcher;
        for (String file : dir.list()) {
            matcher = jsonFilePattern.matcher(file);
            if (matcher.find()) {
                file = dir.getPath() + '/' + file;

                try {
                    if (addInsteadOfUpsert) {
                        log.info("{} Add {} to the index. ", this.count, file);
                        if (this.index.addDoc(new FileInputStream(file),
                                              true) == null) {
                            log.warn("fail.");
                            continue;
                        }
                    }
                    else {
                        log.info("{} Add or update {} to the index. ", this.count, file);
                        if (this.index.upsertDoc(new FileInputStream(file),
                                                 true) == null) {
                            log.warn("fail.");
                            continue;
                        };
                    };
                    this.count++;
                    if (DEBUG){
                        log.debug("Finished adding files. (" + count + ").");
                    }

                    // Commit in case the commit count is reached
                    if ((this.count % this.commitCount) == 0) {

                        // This will be done in addition to the
                        // autocommit initiated by KrillIndex
                        this.commit();
                    }
                }
                catch (FileNotFoundException e) {
                    log.error("File " + file + " is not found!");
                }
            }
            else {
                log.warn("Skip " + file
                        + " since it does not have json.gz format.");
            }
        }
    }


    /**
     * Commit changes to the index.
     */
    private void commit () {
        log.info("Committing index ... ");
        try {
            this.index.commit();
        }
        catch (IOException e) {
            log.error("Unable to commit to index " + path);
        }
    }

    private void closeIndex() throws IOException {
        this.commit();
        this.index.closeReader();
        this.index.closeWriter();
    }

    /**
     * Main method.
     * 
     * @param argv
     *            Argument list,
     *            expecting the properties file
     *            and a list of directories
     * @throws IOException
     */
    public static void main (String[] argv) {
        
        Options options = new Options();
        options.addOption(Option.builder("c").longOpt("config")
                .desc("configuration file (defaults to "
                        + KrillProperties.DEFAULT_PROPERTIES_LOCATION
                        + ").")
                .hasArg().argName("properties file").required().build());
        options.addOption(Option.builder("i").longOpt("inputDir")
                .desc("input directories separated by semicolons. The input files "
                        + "have to be in <filename>.json.gz format. ")
                .hasArgs().argName("input directories").required()
                .valueSeparator(Character.valueOf(';')).build());
        options.addOption(Option.builder("o").longOpt("outputDir")
                .desc("index output directory (defaults to "
                        + "krill.indexDir in the configuration.")
                .hasArg().argName("output directory").build());
        options.addOption(Option.builder("a").longOpt("addInsteadofUpsert")
                .desc("Always add files to the index, never update")
                .build());

        CommandLineParser parser = new DefaultParser();

        String propFile = null;
        String[] inputDirectories = null;
        try {
            CommandLine cmd = parser.parse(options, argv);
            log.info("Configuration file: " + cmd.getOptionValue("c"));
            propFile = cmd.getOptionValue("c");
            log.info("Input directories: "
                    + StringUtils.join(cmd.getOptionValues("i"), ";"));
            inputDirectories = cmd.getOptionValues("i");

            if (cmd.hasOption("o")) {
                log.info("Output directory: " + cmd.getOptionValue("o"));
                path = cmd.getOptionValue("o");
            }

            if (cmd.hasOption("a")) {
                addInsteadOfUpsert = true;
            };
        }
        catch (MissingOptionException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    "Krill indexer\n java -jar -c <properties file> -i <input directories> "
                            + "[-o <output directory> -a]",
                    options);
            return;
        }
        catch (ParseException e) {
            log.error("Unexpected error: " + e);
            e.printStackTrace();
        }

        // Load properties
        Properties prop = KrillProperties.loadProperties(propFile);

        try {
            // Get indexer object
            Indexer indexer = new Indexer(prop);
            
            // Apply max text size from configuration
            if (KrillProperties.maxTextSize > DEFAULT_MAX_STRING_LEN) {
                log.info("Setting max text length to " + KrillProperties.maxTextSize);
                indexer.index.setMaxStringLength(KrillProperties.maxTextSize);
            }

            // Iterate over list of directories
            for (String arg : inputDirectories) {
                log.info("Indexing files in " + arg);
                File f = new File(arg);
                if (f.isDirectory())
                    indexer.parse(f);
            }
            indexer.closeIndex();

            // Final commit
            log.info("Finished indexing.");
            // Finish indexing
            String message = "Added ";
            if (!addInsteadOfUpsert)
                message += "or updated ";
            message += indexer.count + " file";
            if (indexer.count > 1) {
                message += "s";
            }
            System.out.println(message + ".");
        }

        catch (IOException e) {
            log.error("Unexpected error: " + e);
            e.printStackTrace();
        }
    }
}
