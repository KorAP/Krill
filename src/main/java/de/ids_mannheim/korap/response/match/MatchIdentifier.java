package de.ids_mannheim.korap.response.match;

import java.util.*;
import java.util.regex.*;
import java.util.Base64;
import com.google.crypto.tink.subtle.Hex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.TinkJsonProtoKeysetFormat;
import com.google.crypto.tink.aead.AeadConfig;

import java.security.GeneralSecurityException;

import de.ids_mannheim.korap.util.KrillProperties;

public class MatchIdentifier extends DocIdentifier {
    private int startPos, endPos = -1;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(MatchIdentifier.class);
    
    private ArrayList<int[]> pos = new ArrayList<>(8);

    String idRegexPos = "p([0-9]+)-([0-9]+)"
        + "((?:\\(-?[0-9]+\\)-?[0-9]+--?[0-9]+)*)"
        + "(?:c.+?)?";
    
    // Remember: "contains" is necessary for a compatibility bug in Kustvakt
	// Identifier pattern is "match-
    Pattern idRegex = Pattern.compile("^(?:match-|contains-)"
									  + "(?:([^!]+?)[!\\.])?"
									  + "([^!]+)[-/]"
                                      + idRegexPos
                                      + "$");


    
    Pattern idRegexCompat = Pattern.compile("^(?:match-|contains-)"
                                            + "(?:([^!]+?)[!\\.])?"
                                            + "([^!]+)[-/]"
                                            + "(?:"
                                            // Not encrypted
                                            + idRegexPos
                                            +   "|"
                                            // Encrypted
                                            +   "x(.+?)"                                           
                                            + ")"
                                            + "$");

    Pattern idRegexDecrypt = Pattern.compile("^"
                                             + "([^:]+?)" // TextSigle
                                             + "(::)"     // Separator
                                             + idRegexPos
                                             + "$");

    
    Pattern posRegex = Pattern.compile("\\(([0-9]+)\\)([0-9]+)-([0-9]+)");
    
    private Aead aead = null;

        {
            // Load the secret key from the properties file
            Properties prop = KrillProperties.loadDefaultProperties();

            // The secret is only fix, if the matchIDs need to be treated as
            // persistant identifiers, otherwise it only needs to be stable temporarily
            String secretKey = prop.getProperty("krill.secretB64");

            if (secretKey != null) {
                
                try {
            
                    // Register Tink configurations
                    AeadConfig.register();

                
                    // Decode the base64-encoded secret key
                    byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);

                    // Read the keyset into a KeysetHandle.
                    KeysetHandle keysetHandle =
                        TinkJsonProtoKeysetFormat.parseKeyset(
                            new String(secretKeyBytes, UTF_8), InsecureSecretKeyAccess.get());
            
                    // Initialize the AEAD primitive
                    aead = keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead.class);

                } catch (GeneralSecurityException e) {
                    log.error("Can't initialize match id encryption: {}", e);
                };
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

        Matcher matcher = idRegexCompat.matcher(id);
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

            if (aead != null) {
                try {
                    String matchid = decrypt(matcher.group(3));
                    matcher = idRegexDecrypt.matcher(id);

                    String textSigleCheck = matcher.group(1);

                    // Ignore group 2!

                    // Needs to contain the textSigle, so the encrypted position
                    // Can't be reused for another match
                    if (this.getTextSigle().equals(textSigleCheck))
                        return;
                }
                catch (GeneralSecurityException e) {
                    return;
                };
                // Check that textSigle matches
            }

            // No en/decryption support
            this.setStartPos(Integer.parseInt(matcher.group(3)));
            this.setEndPos(Integer.parseInt(matcher.group(4)));

            if (matcher.group(5) != null) {
                matcher = posRegex.matcher(matcher.group(5));
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

        // Add matchID with fallback.
        // This should be changed, once matchid encryption is an established standard!
        if (aead != null) {
            try {
                // Encryption marker
                sb.append('x')
                    .append(
                    encrypt(
                        this.getPositionString()
                        + "::"
                        + this.getTextSigle()
                        )
                    );
            }
            catch (GeneralSecurityException e) {
                return null;
            };
        } else {
            sb.append(this.getPositionString());
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
    
    public String encrypt(String plaintext) throws GeneralSecurityException {
        if (aead == null)
            return null;
        
        byte[] ciphertext = aead.encrypt(plaintext.getBytes(), null);
        return Hex.encode(ciphertext);
    };

    public String decrypt(String ciphertext) throws GeneralSecurityException {
        if (aead == null)
            return null;

        byte[] plaintext = aead.decrypt(Hex.decode(ciphertext), null);
        return new String(plaintext);
    };
};
