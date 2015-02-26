package de.ids_mannheim.korap.util;

import java.util.*;

/**
 * A collection of string array related
 * utility functions.
 *
 * @author diewald
 */
public class KrillArray {

    /**
     * Join a sequence of strings to a single string.
     *
     * @param separator String to separate joined segments
     * @param strings Segments to join
     * @return The joined string.
     */
    public static String join (String separator, String ... strings) {
        if (strings.length == 0)
            return "";

        StringBuffer sb = new StringBuffer(strings[0]);

        for (int i = 1; i < strings.length; i++) {
            sb.append(separator);
            sb.append(strings[i]);
        };

        return sb.toString();
    };


    /**
     * Join a sequence of strings to a single string.
     *
     * @param separator Character to separate joined segments
     * @param strings Segments to join
     * @return The joined string.
     */
    public static String join (char separator, String ... strings) {
        if (strings.length == 0)
            return "";

        StringBuffer sb = new StringBuffer(strings[0]);
        
        for (int i = 1; i < strings.length; i++) {
            sb.append(separator);
            sb.append(strings[i]);
        };
        
        return sb.toString();
    };
};
