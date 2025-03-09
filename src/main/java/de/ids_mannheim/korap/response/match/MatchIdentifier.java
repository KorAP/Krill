package de.ids_mannheim.korap.response.match;

import java.util.*;
import java.util.regex.*;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import de.ids_mannheim.korap.util.KrillProperties;

public class MatchIdentifier extends DocIdentifier {
    private int startPos, endPos = -1;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(MatchIdentifier.class);
    
    private ArrayList<int[]> pos = new ArrayList<>(8);

    String idRegexPos = "(p([0-9]+)-([0-9]+)"
        + "((?:\\(-?[0-9]+\\)-?[0-9]+--?[0-9]+)*)"
        + "(?:c.+?)?)";
    
    // Remember: "contains" is necessary for a compatibility bug in Kustvakt
	// Identifier pattern is "match-
    Pattern idRegex = Pattern.compile("^(?:match-|contains-)"
									  + "(?:([^!]+?)[!\\.])?"
									  + "([^!]+)[-/]"
                                      + idRegexPos
                                      + "(?:x_([a-zA-Z0-9-_]+?))?"
                                      + "$");
        
    Pattern posRegex = Pattern.compile("\\(([0-9]+)\\)([0-9]+)-([0-9]+)");
    
    private static volatile Mac mac = null;

        {
            if (mac == null) {
                // Load the secret key from the properties file
                Properties prop = KrillProperties.loadDefaultProperties();

                // The secret is only fix, if the matchIDs need to be treated as
                // persistant identifiers, otherwise it only needs to be stable temporarily
                String secretKey = KrillProperties.secret;

                initMac(secretKey);
            };
        };

    public MatchIdentifier () {};


    /**
     * Construct a new MatchIdentifier.
     * Due to lots of internal changes and compatibility reasons,
     * the structure of the identifier has changed a lot.
     * The constructor supports different legacy structures for test
     * compatibility.
     */
    public MatchIdentifier (String id) {
        
        // Replace for legacy reasons with incompatible versions of Kustvakt
        id = id.replaceAll("^(contains-|match-)([^!_\\.]+?)!\\2_", "$1$2_");

        
        Matcher matcher = idRegex.matcher(id);
        if (matcher.matches()) {
            
            // textSigle is provided directly
            if (matcher.group(1) == null && id.contains("/")) {
                // Todo: potentially use UID!
                this.setTextSigle(matcher.group(2));
            }

            // <legacy>
            else if (id.contains("!") || !id.contains("_")) {
                this.setCorpusID(matcher.group(1));
                this.setDocID(matcher.group(2));
            }
            // </legacy>     

            // textSigle is provided indirectly
            // <legacy>
            else {
                this.setTextSigle(matcher.group(1) + '.' + matcher.group(2));
            };
            // </legacy>

            if (mac != null) {
                
                String posString = matcher.group(3);

                String message = this.getTextSigle() + "::" + posString;
                
                String hmacStr = matcher.group(7);

                
                // No signature returned
                if (hmacStr == null) {
                    this.textSigle = "";
                    return;
                };

                byte[] hmacBytes = Base64.getUrlDecoder().decode(hmacStr);

                // Generate the HMAC hash
                byte[] hmacVerify = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
                
                if (!MessageDigest.isEqual(hmacBytes, hmacVerify)) {
                    this.textSigle = "";
                    return;
                };
            };

            this.setStartPos(Integer.parseInt(matcher.group(4)));
            this.setEndPos(Integer.parseInt(matcher.group(5)));

            if (matcher.group(6) != null) {

                matcher = posRegex.matcher(matcher.group(6));
                while (matcher.find()) {
                    this.addPos(Integer.parseInt(matcher.group(2)),
                                Integer.parseInt(matcher.group(3)),
                                Integer.parseInt(matcher.group(1)));
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


    public void addPos (int start, int end, int number) {
        if (start >= 0 && end >= 0 && number >= 0)
            this.pos.add(new int[] { start, end, number });
    };


    public ArrayList<int[]> getPos () {
        return this.pos;
    };


    public String toString () {
        StringBuilder sb = new StringBuilder("match-");

        if (this.docID == null) {
            if (this.textSigle == null)
                return null;

            sb.append(this.textSigle);
        }

        // Get prefix string corpus/doc
        // LEGACY
        else if (this.corpusID != null) {
            sb.append(this.corpusID).append('!').append(this.docID);
        }
        else {
            sb.append(this.docID);
        };

        sb.append('-');

        sb.append(this.getPositionString());

        // Add signature
        if (mac != null) {
            String message = this.getTextSigle() + "::" + this.getPositionString();

            // Generate the HMAC hash
            byte[] hmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            String hmacStr = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hmac);
                
            // Signature marker
            sb.append("x_").append(hmacStr);
        };
        
        return sb.toString();
    };


    public String getPositionString () {
        StringBuilder sb = new StringBuilder();        
        sb.append('p').append(this.startPos).append('-').append(this.endPos);

        // Get Position information
        for (int[] i : this.pos) {
            sb.append('(').append(i[2]).append(')');
            sb.append(i[0]).append('-').append(i[1]);
        };

        return sb.toString();
    };

    public static void initMac(String secretKey) {
        if (secretKey != "") {
            try {
                mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                mac.init(keySpec);
            } catch (Exception e) {
                log.error("Can't initialize match id signing: {}", e);
            };
        } else {
            mac = null;
        };
    };
};
