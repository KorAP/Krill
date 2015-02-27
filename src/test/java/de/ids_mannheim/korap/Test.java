package de.ids_mannheim.korap;

import java.util.*;
import java.io.IOException;

import static org.junit.Assert.*;

import de.ids_mannheim.korap.index.MultiTermTokenStream;
import de.ids_mannheim.korap.index.MultiTermToken;
import de.ids_mannheim.korap.util.CorpusDataException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;

/**
 * Helper class for testing the KrillIndex framework (Normal).
 *
 * @author diewald
 */
public class Test {

    public static void addDoc(IndexWriter w, Map<String, String> m) throws IOException {
        Document doc = new Document();
        String[] strInt = { "pubDate" };
        String[] strStr = { "id", "corpus", "pubPlace" };
        String[] strTxt = { "title", "subtitle", "textClass" };

        // Text fields
        for (String s : strTxt) {
            doc.add(new TextField(s, m.get(s), Field.Store.YES));
        };

        // String fields
        for (String s : strStr) {
            doc.add(new StringField(s, m.get(s), Field.Store.YES));
        };

        // Integer fields
        for (String s : strInt) {
            doc.add(new IntField(s, Integer.parseInt(m.get(s)), Field.Store.YES));
        };

        FieldType textFieldWithTermVectors = new FieldType(TextField.TYPE_STORED);
        textFieldWithTermVectors.setStoreTermVectors(true);
        textFieldWithTermVectors.setStoreTermVectorOffsets(true);
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
            
            String[] tokseg = seg.split("\\|");

            try {
                MultiTermToken mtt = new MultiTermToken('s', tokseg[0]);
            
                mtt.add("T");
                mtt.add('i', tokseg[0].toLowerCase());
                mtt.add('p', tokseg[1]);
                mtt.add('l', tokseg[2]);

                if (tokseg.length == 4) {
                    for (String morph : tokseg[3].split(";")) {
                        mtt.add('m', morph);
                    }
                };
                if (tokseg.length == 5) {
                    mtt.add('e', tokseg[4]);
                };
                
                ts.addMultiTermToken(mtt);
            }
            catch (CorpusDataException cde) {
                fail(cde.getErrorCode() + ": " + cde.getMessage());
            };
        };
        
        return ts;
    };
};
