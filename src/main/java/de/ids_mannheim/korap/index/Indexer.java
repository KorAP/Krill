package de.ids_mannheim.korap.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Locale;
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
    private boolean progressEnabled = false;
    private SimpleProgressBar progressBar;

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

    private void initProgress (long total) {
        this.progressEnabled = true;
        this.progressBar = new SimpleProgressBar(total);
        this.progressBar.start();
    }

    private void initProgressIndeterminate () {
        this.progressEnabled = true;
        this.progressBar = new SimpleProgressBar(0);
        this.progressBar.start();
    }

    private void stepProgress (long bytes) {
        if (this.progressEnabled && this.progressBar != null) {
            this.progressBar.addBytes(bytes);
        }
    }

    private void finishProgress () {
        if (this.progressEnabled && this.progressBar != null) {
            this.progressBar.finish();
        }
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
                        if (!progressEnabled)
                            log.info("{} Add {} to the index. ", this.count, file);
                        if (this.index.addDoc(new FileInputStream(file),
                                              true) == null) {
                            log.warn("fail.");
                            continue;
                        }
                    }
                    else {
                        if (!progressEnabled)
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
                    this.stepProgress(new File(file).length());
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
            CountingInputStream countingStream = new CountingInputStream(new FileInputStream(tarFile));
            InputStream fileInputStream = countingStream;
            long prevCompressedBytes = 0;

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
                                if (!progressEnabled)
                                    log.info("{} Add {} from tar {} to the index. ", 
                                            this.count, entryName, tarFile.getName());
                                if (this.index.addDoc(entryStream, isGzipped) == null) {
                                    log.warn("fail.");
                                    continue;
                                }
                            }
                            else {
                                if (!progressEnabled)
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
                            long nowBytes = countingStream.getBytesRead();
                            this.stepProgress(nowBytes - prevCompressedBytes);
                            prevCompressedBytes = nowBytes;
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
                            if (!progressEnabled)
                                log.info("{} Add {} from zip {} to the index. ", 
                                        this.count, entryName, zipFile.getName());
                            if (this.index.addDoc(entryStream, isGzipped) == null) {
                                log.warn("fail.");
                                continue;
                            }
                        }
                        else {
                            if (!progressEnabled)
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
                        long compSize = entry.getCompressedSize();
                        this.stepProgress(compSize > 0 ? compSize : entry.getSize());
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
                .hasArgs().argName("input paths")
                .valueSeparator(Character.valueOf(';')).build());
        options.addOption(Option.builder("o").longOpt("outputDir")
                .desc("index output directory (defaults to "
                        + "krill.indexDir in the configuration.")
                .hasArg().argName("output directory").build());
        options.addOption(Option.builder("D").longOpt("delete")
                .desc("delete documents from the index by field and value "
                        + "(example: -D textSigle GOE/AGX/00002).")
                .numberOfArgs(2).argName("field value").build());
        options.addOption(Option.builder("a").longOpt("addInsteadofUpsert")
                .desc("Always add files to the index, never update")
                .build());
        options.addOption(Option.builder().longOpt("progress")
                .desc("Show progress bar with ETA")
                .build());

        CommandLineParser parser = new DefaultParser();

        String propFile = null;
        String[] inputPaths = null;
        String deleteField = null;
        String deleteValue = null;
        boolean showProgress = false;
        try {
            CommandLine cmd = parser.parse(options, argv);
            log.info("Configuration file: " + cmd.getOptionValue("c"));
            propFile = cmd.getOptionValue("c");

            if (cmd.hasOption("i")) {
                inputPaths = cmd.getOptionValues("i");
                log.info("Input paths: " + StringUtils.join(inputPaths, ";"));
            }

            if (cmd.hasOption("o")) {
                log.info("Output directory: " + cmd.getOptionValue("o"));
                path = cmd.getOptionValue("o");
            }

            if (cmd.hasOption("D")) {
                String[] deleteArgs = cmd.getOptionValues("D");
                deleteField = deleteArgs[0];
                deleteValue = deleteArgs[1];
                log.info("Delete documents with {}={}", deleteField, deleteValue);
            }

            if (inputPaths == null && !cmd.hasOption("D")) {
                throw new MissingOptionException(
                        "Missing required option: either -i/--input or -D/--delete");
            }

            if (cmd.hasOption("a")) {
                addInsteadOfUpsert = true;
            };
            if (cmd.hasOption("progress")) {
                showProgress = true;
            }
        }
        catch (MissingOptionException e) {
            HelpFormatter formatter = new HelpFormatter();
            String helpSyntax = "Krill indexer\n java -jar Krill-Indexer.jar -c <properties file> "
                    + "[-i <input paths>] [-D <field> <value>] "
                    + "[-o <output directory> -a --progress]";
            formatter.printHelp(
                    helpSyntax,
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

            // Initialize progress bar; total is computed instantly from compressed file sizes
            if (showProgress) {
                long totalBytes = computeTotalBytes(inputPaths);
                indexer.initProgress(totalBytes);
            }

            // Iterate over list of input paths (auto-detect directories vs zip/tar files)
            if (inputPaths != null) {
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
            }

            if (deleteField != null && deleteValue != null) {
                indexer.index.delDocs(deleteField, deleteValue);
            }

            indexer.finishProgress();
            indexer.closeIndex();

            // Final commit
            log.info("Finished indexing.");
            // Finish indexing/deletion
            if (inputPaths != null) {
                String message = "Added ";
                if (!addInsteadOfUpsert)
                    message += "or updated ";
                message += indexer.count + " file";
                if (indexer.count > 1) {
                    message += "s";
                }
                System.out.println(message + ".");
            }
            else {
                System.out.println("Deleted documents where " + deleteField + "="
                        + deleteValue + ".");
            }
        }

        catch (IOException e) {
            log.error("Unexpected error: " + e);
            e.printStackTrace();
        }
    }

    public static long countTargetFiles (String[] inputPaths) {
        if (inputPaths == null)
            return 0;
        Pattern gzPattern = Pattern.compile(".*\\.json\\.gz$");
        Pattern jsonPattern = Pattern.compile(".*\\.json$");
        long total = 0L;
        for (String arg : inputPaths) {
            File f = new File(arg);
            if (f.isDirectory()) {
                String[] list = f.list();
                if (list != null) {
                    for (String name : list) {
                        if (gzPattern.matcher(name).find())
                            total++;
                    }
                }
            }
            else if (f.isFile() && f.getName().toLowerCase().endsWith(".zip")) {
                try (ZipFile zip = new ZipFile(f)) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (!entry.isDirectory()) {
                            String entryName = entry.getName();
                            if (gzPattern.matcher(entryName).find() ||
                                jsonPattern.matcher(entryName).find()) {
                                total++;
                            }
                        }
                    }
                }
                catch (IOException e) {
                    log.warn("Unable to count entries in zip " + arg, e);
                }
            }
            else if (f.isFile() && (f.getName().toLowerCase().endsWith(".tar") ||
                                    f.getName().toLowerCase().endsWith(".tar.gz") ||
                                    f.getName().toLowerCase().endsWith(".tgz"))) {
                try (InputStream fis = new FileInputStream(f);
                     InputStream in = (f.getName().toLowerCase().endsWith(".tar.gz") || f.getName().toLowerCase().endsWith(".tgz"))
                             ? new GzipCompressorInputStream(fis)
                             : fis;
                     TarArchiveInputStream tis = new TarArchiveInputStream(in)) {
                    TarArchiveEntry entry;
                    while ((entry = tis.getNextTarEntry()) != null) {
                        if (!entry.isDirectory()) {
                            String entryName = entry.getName();
                            if (gzPattern.matcher(entryName).find() ||
                                jsonPattern.matcher(entryName).find()) {
                                total++;
                            }
                        }
                    }
                }
                catch (IOException e) {
                    log.warn("Unable to count entries in tar " + arg, e);
                }
            }
        }
        return total;
    }

    /**
     * Compute total compressed bytes for a set of input paths (instant: uses file sizes only).
     * For directories, sums the sizes of all .json.gz files.
     * For archive files (zip, tar, tar.gz), uses the file size directly.
     */
    public static long computeTotalBytes (String[] inputPaths) {
        if (inputPaths == null) return 0;
        Pattern gzPattern = Pattern.compile(".*\\.json\\.gz$");
        long total = 0L;
        for (String arg : inputPaths) {
            File f = new File(arg);
            if (f.isDirectory()) {
                String[] list = f.list();
                if (list != null) {
                    for (String name : list) {
                        if (gzPattern.matcher(name).find())
                            total += new File(f, name).length();
                    }
                }
            }
            else if (f.isFile()) {
                total += f.length();
            }
        }
        return total;
    }

    /**
     * Format a duration in seconds into a human-readable string.
     * Durations under 1 hour display as MM:SS,
     * durations under 1 day display as HH:MM:SS,
     * and longer durations display as Xd HH:MM:SS.
     *
     * @param seconds duration in seconds
     * @return formatted duration string
     */
    public static String formatDuration (long seconds) {
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (d > 0)
            return String.format(Locale.US, "%dd %02d:%02d:%02d", d, h, m, s);
        if (h > 0)
            return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
        else
            return String.format(Locale.US, "%02d:%02d", m, s);
    }

    // Minimal counting wrapper to track compressed bytes read from a stream
    private static class CountingInputStream extends InputStream {
        private final InputStream wrapped;
        private volatile long bytesRead = 0;

        CountingInputStream (InputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int read () throws IOException {
            int b = wrapped.read();
            if (b != -1) bytesRead++;
            return b;
        }

        @Override
        public int read (byte[] buf, int off, int len) throws IOException {
            int n = wrapped.read(buf, off, len);
            if (n > 0) bytesRead += n;
            return n;
        }

        long getBytesRead () { return bytesRead; }
    }

    // Simple console progress bar with ETA; supports indeterminate mode until total is known
    private static class SimpleProgressBar implements Runnable {
        private volatile long total; // 0 means indeterminate
        private volatile long current = 0;
        private volatile boolean running = false;
        private volatile boolean finished = false;
        private final long startTimeMs;
        private final int barWidth = 40;
        private final Thread thread;
        private int slidePos = 0;
        private int slideDir = 1; // 1 right, -1 left

        SimpleProgressBar (long total) {
            this.total = total;
            this.startTimeMs = System.currentTimeMillis();
            this.thread = new Thread(this, "krill-progress-bar");
            this.thread.setDaemon(true);
        }

        void start () {
            running = true;
            thread.start();
        }

        void addBytes (long bytes) {
            current += bytes;
        }

        void setTotal (long total) {
            if (total < 0) total = 0;
            this.total = total;
        }

        void finish () {
            finished = true;
            running = false;
            try {
                thread.join(500);
            }
            catch (InterruptedException e) {
                // ignore
            }
            // Final render as completed line if determinate
            if (total > 0 && current < total) {
                current = total;
            }
            render();
            System.err.println();
        }

        @Override
        public void run () {
            // periodic render loop
            while (running && !finished) {
                render();
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        private void render () {
            if (total <= 0) {
                // indeterminate: sliding bar
                slidePos += slideDir;
                if (slidePos >= barWidth - 5) {
                    slideDir = -1;
                }
                else if (slidePos <= 0) {
                    slideDir = 1;
                }
                StringBuilder bar = new StringBuilder(barWidth);
                for (int i = 0; i < barWidth; i++) bar.append('-');
                // draw a 5-char slider
                for (int i = slidePos; i < Math.min(slidePos + 5, barWidth); i++) {
                    bar.setCharAt(i, '=');
                }
                long now = System.currentTimeMillis();
                double elapsedSec = (now - startTimeMs) / 1000.0;
                double rateMBs = elapsedSec > 0 ? current / 1_000_000.0 / elapsedSec : 0.0;
                String rateStr = rateMBs > 0 ? String.format(Locale.US, "%.2f MB/s", rateMBs) : "NA";
                String line = String.format(Locale.US, "\r[%s]   %.1f MB processed | %s | ETA calculating...", bar, current / 1_000_000.0, rateStr);
                System.err.print(line);
                return;
            }

            double percent = (double) current / (double) Math.max(total, 1);
            int filled = (int) Math.round(percent * barWidth);
            StringBuilder bar = new StringBuilder(barWidth);
            for (int i = 0; i < barWidth; i++) {
                bar.append(i < filled ? '=' : '-');
            }

            long now = System.currentTimeMillis();
            double elapsedSec = (now - startTimeMs) / 1000.0;
            double rateBytesPerSec = elapsedSec > 0 ? current / elapsedSec : 0.0;
            long etaSec = (rateBytesPerSec > 0 && total > current) ? (long) Math.ceil((total - current) / rateBytesPerSec) : 0;

            String etaStr = (rateBytesPerSec > 0) ? Indexer.formatDuration(etaSec) : "NA";
            String pctStr = String.format(Locale.US, "%5.1f%%", percent * 100.0);
            String rateStr = String.format(Locale.US, "%.2f MB/s", rateBytesPerSec / 1_000_000.0);
            double processedMB = current / 1_000_000.0;
            double totalMB = total / 1_000_000.0;

            String line = String.format(Locale.US, "\r[%s] %s %.1f/%.1f MB | %s | ETA %s", bar, pctStr, processedMB, totalMB, rateStr, etaStr);
            System.err.print(line);
        }
    }
}
