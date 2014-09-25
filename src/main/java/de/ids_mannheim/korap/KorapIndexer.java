package de.ids_mannheim.korap;
import java.util.*;
import java.io.*;
import org.apache.lucene.store.MMapDirectory;
import de.ids_mannheim.korap.KorapIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KorapIndexer {
    KorapIndex index;
    String indexDir;
    int count;
    int commitCount;

    // Init logger
    private final static Logger log = LoggerFactory.getLogger(KorapIndexer.class);

    public KorapIndexer(Properties prop) throws IOException {
        this.indexDir = prop.getProperty("lucene.indexDir");

	System.out.println("Index to " + this.indexDir);
	
        String commitCount = prop.getProperty("lucene.index.commit.count", "1000");

	this.index = new KorapIndex(new MMapDirectory(new File(indexDir)));
        this.count = 0;
        this.commitCount = Integer.parseInt(commitCount);
    };

    
    public void parse (File dir) {
	for (String file : dir.list()) {
	    if (file.matches("^[^\\.].+?\\.json\\.gz$")) {
		String found = dir.getPath() + '/' + file;
		System.out.print("  Index " + found + " ... ");
		if (this.index.addDocFile(found, true) == null) {
		    System.out.println("fail.");
		    continue;
		};
		System.out.println("done (" + count + ").");
		this.count++;

		if ((this.count % this.commitCount) == 0)
		    this.commit();
	    };
	};
    };


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



    public static void main (String[] argv) throws IOException {
	Properties prop = new Properties();
	InputStream fr = new FileInputStream(argv[0]);
	prop.load(fr);
	KorapIndexer ki = new KorapIndexer(prop);
	System.out.println();

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
