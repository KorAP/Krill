package de.ids_mannheim.korap.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Nils Diewald
 *
 * A collection of methods to deal with Strings.
 */
public class KorapString {

    public static String StringfromFile(String path, Charset encoding) throws IOException {
	byte[] encoded = Files.readAllBytes(Paths.get(path));
	return new String(encoded, encoding);
    };

    public static String StringfromFile (String path) throws IOException {
	return StringfromFile(path, StandardCharsets.UTF_8);
    };
};
