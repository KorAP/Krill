package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.IOException;

import de.ids_mannheim.korap.index.KeywordAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class TestKeywordAnalyzer {

	@Test
	public void keywordAnalyzer () throws IOException {

		StringReader reader = new StringReader("alpha beta gamma");
		
		KeywordAnalyzer kwa = new KeywordAnalyzer();
		TokenStream ts = kwa.tokenStream("keys", reader);
		ts.reset();

		assertTrue(ts.incrementToken());
		CharTermAttribute term = ts.getAttribute(CharTermAttribute.class);

		assertEquals("alpha", term.toString());
		assertTrue(ts.incrementToken());
		term = ts.getAttribute(CharTermAttribute.class);
		assertEquals("beta", term.toString());

		assertTrue(ts.incrementToken());
		term = ts.getAttribute(CharTermAttribute.class);
		assertEquals("gamma", term.toString());

		assertTrue(!ts.incrementToken());
	};
};
