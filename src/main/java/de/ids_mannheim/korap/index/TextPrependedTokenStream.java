package de.ids_mannheim.korap.index;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.analysis.standard.StandardTokenizerImpl;
import org.apache.lucene.analysis.util.CharacterUtils;
import java.io.IOException;
import java.io.StringReader;

/**
 * Create a tokenstream with the first token being the verbatim string.
 * All following tokens are standardtokenized and lowercased.
 */

public class TextPrependedTokenStream extends TokenStream {
	private final CharTermAttribute charTermAttr = this.addAttribute(CharTermAttribute.class);
	private final PositionIncrementAttribute posIncrAttr = this.addAttribute(PositionIncrementAttribute.class);
	private final CharacterUtils charUtils = CharacterUtils.getInstance();
    private Boolean init;
	private String verbatim;
	private int skippedPositions;

	/** A private instance of the JFlex-constructed scanner */
	private StandardTokenizerImpl scanner;
	private final int maxTokenLength = 1024 * 1024;

	/** Constructor */
	public TextPrependedTokenStream (String text) {
		this.init = true;
		this.verbatim = text;
		this.scanner = null;
	};

	/** Do not repeat the verbatim string at the beginning */
	public void doNotPrepend () {
		this.init = false;
	};

	@Override
	public final boolean incrementToken () throws IOException {
		clearAttributes();
		skippedPositions = 0;

		// Repeat the verbatim string at the beginning
		if (this.init) {
			posIncrAttr.setPositionIncrement(255);
			charTermAttr.append(this.verbatim);
			this.init = false;
			return true;
		};

		// Initialize the scanner
		if (this.scanner == null) {
			this.scanner = new StandardTokenizerImpl(
				new StringReader(this.verbatim)
				);
		};

		// Increment tokens by wrapping the scanner like the StandardTokenizer
		while(true) {
			int tokenType = scanner.getNextToken();
			
			if (tokenType == StandardTokenizerImpl.YYEOF) {
				return false;
			}

			if (scanner.yylength() <= maxTokenLength) {
				posIncrAttr.setPositionIncrement(
					skippedPositions+1
					);
				scanner.getText(charTermAttr);
				charUtils.toLowerCase(charTermAttr.buffer(), 0, charTermAttr.length());
				return true;
			} else
				skippedPositions++;
		}
	};
};
