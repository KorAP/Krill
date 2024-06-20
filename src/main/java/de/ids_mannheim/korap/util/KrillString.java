package de.ids_mannheim.korap.util;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A collection of string related utility
 * functions.
 * 
 * @author diewald
 */
public class KrillString {

    /**
     * Get String from file.
     * 
     * @param path
     *            The path of the file represented as a string.
     * @param path
     *            The expected {@link Charset}.
     * @return The content of the file
     * @throws IOException
     */
    public static String StringfromFile (String path, Charset encoding)
            throws IOException {
        path = URLDecoder.decode(path, "UTF-8");
        path = path.replaceFirst("^/(.:/)", "$1");
        Path p = Paths.get(path);
        byte[] encoded = Files.readAllBytes(p);
        return new String(encoded, encoding);
    };


    /**
     * Get String from file (expecting UTF-8).
     * 
     * @param path
     *            The path of the file represented as a string.
     * @return The content of the file
     * @throws IOException
     */
    public static String StringfromFile (String path) throws IOException {
        return StringfromFile(path, StandardCharsets.UTF_8);
    };


    /**
     * Escape HTML relevant characters as entities.
     * 
     * @param text
     *            The string to escape.
     * @return The secured string.
     */
    public static String escapeHTML (String text) {

		if (text == null)
			return "";
		
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    };


    /**
     * Escape Bracket relevant characters.
     * 
     * @param text
     *            The string to escape.
     * @return The secured string.
     */
    public static String escapeBrackets (String text) {
        if (text == null)
            return "";
        return text.replaceAll("([\\{\\}\\[\\]\\\\])", "\\\\$1");
    };


    /**
     * Add surrounding double quotes.
     * 
     * @param text
     *            The string to escape.
     * @return The secured string.
     */
    public static String quote (String text) {
        return '"' + text.replaceAll("([\"\\\\])", "\\\\$1") + '"';
    };


    /**
     * Provide a substring method that works well with surrogate pairs.
     * 
     * @param text
     *            The string to substring.
     * @param start
     *            The start offset.
     * @param end
     *            The end offset.
     * @return The substring.
     */
    public static String codePointSubstring(String text, int start, int end) {
        int a = text.offsetByCodePoints(0, start);
        return text.substring(
            a,
            text.offsetByCodePoints(a, end - start)
            );
    };

    /**
     * Provide a substring method that works well with surrogate pairs.
     * 
     * @param text
     *            The string to substring.
     * @param start
     *            The start offset.
     * @return The substring.
     */
    public static String codePointSubstring(String text, int start) {
        return text.substring(text.offsetByCodePoints(0, start));
    };
};
