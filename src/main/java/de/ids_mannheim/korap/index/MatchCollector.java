package de.ids_mannheim.korap.index;
import de.ids_mannheim.korap.KorapMatch;
import java.util.*;

public interface MatchCollector {
    public void add (int uniqueDocID, int matchcount);

    /*
     * The following methods are shared and should be used from KorapResult
     * And:
     * getQueryHash
     * getNode
     */

    public void setError(String s);
    public void setBenchmarkHitCounter(long t1, long t2);
    public int getMatchCount ();
    public int getDocumentCount ();
    public String toJSON();
    public void commit();
};
