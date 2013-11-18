package de.ids_mannheim.korap.util;

import java.util.*;

// Maybe wrong! TEST!

/**
 * @author Nils Diewald
 *
 * A collection of methods to deal with Bytes and Byte arrays.
 */
public class KorapByte {

    /**
     * Convert an integer to a byte array.
     *
     * @param number The number to convert.
     */
    // Based on http://www.tutorials.de/java/228129-konvertierung-von-integer-byte-array.html
    public static byte[] int2byte (int number) {
	byte[] data = new byte[4];
	for (int i = 0; i < 4; ++i) {
	    int shift = i << 3; // i * 8
	    data[3-i] = (byte)((number & (0xff << shift)) >>> shift);
	};
	return data;
    };

    /**
     * Convert a byte array to an integer.
     *
     * @param number The number to convert.
     */
    // Based on http://www.tutorials.de/java/228129-konvertierung-von-integer-byte-array.html
    public static int byte2int (byte[] data, int offset) {
	int number = 0;   
	int i = (offset*4);  
	for (; i < 4; ++i) {
	    number |= (data[3-i] & 0xff) << (i << 3);
	};
	return number;
    };

    public static int byte2int (byte[] data) {
	return byte2int(data, 0);
    };

    /*
    public static short byte2short (byte[] data, int offset) {
	short number = 0;
	number |= (data[3-offset] & 0xff) << (offset << 3);
	offset--;
	number |= (data[3-offset] & 0xff) << (offset << 3);
	return number;
    };

    public static short byte2short (byte[] data) {
	return byte2short(datam 0);
    };
    */
};
