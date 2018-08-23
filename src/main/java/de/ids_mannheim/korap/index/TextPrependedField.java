package de.ids_mannheim.korap.index;

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import java.io.IOException;

public final class TextPrependedField extends Field {

    public static FieldType TEXT = new FieldType(TextField.TYPE_STORED);

    static {
        TEXT.setStoreTermVectors(true);
        TEXT.setStoreTermVectorPositions(true);
        TEXT.setStoreTermVectorPayloads(true);
        TEXT.setStoreTermVectorOffsets(false);
    };


    public TextPrependedField (String name, String value) {
        super(name, value, TEXT);
        TextPrependedTokenStream tpts = new TextPrependedTokenStream(value);
        this.setTokenStream(tpts);
    };
};
