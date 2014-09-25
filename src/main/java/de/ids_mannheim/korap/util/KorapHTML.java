package de.ids_mannheim.korap.util;

/**
 * @author Nils Diewald
 *
 * A VCollection of methods to deal with Bytes and Byte arrays.
 */
public class KorapHTML {

    /**
     * Encode a string HTML secure.
     *
     * @param text The string to encode.
     */
    public static String encodeHTML (String text) {
	return
	    text.replace("&", "&amp;")
	    .replace("<", "&lt;")
	    .replace(">", "&gt;")
	    .replace("\"", "&quot;");
    };
};
