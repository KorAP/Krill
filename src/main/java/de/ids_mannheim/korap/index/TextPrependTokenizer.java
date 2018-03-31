package de.ids_mannheim.korap.index;

// This code is pretty similar to
// org.apache.lucene.analysis.standard.StandardTokenizer,
// but prepends a verbatim string to the TokenStream

import java.io.IOException;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
//  de.ids_mannheim.korap.index.VerbatimAttr
import org.apache.lucene.analysis.standard.StandardTokenizerImpl;

import org.apache.lucene.util.AttributeFactory;


public final class TextPrependTokenizer extends Tokenizer {

	/** A private instance of the JFlex-constructed scanner */
	private StandardTokenizerImpl scanner;

	private int skippedPositions;

	private String verbatim;
	private Boolean init = true;

	private int maxTokenLength = 1024 * 1024;

	public TextPrependTokenizer() {
		init();
	}

	public TextPrependTokenizer(AttributeFactory factory) {
		super(factory);
		init();
	}

	private void init() {
		this.scanner = new StandardTokenizerImpl(input);
		this.init = true;
	}

	public void setVerbatim (String v) {
		this.verbatim = v;
	};
	
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

	@Override
	public final boolean incrementToken() throws IOException {
		clearAttributes();
		skippedPositions = 0;

		if (this.init) {
			posIncrAtt.setPositionIncrement(10000);
			termAtt.append("[PREPEND]");
			this.init = false;
			return true;
		};

		while(true) {
			int tokenType = scanner.getNextToken();
			
			if (tokenType == StandardTokenizerImpl.YYEOF) {
				return false;
			}

			if (scanner.yylength() <= maxTokenLength) {
				posIncrAtt.setPositionIncrement(skippedPositions+1);
				scanner.getText(termAtt);
				return true;
			} else
				skippedPositions++;
		}
	}
  
	@Override
	public final void end() throws IOException {
		super.end();
		posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement()+skippedPositions);
	}

	@Override
	public void close() throws IOException {
		super.close();
		scanner.yyreset(input);
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		scanner.yyreset(input);
		skippedPositions = 0;
	}
}
