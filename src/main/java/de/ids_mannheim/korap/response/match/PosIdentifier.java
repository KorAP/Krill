package de.ids_mannheim.korap.response.match;

import java.util.*;

public class PosIdentifier extends DocIdentifier {
    private int start = -1;
	private int end = -1;

    public PosIdentifier () {};


    public void setStart (int pos) {
        if (pos >= 0)
            this.start = pos;
    };

    public int getStart () {
        return this.start;
    };

	public void setEnd (int pos) {
        if (pos >= 0)
            this.end = pos;
    };

	public int getEnd () {
        return this.end;
    };

    public String toString () {
        if (this.textSigle == null && this.docID == null)
            return null;

        StringBuilder sb = new StringBuilder("token-");

        // Get prefix string text sigle
		if (this.textSigle != null) {
			sb.append(this.textSigle);
		}
        // Get prefix string corpus/doc
		else {
			// <legacy>
			if (this.corpusID != null) {
				sb.append(this.corpusID).append('!');
			};
			sb.append(this.docID);
			// </legacy>
		};

        sb.append("-p");
        sb.append(this.getStart());
		if (this.getEnd() != -1)
			sb.append("-").append(this.end);

        return sb.toString();
    };
};
