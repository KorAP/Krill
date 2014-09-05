package de.ids_mannheim.korap.index;
import de.ids_mannheim.korap.KorapMatch;

public interface MatchCollector {
    public int add (KorapMatch km);

    /*
     * The following methods are shared and should be used from KorapResult
     * And:
     * getQueryHash
     * getNode
     */

    public void setError(String s);
    public void setBenchmarkHitCounter(long t1, long t2);
    public String getBenchmarkHitCounter();


    public int getMatchCount ();
    public int getDocumentCount ();
    public String toJSON();
};
