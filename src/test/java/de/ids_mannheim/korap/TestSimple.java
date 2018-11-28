package de.ids_mannheim.korap;

import static de.ids_mannheim.korap.util.KrillByte.byte2int;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.index.MultiTermToken;
import de.ids_mannheim.korap.index.MultiTermTokenStream;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.CorpusDataException;
import de.ids_mannheim.korap.util.QueryException;

/**
 * Helper class for testing the KrillIndex framework (Simple).
 * 
 * @author diewald
 */
public class TestSimple {

    private static Logger log  = LoggerFactory.getLogger(TestSimple.class);
    
    // Add document
    public static void addDoc (IndexWriter w, Map<String, String> m)
            throws IOException {
        Document doc = new Document();

        FieldType textFieldWithTermVectors = new FieldType(
                TextField.TYPE_STORED);
        textFieldWithTermVectors.setStoreTermVectors(true);
        /*
          No offsets are stored.
          textFieldWithTermVectors.setStoreTermVectorOffsets(true);
        */
        textFieldWithTermVectors.setStoreTermVectorPositions(true);
        textFieldWithTermVectors.setStoreTermVectorPayloads(true);

        Field textFieldAnalyzed = new Field("text", m.get("textStr"),
                textFieldWithTermVectors);

        MultiTermTokenStream ts = getTermVector(m.get("text"));

        textFieldAnalyzed.setTokenStream(ts);

        doc.add(textFieldAnalyzed);

        // Add document to writer
        w.addDocument(doc);
    };

    public static FieldDocument simpleFieldDoc (String s) {
        return simpleFieldDoc(s, "");
    }
            
    // Add document
    public static FieldDocument simpleFieldDoc (String s, String delimiter) {
        String[] characters = s.split(delimiter);

        FieldDocument fd = new FieldDocument();
        String surface = "";
        String annotation = "";

        for (int i = 0; i < characters.length; i++) {
            String fixChar = characters[i];
            surface += fixChar;
            annotation += "[("+i+"-"+(i+1)+")s:"+fixChar;
            if (i == 0)
                annotation += "|<>:base/s:t$<b>64<i>0<i>" + characters.length + "<i>" + characters.length + "<b>0";
            annotation += "|_"+i+"$<i>"+i+"<i>"+(i+1)+"]";
        };

        fd.addTV("base",surface, annotation);
        return fd;
    };

    // Create a new FieldDocument with random data
    public static FieldDocument simpleFuzzyFieldDoc (List<String> chars, int minLength, int maxLength) {
        String surface = "";

        for (int i = 0; i < (int)(Math.random() * (maxLength - minLength)) + minLength; i++) {
            String randomChar = chars.get((int)(Math.random() * chars.size()));
            surface += randomChar;
        };
        return simpleFieldDoc(surface);

    };

    // Get Term Vector
    public static MultiTermTokenStream getTermVector (String stream) {
        MultiTermTokenStream ts = new MultiTermTokenStream();

        int pos = 0;
        for (String seg : stream.split(" ")) {
            //	    System.err.println("** Prepare " + seg);
            String[] tokens = seg.split("\\|");

            int i = 0;

            while (tokens[i].length() == 0)
                i++;

            try {
                MultiTermToken mtt = new MultiTermToken(tokens[i]);
                //	    System.err.println("** Add term " + tokens[i]);
                i++;
                for (; i < tokens.length; i++) {
                    if (tokens[i].length() == 0)
                        continue;
                    mtt.add(tokens[i]);
                };
                ts.addMultiTermToken(mtt);
            }
            catch (CorpusDataException cde) {
                fail(cde.getErrorCode() + ": " + cde.getMessage());
            };
        };

        return ts;
    };


    // Get query wrapper based on json file
    public static SpanQueryWrapper getJsonQuery (String jsonFile) throws QueryException {
        SpanQueryWrapper sqwi;
		String json = getJsonString(jsonFile);
		sqwi = new KrillQuery("tokens").fromKoral(json);
        return sqwi;
    };


    // Get string
    public static String getJsonString (String path) {

        StringBuilder contentBuilder = new StringBuilder();
        try {			
			BufferedReader in = new BufferedReader(
				new InputStreamReader(
					new FileInputStream(URLDecoder.decode(path, "UTF-8")),
					"UTF-8"
					)
				);
            String str;
            while ((str = in.readLine()) != null) {
                contentBuilder.append(str);
            };
            in.close();
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        return contentBuilder.toString();
    };


    // getSpan Info
    public static List<String> getSpanInfo (IndexReader reader, SpanQuery query)
            throws IOException {
        Map<Term, TermContext> termContexts = new HashMap<>();
        List<String> spanArray = new ArrayList<>();

        for (LeafReaderContext atomic : reader.leaves()) {
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
                spanArray.add("Doc: " + docid + " with " + spans.start() + "-"
                        + spans.end() + " || " + payloadString.toString());
            };
        };
        return spanArray;
    };


    // Simple fuzzing test
    public static void fuzzingTest (List<String> chars, Pattern resultPattern,
            SpanQuery sq, int minTextLength, int maxTextLength, int maxDocs)
            throws IOException, QueryException {

        Krill ks = new Krill(sq);
        String lastFailureConf = "";

        // Multiple runs of corpus creation and query checks
        for (int x = 0; x < 100000; x++) {
            KrillIndex ki = new KrillIndex();
            ArrayList<String> list = new ArrayList<String>();
            int c = 0;

            // Create a corpus of <= maxDocs fuzzy docs
            for (int i = 0; i < (int) (Math.random() * maxDocs); i++) {
                FieldDocument testDoc = simpleFuzzyFieldDoc(chars,
                        minTextLength, maxTextLength);
                String testString = testDoc.doc.getField("base").stringValue();
                Matcher m = resultPattern.matcher(testString);
                list.add(testString);
                int offset = 0;
                while (m.find(offset)) {
                    c++;
                    offset = Math.max(0, m.start() + 1);
                }
                ki.addDoc(testDoc);
            };

            ki.commit();
            Result kr = ks.apply(ki);
            
            // Check if the regex-calculated matches are correct,
            // otherwise
            // spit out the corpus configurations
            if (c != kr.getTotalResults()) {
                String failureConf = "expected:" + c + ", actual:"
                        + kr.getTotalResults() + ", docs:" + list.toString();

                // Try to keep the failing configuration small
                if (lastFailureConf.length() == 0
                        || failureConf.length() < lastFailureConf.length()) {
                    System.err.println(failureConf);
                    lastFailureConf = failureConf;
                    minTextLength--;
                    maxDocs--;
                };
            };
        };
    };
};
