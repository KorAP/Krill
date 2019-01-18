package de.ids_mannheim.korap.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.ids_mannheim.korap.index.AbstractDocument;
import de.ids_mannheim.korap.util.KrillDate;

import java.io.IOException;

import de.ids_mannheim.korap.index.KeywordAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.StringReader;

import java.util.*;
import java.util.regex.*;

import org.apache.lucene.index.*;
import org.apache.lucene.document.FieldType;

public class MetaFieldsObj implements Iterable<MetaField> {

	// Logger
	private final static Logger log = LoggerFactory.getLogger(MetaFields.class);

	// This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private static final Pattern dateValuePattern = Pattern.compile("^([0-9]{8})$");

	// Mapper for JSON serialization
    ObjectMapper mapper = new ObjectMapper();

	public List<String> fieldsOrder;

	private Map<String, MetaField> fieldsMap = new HashMap<>();


	public MetaFieldsObj () {};

    
	/**
	 * Add field to collection
	 */
	public MetaField add (IndexableField iField) {
        return this.add(
            metaFieldFromIndexableField(
                iField,
                new MetaField(iField.name())
                )
            );
	};


	/**
	 * Add field to collection
	 */
    public MetaField add (MetaField mf) {
		// Ignore non-stored fields
		if (mf == null)
			return null;

        if (fieldsMap.containsKey(mf.key)) {
            fieldsMap.get(mf.key).values.addAll(mf.values);
            return fieldsMap.get(mf.key);
        };
        
        fieldsMap.put(mf.key, mf);
        return mf;
    };


    // Field type needs to be restored heuristically
    // - though that's not very elegant
    public static MetaField metaFieldFromIndexableField (IndexableField iField, MetaField mf) {
		IndexableFieldType iFieldType = iField.fieldType();

		// Field type needs to be restored heuristically
		// - though that's not very elegant

		// Ignore non-stored fields
		if (!iFieldType.stored())
			return null;
		
		// TODO: Check if metaField exists for that field

		Number n = iField.numericValue();
		String s = iField.stringValue();

		// Field has numeric value (possibly a date)
		if (n != null) {

			// Check if key indicates a date
			Matcher dateMatcher = dateValuePattern.matcher(n.toString());
            if (dateMatcher.matches()) {
                mf.type = "type:date";
                
                KrillDate date = new KrillDate(n.toString());
				if (date != null) {

					// Serialize withz dash separation
					mf.values.add(date.toDisplay());
				};
            }

			// Field is a number
			else {
                mf.type = "type:integer";
                mf.values.add(new Integer(n.intValue()).toString());
			};
		}
		
		// Field has a textual value
		else if (s != null) {

            // Stored
			if (iFieldType.indexOptions() == IndexOptions.NONE) {

                String value = s.toString();
                if (value.startsWith("data:")) {
                    mf.type = "type:attachement";
                }
                else {
                    mf.type = "type:store";
                };
				mf.values.add(value);
                return mf;
			}

			// Keywords
			else if (iFieldType.indexOptions() == IndexOptions.DOCS_AND_FREQS) {
				mf.type = "type:keywords";

				// Analyze keywords
				try {
					StringReader reader = new StringReader(s.toString());
					KeywordAnalyzer kwa = new KeywordAnalyzer();
					TokenStream ts = kwa.tokenStream("-", reader);
					CharTermAttribute term;
					ts.reset();
					while (ts.incrementToken()) {
						term = ts.getAttribute(CharTermAttribute.class);
						mf.values.add(term.toString());
					};
					ts.close();
					reader.close();
				}
				catch (IOException e) {
					log.error("Unable to split {}={}", iField.name(), s.toString());
				}
			}

			// Text
			else if (iFieldType.indexOptions() != IndexOptions.DOCS) {
				mf.type = "type:text";
				mf.values.add(s.toString());
			}

            // Special treatment for legacy indices
            else if (mf.key.equals("UID")) {
				mf.type = "type:integer";
				mf.values.add(s.toString());
            }

			// String
			else {
				mf.values.add(s.toString());
			};
		}
		
		else {
			log.error("Unknown field type {}", iField.name());
		};

        mf.values.removeAll(Collections.singleton(null));

        return mf;
    };


    /**
	 * Get field from collection
     *
     * @param key
     *        The key of the field
     */
    public MetaField get (String key) {
        return fieldsMap.get(key);
    };


    /**
	 * Check for field existence.
     *
     * @param key
     *        The key of the field
     */
    public Boolean contains (String key) {
        return fieldsMap.containsKey(key);
    };

    private Iterator<String> getIterator () {
        if (this.fieldsOrder == null) {
            return fieldsMap.keySet().iterator();
        };
        return this.fieldsOrder.iterator();
    };

    
    @Override
    public Iterator<MetaField> iterator() {
        return new Iterator<MetaField>() {

            private Iterator<String> it = getIterator();

            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            };

            @Override
            public MetaField next() {
                return fieldsMap.get(it.next());
            };

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            };
        };
    };
};
