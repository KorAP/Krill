package de.ids_mannheim.korap.response.match;

import java.util.*;

public class PosIdentifier extends DocIdentifier {
    private int pos;

    public PosIdentifier () {};


    public void setPos (int pos) {
        if (pos >= 0)
            this.pos = pos;
    };


    public int getPos () {
        return this.pos;
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
        sb.append(this.pos);

        return sb.toString();
    };
};
