package de.ids_mannheim.korap.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

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
 * Input can be directories containing files in the json.gz format, or
 * zip/tar files containing .json or .json.gz files. The indexer automatically
 * detects whether each input path is a directory, zip file, or tar file. Files
 * of other formats will be skipped or not indexed. The output
 * directory can be specified in the config file. See
 * src/main/resources/krill.properties.info to create a config file.
 * 
 * <pre>
 * Usage:
 * 
 * java -jar Krill-Indexer.jar -c [propfile] -i [input paths] -o
 * [output directory]
 * 
 * java -jar Krill-Indexer.jar --config [propfile] --input [input paths]
 * --output [output directory]
 * 
 * Input paths can be:
 * - Directories containing .json.gz files
 * - Zip files containing .json or .json.gz files
 * - Tar files (including .tar.gz) containing .json or .json.gz files
 * - Mix of any of the above, separated by semicolons
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
    private Pattern plainJsonFilePattern;

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
        plainJsonFilePattern = Pattern.compile(".*\\.json$");
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
     * Parse a tar file for document files.
     * 
     * @param tarFile
     *            The {@link File} tar file containing
     *            JSON documents (plain .json or gzipped .json.gz) to index.
     */
    private void parseTar (File tarFile) {
        try {
            InputStream fileInputStream = new FileInputStream(tarFile);
            
            // Check if it's a gzipped tar file
            if (tarFile.getName().toLowerCase().endsWith(".tar.gz") || 
                tarFile.getName().toLowerCase().endsWith(".tgz")) {
                fileInputStream = new GzipCompressorInputStream(fileInputStream);
            }
            
            try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(fileInputStream)) {
                TarArchiveEntry entry;
                
                while ((entry = tarInputStream.getNextTarEntry()) != null) {
                    // Skip directories
                    if (entry.isDirectory()) {
                        continue;
                    }
                    
                    String entryName = entry.getName();
                    Matcher gzipMatcher = jsonFilePattern.matcher(entryName);
                    Matcher plainMatcher = plainJsonFilePattern.matcher(entryName);
                    
                    boolean isGzipped = gzipMatcher.find();
                    boolean isPlainJson = plainMatcher.find();
                    
                    if (isGzipped || isPlainJson) {
                        // Read the entry content into a byte array to avoid stream closure issues
                        byte[] entryData = new byte[(int) entry.getSize()];
                        int totalRead = 0;
                        while (totalRead < entryData.length) {
                            int bytesRead = tarInputStream.read(entryData, totalRead, entryData.length - totalRead);
                            if (bytesRead == -1) break;
                            totalRead += bytesRead;
                        }
                        
                        try (InputStream entryStream = new java.io.ByteArrayInputStream(entryData)) {
                            if (addInsteadOfUpsert) {
                                log.info("{} Add {} from tar {} to the index. ", 
                                        this.count, entryName, tarFile.getName());
                                if (this.index.addDoc(entryStream, isGzipped) == null) {
                                    log.warn("fail.");
                                    continue;
                                }
                            }
                            else {
                                log.info("{} Add or update {} from tar {} to the index. ", 
                                        this.count, entryName, tarFile.getName());
                                if (this.index.upsertDoc(entryStream, isGzipped) == null) {
                                    log.warn("fail.");
                                    continue;
                                }
                            }
                            
                            this.count++;
                            if (DEBUG) {
                                log.debug("Finished adding files. (" + count + ").");
                            }

                            // Commit in case the commit count is reached
                            if ((this.count % this.commitCount) == 0) {
                                this.commit();
                            }
                        }
                    }
                    else {
                        log.warn("Skip " + entryName + " from tar " + tarFile.getName()
                                + " since it does not have .json or .json.gz format.");
                    }
                }
            }
        }
        catch (IOException e) {
            log.error("Error reading tar file " + tarFile.getName(), e);
        }
    }

    /**
     * Parse a zip file for document files.
     * 
     * @param zipFile
     *            The {@link File} zip file containing
     *            JSON documents (plain .json or gzipped .json.gz) to index.
     */
    private void parseZip (File zipFile) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                
                // Skip directories
                if (entry.isDirectory()) {
                    continue;
                }
                
                String entryName = entry.getName();
                Matcher gzipMatcher = jsonFilePattern.matcher(entryName);
                Matcher plainMatcher = plainJsonFilePattern.matcher(entryName);
                
                boolean isGzipped = gzipMatcher.find();
                boolean isPlainJson = plainMatcher.find();
                
                if (isGzipped || isPlainJson) {
                    try (InputStream entryStream = zip.getInputStream(entry)) {
                        if (addInsteadOfUpsert) {
                            log.info("{} Add {} from zip {} to the index. ", 
                                    this.count, entryName, zipFile.getName());
                            if (this.index.addDoc(entryStream, isGzipped) == null) {
                                log.warn("fail.");
                                continue;
                            }
                        }
                        else {
                            log.info("{} Add or update {} from zip {} to the index. ", 
                                    this.count, entryName, zipFile.getName());
                            if (this.index.upsertDoc(entryStream, isGzipped) == null) {
                                log.warn("fail.");
                                continue;
                            }
                        }
                        
                        this.count++;
                        if (DEBUG) {
                            log.debug("Finished adding files. (" + count + ").");
                        }

                        // Commit in case the commit count is reached
                        if ((this.count % this.commitCount) == 0) {
                            this.commit();
                        }
                    }
                    catch (IOException e) {
                        log.error("Error reading entry " + entryName + " from zip file " + zipFile.getName(), e);
                    }
                }
                else {
                    log.warn("Skip " + entryName + " from zip " + zipFile.getName()
                            + " since it does not have .json or .json.gz format.");
                }
            }
        }
        catch (IOException e) {
            log.error("Error reading zip file " + zipFile.getName(), e);
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
        options.addOption(Option.builder("i").longOpt("input")
                .desc("input paths separated by semicolons. Can be directories containing "
                        + "<filename>.json.gz files, zip files containing .json or .json.gz files, "
                        + "or tar files (including .tar.gz) containing .json or .json.gz files. "
                        + "The indexer will automatically detect the type.")
                .hasArgs().argName("input paths").required()
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
        String[] inputPaths = null;
        try {
            CommandLine cmd = parser.parse(options, argv);
            log.info("Configuration file: " + cmd.getOptionValue("c"));
            propFile = cmd.getOptionValue("c");
            
            log.info("Input paths: "
                    + StringUtils.join(cmd.getOptionValues("i"), ";"));
            inputPaths = cmd.getOptionValues("i");

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
                    "Krill indexer\n java -jar -c <properties file> -i <input paths> "
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

            // Iterate over list of input paths (auto-detect directories vs zip/tar files)
            for (String arg : inputPaths) {
                File f = new File(arg);
                
                if (f.isDirectory()) {
                    log.info("Indexing files in directory " + arg);
                    indexer.parse(f);
                }
                else if (f.isFile() && f.getName().toLowerCase().endsWith(".zip")) {
                    log.info("Indexing files in zip " + arg);
                    indexer.parseZip(f);
                }
                else if (f.isFile() && (f.getName().toLowerCase().endsWith(".tar") || 
                                       f.getName().toLowerCase().endsWith(".tar.gz") ||
                                       f.getName().toLowerCase().endsWith(".tgz"))) {
                    log.info("Indexing files in tar " + arg);
                    indexer.parseTar(f);
                }
                else {
                    log.warn("Skipping " + arg + " - not a valid directory, zip file, or tar file");
                }
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
