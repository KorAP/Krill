package de.ids_mannheim.korap;

import java.util.*;
import java.io.IOException;

import de.ids_mannheim.korap.analysis.MultiTermTokenStream;
import de.ids_mannheim.korap.analysis.MultiTermToken;
import static de.ids_mannheim.korap.util.KorapByte.*;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.AtomicReaderContext;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;

import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.SpanQuery;

import org.apache.lucene.util.Bits;

/**
 * @author Nils Diewald
 *
 * Helper class for testing the KorapIndex framework (Simple).
 */
public class TestSimple {

    public static void addDoc(IndexWriter w, Map<String, String> m) throws IOException {
	Document doc = new Document();

	FieldType textFieldWithTermVectors = new FieldType(TextField.TYPE_STORED);
	textFieldWithTermVectors.setStoreTermVectors(true);
	/*
	  No offsets are stored.
	  textFieldWithTermVectors.setStoreTermVectorOffsets(true);
	*/
	textFieldWithTermVectors.setStoreTermVectorPositions(true);
	textFieldWithTermVectors.setStoreTermVectorPayloads(true);

	Field textFieldAnalyzed = new Field(
          "text",
          m.get("textStr"),
	  textFieldWithTermVectors
	);

	MultiTermTokenStream ts = getTermVector(m.get("text"));

	textFieldAnalyzed.setTokenStream( ts );

	doc.add(textFieldAnalyzed);

	// Add document to writer
	w.addDocument(doc);
    };


    public static MultiTermTokenStream getTermVector (String stream) {
	MultiTermTokenStream ts = new MultiTermTokenStream();

	int pos = 0;
	for (String seg : stream.split(" ")) {
	    //	    System.err.println("** Prepare " + seg);
	    String[] tokens = seg.split("\\|");

	    int i = 0;

	    while (tokens[i].length() == 0)
		i++;

	    MultiTermToken mtt = new MultiTermToken(tokens[i]);
	    //	    System.err.println("** Add term " + tokens[i]);
	    i++;
	    for (; i < tokens.length; i++) {
		if (tokens[i].length() == 0)
		    continue;
		mtt.add(tokens[i]);
	    };

	    ts.addMultiTermToken(mtt);
	};

	return ts;
    };

    public static List<String> getSpanInfo (IndexReader reader, SpanQuery query) throws IOException {
	Map<Term, TermContext> termContexts = new HashMap<>();
	List<String> spanArray = new ArrayList<>();

	for (AtomicReaderContext atomic : reader.leaves()) {
	    Bits bitset = atomic.reader().getLiveDocs();
	    // Spans spans = NearSpansOrdered();
	    Spans spans = query.getSpans(atomic, bitset, termContexts);

	    while (spans.next()) {
		StringBuffer payloadString = new StringBuffer();
		int docid = atomic.docBase + spans.doc();
		if (spans.isPayloadAvailable()) {
		    for (byte[] payload : spans.getPayload()) {
			/* retrieve payload for current matching span */

			payloadString.append(byte2int(payload)).append(",");
			payloadString.append(byte2int(payload, 2));
			//			payloadString.append(byte2int(payload, 1));
			payloadString.append(" (" + payload.length + ")");
			payloadString.append(" | ");
		    };
		};
		spanArray.add(
		    "Doc: " +
		    docid +
		    " with " +
		    spans.start() +
		    "-" +
		    spans.end() +
		    " || " +
		    payloadString.toString()
		);
	    };
	};
	return spanArray;
    };
};
