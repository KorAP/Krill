package de.ids_mannheim.korap.index;
import java.util.*;
import java.util.regex.*;
import de.ids_mannheim.korap.index.DocIdentifier;


public class MatchIdentifier extends DocIdentifier {
    private int startPos, endPos = -1;

    private ArrayList<int[]> pos = new ArrayList<>(8);

    Pattern idRegex = Pattern.compile(
		        "^match-(?:([^!]+?)!)?" +
			"([^!]+)-p([0-9]+)-([0-9]+)" +
			"((?:\\(-?[0-9]+\\)-?[0-9]+--?[0-9]+)*)" +
			"(?:c.+?)?$");
    Pattern posRegex = Pattern.compile(
		        "\\(([0-9]+)\\)([0-9]+)-([0-9]+)");

    public MatchIdentifier () {};

    public MatchIdentifier (String id) {
	Matcher matcher = idRegex.matcher(id);
	if (matcher.matches()) {
	    this.setCorpusID(matcher.group(1));
	    this.setDocID(matcher.group(2));
	    this.setStartPos(Integer.parseInt(matcher.group(3)));
	    this.setEndPos(Integer.parseInt(matcher.group(4)));

	    if (matcher.group(5) != null) {
		matcher = posRegex.matcher(matcher.group(5));
		while (matcher.find()) {
		    this.addPos(
		        Integer.parseInt(matcher.group(2)),
		        Integer.parseInt(matcher.group(3)),
			Integer.parseInt(matcher.group(1))
		    );
		};
	    };
	};
    };

    public int getStartPos () {
	return this.startPos;
    };

    public void setStartPos (int pos) {
	if (pos >= 0)
	    this.startPos = pos;
    };

    public int getEndPos () {
	return this.endPos;
    };

    public void setEndPos (int pos) {
	if (pos >= 0)
	    this.endPos = pos;
    };

    public void addPos(int start, int end, int number) {
	if (start >= 0 && end >= 0 && number >= 0)
	    this.pos.add(new int[]{start, end, number});
    };

    public ArrayList<int[]> getPos () {
	return this.pos;
    };

    public String toString () {

	if (this.docID == null) return null;

	StringBuilder sb = new StringBuilder("match-");

	// Get prefix string corpus/doc
	if (this.corpusID != null) {
	    sb.append(this.corpusID).append('!');
	};
	sb.append(this.docID);

	sb.append('-');
	sb.append(this.getPositionString());	
	return sb.toString();
    };

    /*
    public String getPositionBytes () {
	ByteBuffer b = new ByteBuffer(8);
	b.putInt(this.startPos);
	b.putInt(this.endPos);

	// Get Position information
	for (int[] i : this.pos) {
	    b.putInt(i[2]).putInt(i[0]).putInt(i[1]);
	};
    };
    */

    public String getPositionString () {
	StringBuilder sb = new StringBuilder();
	sb.append('p').append(this.startPos).append('-').append(this.endPos);

	// Get Position information
	for (int[] i : this.pos) {
	    sb.append('(').append(i[2]).append(')');
	    sb.append(i[0]).append('-').append(i[1]);
	};

	return sb.toString();

	/*
	if (this.processed) {
	    sb.append('c');
	    for (int[] s : this.span) {
		if (s[2] >= 256)
		    continue;
		
		if (s[2] != -1)
		    sb.append('(').append(s[2]).append(')');
		sb.append(s[0] + this.startOffsetChar);
		sb.append('-');
		sb.append(s[1] + this.startOffsetChar);
	    };
	};
	*/
    };
};
