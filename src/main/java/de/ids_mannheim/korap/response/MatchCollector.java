package de.ids_mannheim.korap.response;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Response;
import java.util.*;

public class MatchCollector extends Response {
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
     * The following methods are shared and should be used from Result
     * And:
     * getQueryHash
     * getNode
     */
};
