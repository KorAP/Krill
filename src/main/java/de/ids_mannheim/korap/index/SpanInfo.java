package de.ids_mannheim.korap.index;
import de.ids_mannheim.korap.index.TermInfo;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.index.PositionsToOffset;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SpanInfo {
    ArrayList<TermInfo> terms;
    HashMap<Integer,Integer> startChar, endChar;
    PositionsToOffset pto;
    int localDocID;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(Match.class);
    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    public SpanInfo (PositionsToOffset pto, int localDocID) {
        this.terms      = new ArrayList<TermInfo>(64);
        this.startChar  = new HashMap<Integer,Integer>(16);
        this.endChar    = new HashMap<Integer,Integer>(16);
        this.pto        = pto;
        this.localDocID = localDocID;
    };

    public void add (TermInfo info) {
        info.analyze();
        if (info.getType() != "pos") {
            this.terms.add(info);
        }
        else {
            this.startChar.put(info.getStartPos(), info.getStartChar());
            this.endChar.put(info.getEndPos(), info.getEndChar());
        };
    };

    public ArrayList<TermInfo> getTerms () {
        // Sort terms (this will also analyze them!)
        Collections.sort(this.terms);
        boolean found;

        // Add character offset information to terms that are
        // missing this information
        for (TermInfo t : this.terms) {
            if (DEBUG)
                log.trace("Check offsets for {} and {}", t.getStartPos(), t.getEndPos());
            found = true;
            if (t.getStartChar() == -1) {
                if (this.startChar.containsKey(t.getStartPos()))
                    t.setStartChar(this.startChar.get(t.getStartPos()));
                else
                    found = false;
            }
            if (t.getEndChar() == -1) {
                if (this.endChar.containsKey(t.getEndPos()))
                    t.setEndChar(this.endChar.get(t.getEndPos()));
                else
                    found = false;
            };
            
            // Add this to found offsets
            if (found && t.getStartPos() == t.getEndPos())
                this.pto.addOffset(
                    this.localDocID,
                    t.getStartPos(),
                    t.getStartChar(),
                    t.getEndChar()
                );
            else {
                if (DEBUG)
                    log.trace("{} can't be found!", t.getAnnotation());
                this.pto.add(this.localDocID, t.getStartPos());
                this.pto.add(this.localDocID, t.getStartPos());
            };
        };

        return this.terms;
    };
};
