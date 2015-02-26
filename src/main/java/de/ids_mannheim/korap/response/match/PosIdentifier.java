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
        if (this.docID == null) return null;

        StringBuilder sb = new StringBuilder("word-");

        // Get prefix string corpus/doc
        if (this.corpusID != null) {
            sb.append(this.corpusID).append('!');
        };
        sb.append(this.docID);

        sb.append("-p");
        sb.append(this.pos);

        return sb.toString();
    };
};
