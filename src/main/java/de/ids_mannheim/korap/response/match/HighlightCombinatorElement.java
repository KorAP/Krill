package de.ids_mannheim.korap.response.match;

import org.apache.lucene.util.FixedBitSet;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.match.Relation;
import static de.ids_mannheim.korap.util.KrillString.*;
import java.util.*;
import java.io.*;

/*
  Class for elements with highlighting information
*/
public class HighlightCombinatorElement {

    // Type 0: Textual data
    // Type 1: Opening
    // Type 2: Closing
    public byte type;

    public int number = 0;

    public String characters;
    public boolean terminal = true;


    // Constructor for highlighting elements
    public HighlightCombinatorElement (byte type, int number) {
        this.type = type;
        this.number = number;
    };


    // Constructor for highlighting elements,
    // that may not be terminal, i.e. they were closed and will
    // be reopened for overlapping issues.
    public HighlightCombinatorElement (byte type, int number, boolean terminal) {
        this.type = type;
        this.number = number;
        this.terminal = terminal;
    };


    // Constructor for textual data
    public HighlightCombinatorElement (String characters) {
        this.type = (byte) 0;
        this.characters = characters;
    };


    // Return html fragment for this combinator element
    public String toHTML (Match match, FixedBitSet level, byte[] levelCache) {
        // Opening
        if (this.type == 1) {
            StringBuilder sb = new StringBuilder();

	    // This is the surrounding match mark
            if (this.number == -1) {
                sb.append("<mark>");
            }

            else if (this.number < -1) {
                sb.append("<span xml:id=\"")
                    .append(escapeHTML(match.getPosID(match.getClassID(this.number))))
                        .append("\">");
            }

            else if (this.number >= 256) {
                sb.append("<span ");
                if (this.number < 2048) {
                    sb.append("title=\"")
                        .append(escapeHTML(match.getAnnotationID(this.number)))
                            .append('"');
                }
                else {
                    Relation rel = match.getRelationID(this.number);
                    sb.append("xlink:title=\"").append(escapeHTML(rel.annotation))
                            .append("\" xlink:type=\"simple\" xlink:href=\"#")
                        .append(escapeHTML(match.getPosID(rel.ref))).append('"');
                };
                sb.append('>');
            }

            // Highlight - < 256
            else {
                // Get the first free level slot
                byte pos;
                if (levelCache[this.number] != '\0') {
                    pos = levelCache[this.number];
                }
                else {
                    pos = (byte) level.nextSetBit(0);
                    level.clear(pos);
                    levelCache[this.number] = pos;
                };
                sb.append("<mark class=\"class-").append(this.number)
                        .append(" level-").append(pos).append("\">");
            };
            return sb.toString();
        }
        // Closing
        else if (this.type == 2) {
            if (this.number < -1 || this.number >= 256)
                return "</span>";

            if (this.number == -1)
                return "</mark>";

            if (this.terminal)
                level.set((int) levelCache[this.number]);
            return "</mark>";
        };

        // HTML encode primary data
        return escapeHTML(this.characters);
    };


    // Return bracket fragment for this combinator element
    public String toBrackets (Match match) {
        if (this.type == 1) {
            StringBuilder sb = new StringBuilder();

            // Match
            if (this.number == -1) {
                sb.append("[");
            }

            // Identifier
            else if (this.number < -1) {
                sb.append("{#");
                sb.append(match.getClassID(this.number));
                sb.append(':');
            }

            // Highlight, Relation, Span
            else {
                sb.append("{");
                if (this.number >= 256) {
                    if (this.number < 2048)
                        sb.append(match.getAnnotationID(this.number));
                    else {
                        Relation rel = match.getRelationID(this.number);
                        sb.append(rel.annotation);
                        sb.append('>').append(rel.ref);
                    };
                    sb.append(':');
                }
                else if (this.number != 0)
                    sb.append(this.number).append(':');
            };

            return sb.toString();
        }
        else if (this.type == 2) {
            if (this.number == -1)
                return "]";
            return "}";
        };
        return escapeBrackets(this.characters);
    };
};
