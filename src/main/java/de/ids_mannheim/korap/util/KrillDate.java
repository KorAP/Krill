package de.ids_mannheim.korap.util;

import java.util.*;
import java.util.regex.*;

/**
 * KrillDate implements a helper object to stringify
 * and parse date strings optimized
 * for integer range queries in Lucene.
 * No support for b.c. dates.
 *
 * Strings are parsed and serialized to
 * {@link http://tools.ietf.org/html/rfc3339 RFC3339}
 * compatible strings with a day granularity according to
 * {@link http://www.w3.org/TR/NOTE-datetime W3-DateTimes}.
 *
 * <blockquote><pre>
 *   KrillDate kd = new KrillDate("2005-06-03");
 *   System.err.println(kd.day());
 *   // 3

 *   kd = new KrillDate("2005-06");
 *   System.err.println(kd.month());
 *   // 6
 * </pre></blockquote>
 *
 * @author diewald
 */
public class KrillDate {

    /**
     * The year of the date.
     */
    public int year = 0;


    /**
     * The month of the date.
     */
    public int month = 0;


    /**
     * The day of the date.
     */
    public int day = 0;


    // Date string regex pattern
    private static final Pattern datePattern = Pattern.compile(
        "\\s*(\\d\\d\\d\\d)" +
        "(?:\\s*[-/]?\\s*(\\d\\d)" +
        "(?:\\s*[-/]?\\s*(\\d\\d))?)?\\s*"
    );

    /**
     * Static value representing the minimum date.
     */
    public static final int BEGINNING = 0;


    /**
     * Static value representing the maximum date.
     */
    public static final int END = 99_999_999;


    /**
     * Construct a new KrillDate object.
     */
    public KrillDate () { };


    /**
     * Construct a new KrillDate object.
     *
     * @param date The date as a string (see synopsis).
     */
    public KrillDate (String date) {
        if (date == null || date.isEmpty())
            return;

        // Use pattern to split string
        Matcher m = datePattern.matcher(date);
        if (m.matches()) {
            this.year = Integer.parseInt(m.group(1));
            if (m.group(2) != null)
                this.month = Integer.parseInt(m.group(2));
            if (m.group(3) != null)
                this.day   = Integer.parseInt(m.group(3));
        };
    };


    /**
     * Get the date as an integer with ceiled values for
     * undefined date segments.
     *
     * <blockquote><pre>
     *   KrillDate kd = new KrillDate("2005-06");
     *   System.err.println(kd.ceil());
     *   // 20050699
     * </pre></blockquote>
     *
     * @return ceiled integer value.
     */
    public int ceil () {
        return
            (ceil((byte) 4, this.year) * 10_000) +
            (ceil((byte) 2, this.month) * 100) +
            (ceil((byte) 2, this.day));
    };


    /**
     * Get the date as an integer with floored values for
     * undefined date segments.
     *
     * <blockquote><pre>
     *   KrillDate kd = new KrillDate("2005-06");
     *   System.err.println(kd.floor());
     *   // 20050600
     * </pre></blockquote>
     *
     * @return floored integer value.
     */
    public int floor () {
        int floor = 0;
        if (this.year == 0)
            return 0;

        floor = this.year * 10_000;

        if (this.month == 0)
            return floor;

        floor += this.month * 100;

        if (this.day == 0)
            return floor;

        return (floor + this.day);
    };


    /**
     * Serialize date to string, appended by zeros,
     * in the form of &quot;20050300&quot;.
     *
     * @return The date as a string.
     */
    public String toString() {
        StringBuilder sb = this.toStringBuilder();
        if (sb.length() < 4)
            return null;

        if (sb.length() < 8) {
            sb.append("00");
            if (sb.length() < 8) {
                sb.append("00");
            };
        };

        return sb.toString();
    };


    /**
     * Serialize ceiled date to string.
     *
     * @return The date as a string.
     */
    public String toCeilString() {
        StringBuilder sb = new StringBuilder();
        return sb.append(this.ceil()).toString();
    };


    /**
     * Serialize floored date to string.
     *
     * @return The date as a string.
     */
    public String toFloorString() {
        StringBuilder sb = new StringBuilder();
        return sb.append(this.floor()).toString();
    };


    /**
     * Serialize date to displayable string.
     * See format description in the class description.
     *
     * @return The date as a string.
     */
    public String toDisplay() {
        StringBuilder sb = this.toStringBuilder();
        if (sb.length() == 8)
            sb.insert(6, '-');

        if (sb.length() > 4)
            sb.insert(4, '-');

        return sb.toString();
    };



    // Format date as yyyymmdd
    private StringBuilder toStringBuilder () {
        StringBuilder sb = new StringBuilder();

        if (this.year != 0) {

            // Append year
            if (this.year < 100)
                sb.append("20");

            sb.append(this.year);
	    
            if (this.month != 0) {
                
                // Append month
                if (this.month < 10)
                    sb.append('0');

                sb.append(this.month);

                if (this.day != 0) {
                    // Append month
                    if (this.day < 10)
                        sb.append('0');

                    sb.append(this.day);
                };
            };
        };
        return sb;
    };


    // Ceil method
    private static int ceil (byte padding, int nr) {
        if (nr == 0) {
            if (padding == (byte) 4)
                return 9999;
            else if (padding == (byte) 2)
                return 99;
        };
        return nr;
    };
};
