package de.ids_mannheim.korap.util;

import java.util.*;
import java.util.regex.*;

/**
 * @author Nils Diewald
 *
 * KorapDate implements a helper object to stringify and parse date strings implemented
 * for integer range queries.
 */
public class KorapDate {
    /*
    protected char[] year  = new char[4];
    protected char[] month = new char[2];
    protected char[] day   = new char[2];
    */

    private int year = 0, month = 0, day = 0;

    private static final Pattern datePattern = Pattern.compile(
	"(\\d\\d\\d\\d)" +
        "(?:[-/]?(\\d\\d)" +
        "(?:[-/]?(\\d\\d))?)?"
    );

    public static int END = 99_999_999;
    public static int BEGINNING = 0;

    public KorapDate (String dateStr) {
	if (dateStr == null || dateStr.isEmpty())
	    return;

	Matcher m = datePattern.matcher(dateStr);
	if (m.matches()) {
	    this.year = Integer.parseInt(m.group(1));
	    if (m.group(2) != null)
		this.month = Integer.parseInt(m.group(2));
		if (m.group(3) != null)
		    this.day   = Integer.parseInt(m.group(3));
	}
	else {
	    return;
	};
    };

    private static int ceil (short padding, int nr) {
	if (nr == 0) {
	    if (padding == (short) 4) {
		return 9999;
	    }
	    else if (padding == (short) 2) {
		return 99;
	    };
	};
	return nr;
    };

    // make yyyy???? become yyyy9999 and yyyymm?? yyyymm99
    public int ceil () {
	return
	    (ceil((short) 4, this.year) * 10_000) +
	    (ceil((short) 2, this.month) * 100) +
	    (ceil((short) 2, this.day));
    };

    // make yyyy???? become yyyy0000 and yyyymm?? yyyymm00
    public int floor () {
	int floor = 0;
	if (this.year == 0) {
	    return 0;
	}
	else {
	    floor = this.year * 10_000;
	};
	if (this.month == 0) {
	    return floor;
	}
	else {
	    floor += this.month * 100;
	};
	if (this.day == 0) {
	    return floor;
	};
	return (floor + this.day);
    };


    public int year () {
	return this.year;
    };

    public int month () {
	return this.month;
    };

    public int day () {
	return this.day;
    };


    public String toString() {
	StringBuilder sb = this.toStringBuilder();
	if (sb.length() < 4)
	    return null;

	if (sb.length() < 8) {
	    sb.append("00");
	    if (sb.length() < 6) {
		sb.append("00");
	    };
	};

	return sb.toString();
    };

    public String toDisplay() {
	StringBuilder sb = this.toStringBuilder();
	if (sb.length() == 8)
	    sb.insert(6, '-');

	if (sb.length() > 4)
	    sb.insert(4, '-');

	return sb.toString();
    };

    public String toCeilString() {
	StringBuilder sb = new StringBuilder();
	return sb.append(this.ceil()).toString();
    };

    public String toFloorString() {
	StringBuilder sb = new StringBuilder();
	return sb.append(this.floor()).toString();
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
};
