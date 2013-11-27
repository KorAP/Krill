package de.ids_mannheim.korap;
import java.util.*;
import java.io.*;
import org.apache.lucene.store.MMapDirectory;
import de.ids_mannheim.korap.KorapIndex;

public class KorapIndexer {
    KorapIndex index;
    int count;

    public KorapIndexer () throws IOException {
	this.index = new KorapIndex(new MMapDirectory(new File("target/test/")));
	this.count = 0;
    };

    public void parse (File dir) {
	for (String file : dir.list()) {
	    if (file.matches("^[^\\.].+?\\.json\\.gz$")) {
		String found = dir.getPath() + '/' + file;
		System.out.print("  Index " + found + " ... ");
		try {
		    this.index.addDocFile(found, true);
		}
		catch (IOException e) {
		    System.out.println("fail.");
		    continue;
		};
		System.out.println("done.");
		this.count++;
	    };
	};
    };

    public static void main(String[] args) throws IOException {

	KorapIndexer ki = new KorapIndexer();

	System.out.println();

	for (String arg : args) {
	    File f = new File( arg );
	    if (f.isDirectory()) {
		ki.parse(f);
	    };
	};

	System.out.println("-----");
	System.out.print("  Commit ... ");
	ki.index.commit();
	System.out.println("done.");
	System.out.println("-----");
	System.out.println("  Indexed " + ki.count + " files.");
	System.out.println();
    };
};