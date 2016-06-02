package de.ids_mannheim.korap.query.spans;

import java.util.Comparator;

public class CandidateSpanComparator implements Comparator<CandidateSpan> {

    @Override
    public int compare (CandidateSpan o1, CandidateSpan o2) {
        if (o1.doc == o2.doc) {
            if (o1.getStart() == o2.getStart()) {
                if (o1.getEnd() == o2.getEnd())
                    return 0;
                if (o1.getEnd() > o2.getEnd())
                    return 1;
                else
                    return -1;
            }
            else if (o1.getStart() < o2.getStart())
                return -1;
            else
                return 1;
        }
        else if (o1.doc < o2.doc)
            return -1;
        else
            return 1;
    }
}
