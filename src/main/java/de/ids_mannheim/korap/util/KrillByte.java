package de.ids_mannheim.korap.util;

import java.util.*;

/**
 * A collection of byte and byte array related
 * utility functions.
 * 
 * @author diewald
 */
public class KrillByte {

    /**
     * Convert an integer to a byte array.
     * 
     * @param number
     *            The number to convert.
     * @return The translated byte array.
     */
    // Based on
    // http://www.tutorials.de/java/228129-konvertierung-von-integer-byte-array.html
    public static byte[] int2byte (int number) {
        byte[] data = new byte[4];
        for (int i = 0; i < 4; ++i) {
            int shift = i << 3; // That's identical to i * 8
            data[3 - i] = (byte) ((number & (0xff << shift)) >>> shift);
        };
        return data;
    };


    /**
     * Convert a byte array to an integer.
     * 
     * @param data
     *            The byte array to convert.
     * @return The translated integer.
     */
    // Based on
    // http://www.tutorials.de/java/228129-konvertierung-von-integer-byte-array.html
    public static int byte2int (byte[] data) {
        return byte2int(data, 0);
    };


    /**
     * Convert a byte array to an integer.
     * 
     * @param data
     *            The byte array to convert.
     * @param offset
     *            The byte offset (Not integer offset!).
     * @return The translated integer.
     */
    // Roughly based on
    // http://www.tutorials.de/java/228129-konvertierung-von-integer-byte-array.html
    public static int byte2int (byte[] data, int offset) {
        offset += 3;
        int number = 0;
        for (int i = 0; i < 4; ++i)
            number |= (data[offset - i] & 0xff) << (i << 3);
        return number;
    };
};
