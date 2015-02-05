package de.ids_mannheim.korap.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A collection of string related utility
 * functions.
 *
 * @author diewald
 */
public class KorapString {

    /**
     * Get String from file.
     *
     * @param path The path of the file represented as a string. 
     * @param path The expected {@link Charset}. 
     * @return The content of the file
     * @throws IOException
     */
    public static String StringfromFile (String path, Charset encoding)
        throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    };


    /**
     * Get String from file (expecting UTF-8).
     *
     * @param path The path of the file represented as a string. 
     * @return The content of the file
     * @throws IOException
     */
    public static String StringfromFile (String path) throws IOException {
        return StringfromFile(path, StandardCharsets.UTF_8);
    };


    /**
     * Escape HTML relevant characters as entities.
     *
     * @param text The string to escape.
     * @return The secured string.
     */
    public static String escapeHTML (String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    };
};
