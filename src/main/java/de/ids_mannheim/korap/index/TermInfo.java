package de.ids_mannheim.korap.index;

import java.util.*;
import java.nio.ByteBuffer;
import java.lang.StringBuffer;
import java.util.regex.*;
import de.ids_mannheim.korap.response.Match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TermInfo implements Comparable<TermInfo> {

    // Logger
    private final static Logger log = LoggerFactory.getLogger(Match.class);
    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;


    // TODO: Support various terms - including relations!

    private String foundry, layer, value, term, type, annotation;
    // type can be "term", "pos", "span", "rel-src", "rel-target"

    private int pos = 0;
    private ByteBuffer payload;
    private boolean analyzed = false;

    private int startChar = -1, endChar = -1, startPos = -1, endPos = -1;

    private byte depth = (byte) 0;

    private Pattern prefixRegex = Pattern
        .compile("(?:([^/]+)/)?([^:/]+)(?::(.+?))?");
    private Matcher matcher;


    public TermInfo (String term, int pos, ByteBuffer payload) {
        this.term = term;
        this.startPos = pos;
        this.endPos = pos;
        this.payload = payload;
    };


    public TermInfo analyze () {
        if (analyzed)
            return this;

        int ttype = 0;
        String tterm = this.term;
        int lastPos = this.payload.position();
        this.payload.rewind();

        // TODO: Use PTI!
        switch (tterm.charAt(0)) {
        case '<':
            // "<>:mate/l:..."
            if (tterm.charAt(1) == '>') {
                // span
                this.type = "span";
                tterm = tterm.substring(3);
                ttype = 2;
            }
            // rel-target
            else {
                this.type = "relTarget";
                tterm = tterm.substring(2);
                ttype = 3;
            }
            ;
            break;

        case '>':
            // rel-src
            this.type = "relSrc";
            tterm = tterm.substring(2);
            ttype = 3;
            break;

        case '_':
            // pos
            this.type = "pos";
            ttype = 1;
            tterm = tterm.substring(1);
            break;

        default:
            // term
            this.type = "term";
        };

        // Analyze term value
        if (ttype != 1) {

            this.payload.get(); // Ignore PTI - temporary!!!

            if (DEBUG)
                log.trace("Check {} for {}", tterm, prefixRegex.toString());
            matcher = prefixRegex.matcher(tterm);
            if (matcher.matches() && matcher.groupCount() == 3) {
                this.annotation = tterm;
                if (matcher.group(1) != null)
                    this.foundry = matcher.group(1);
                else
                    this.foundry = "base";
                this.layer = matcher.group(2);
                this.value = matcher.group(3);
            };
        }

        // for positions
        else {
            this.value = tterm;
            this.startChar = this.payload.getInt();
            this.endChar = this.payload.getInt();
        };

        // for spans
        if (ttype == 2) {
            this.startChar = this.payload.getInt();
            this.endChar = this.payload.getInt();
        };

        // for spans and relations
        if (ttype > 1) {
            if (this.type.equals("relTarget")) {
                this.endPos = this.startPos;
                this.startPos = this.payload.getInt() - 1;
            }
            else {
                this.endPos = this.payload.getInt() - 1;
            };
        };

        // Ignore link id for the moment
        if (ttype == 2 && this.payload.position() < lastPos) {
            this.depth = this.payload.get();
        };

        // payloads can have different meaning
        analyzed = true;
        return this;
    };


    public String getType () {
        return this.type;
    };


    public int getStartChar () {
        return this.startChar;
    };


    public void setStartChar (int pos) {
        this.startChar = pos;
    };


    public int getEndChar () {
        return this.endChar;
    };


    public void setEndChar (int pos) {
        this.endChar = pos;
    };


    public int getStartPos () {
        return this.startPos;
    };


    public int getEndPos () {
        return this.endPos;
    };


    public byte getDepth () {
        return this.depth;
    };


    public String getFoundry () {
        return this.foundry;
    };


    public String getLayer () {
        return this.layer;
    };


    public String getValue () {
        return this.value;
    };


    public String getAnnotation () {
        return this.annotation;
    };


    public String toString () {
        this.analyze();

        StringBuffer sb = new StringBuffer();
        sb.append('<').append(this.getType()).append('>');
        sb.append(this.getFoundry()).append('/').append(this.getLayer());

        if (this.getValue() != null)
            sb.append(':').append(this.getValue());

        if (this.getDepth() != (byte) 0)
            sb.append('(').append(this.getDepth()).append(')');

        sb.append('[').append(this.getStartPos());
        sb.append('-').append(this.getEndPos()).append(']');
        sb.append('[').append(this.getStartChar());
        sb.append('-').append(this.getEndChar()).append(']');

        return sb.toString();
    };


    @Override
        public int compareTo (TermInfo obj) {
        this.analyze();
        obj.analyze();

        // TODO: This sorting does not seem to work!
        // although it might only be important for depth stuff.

        if (this.startChar < obj.startChar) {
            return -1;
        }
        else if (this.startChar > obj.startChar) {
            return 1;
        }
        else if (this.depth < obj.depth) {
            return 1;
        }
        else if (this.depth > obj.depth) {
            return -1;
        };
        return 0;
    };
};
