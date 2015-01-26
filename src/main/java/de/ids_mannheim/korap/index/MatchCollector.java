package de.ids_mannheim.korap.index;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.response.KorapResponse;
import java.util.*;

public class MatchCollector extends KorapResponse {
    public int totalResultDocs = 0;
    /*
      private int totalResults;
      private long totalTexts;
    */

    public void add (int uniqueDocID, int matchcount) {
        this.totalResultDocs++;
        this.incrTotalResults(matchcount);
    };

    public MatchCollector setTotalResultDocs (int i) {
        this.totalResultDocs = i;
        return this;
    };

    public MatchCollector incrTotalResultDocs (int i) {
        this.totalResultDocs += i;
        return this;
    };

    public int getTotalResultDocs () {
        return totalResultDocs;
    };

    public void commit () {};

    public void close () {};

    /*
     * The following methods are shared and should be used from KorapResult
     * And:
     * getQueryHash
     * getNode
     */
};
