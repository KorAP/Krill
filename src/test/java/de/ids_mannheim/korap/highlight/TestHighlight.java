import java.util.*;
import java.io.IOException;

// import org.apache.lucene.search.postingshighlight.PostingsHighlighter;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;

import de.ids_mannheim.korap.analysis.MultiTermTokenStream;

import org.apache.lucene.search.IndexSearcher;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.util.Version;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;

import static de.ids_mannheim.korap.Test.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestHighlight { // extends LuceneTestCase {

    // Create index in RAM
    private Directory index = new RAMDirectory();

    StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);

    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);


    @Test
    public void checkHighlights () throws IOException  {
	// Check directory

	IndexWriter w = new IndexWriter(index, config);

	Document doc = new Document();
	FieldType textFieldWithTermVectors = new FieldType(TextField.TYPE_STORED);
	textFieldWithTermVectors.setStoreTermVectors(true);
	textFieldWithTermVectors.setStoreTermVectorOffsets(true);
	textFieldWithTermVectors.setStoreTermVectorPositions(true);

	Field textFieldAnalyzed = new Field(
            "text",
	    "Er wagte nicht, sich zu ruehren. Er war starr vor Angst.",
	    textFieldWithTermVectors
	);
    
	MultiTermTokenStream ts = getTermVector(
            "Er#0-2|PPER|er|c:nom;n:sg;g:masc;p:3|s:<$0-32 " +
	    "wagte#3-8|VVFIN|wagen|p:3;n:sg;t:past;m:ind| " +
	    "nicht#9-14|PTKNEG|nicht|| " +
	    ",#14-15|$,|,|| " +
	    "sich#16-20|PRF|sich|c:acc;p:3;n:sg| " +
	    "zu#21-23|PTKZU|zu|| " +
	    "ruehren#24-31|VVFIN|ruehren|| " +
	    ".#31-32|$.|.||s:>$0-32 " +
	    "Er#33-35|PPER|er|c:nom;p:3;n:sg;g:masc|s:<$33-56 " +
	    "war#36-39|VAFIN|sein|p:3;n:sg;t:past;m:ind| " +
	    "starr#40-45|ADJD|starr|comp:pos| " +
	    "vor#46-49|APPR|vor|| " +
	    "Angst#50-55|NN|angst|c:dat;n:sg;g:fem| " +
	    ".#55-56|$.|.||s:>$33-56"
        );

	textFieldAnalyzed.setTokenStream( ts );

	doc.add(textFieldAnalyzed);

	// Add document to writer
	w.addDocument(doc);

	assertEquals(1, w.numDocs());

	w.close();
    
	DirectoryReader reader = DirectoryReader.open( index );

	// Check searcher
	IndexSearcher searcher = new IndexSearcher( reader );


    };
};
