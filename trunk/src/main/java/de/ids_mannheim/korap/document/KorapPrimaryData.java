package de.ids_mannheim.korap.document;

public class KorapPrimaryData {
    private String primary;

    public KorapPrimaryData (String text) {
	this.primary = text;
    };

    public String substring (int startOffset) {
	return this.primary.substring(startOffset);
    };

    public String substring (int startOffset, int endOffset) {
	return this.primary.substring(startOffset, endOffset);
    };

    public String toString () {
	return this.primary;
    };

    public int length () {
	return this.primary.length();
    };
};
