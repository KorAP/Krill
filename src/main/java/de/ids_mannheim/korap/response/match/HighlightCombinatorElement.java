package de.ids_mannheim.korap.response.match;

import org.apache.lucene.util.FixedBitSet;
import de.ids_mannheim.korap.response.Match;
import static de.ids_mannheim.korap.util.KrillString.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
  Class for elements with highlighting information
*/
public class HighlightCombinatorElement {

	// Number -1:     Match
	// Number -99997: Context
	private final static int CONTEXT = -99997;
	
    // Type 0: Textual data
    // Type 1: Opening
    // Type 2: Closing
	// Type 3: Empty (pagebreak)
    // Type 4: Empty (marker)
    public byte type;

    public int number = 0;

    public String characters;
    public boolean terminal = true;

	// Logger
	private final static Logger log = LoggerFactory.getLogger(Match.class);

	// This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    // Constructor for highlighting elements
    public HighlightCombinatorElement (byte type, int number) {
        this.type = type;
        this.number = number;
    };


    // Constructor for highlighting elements,
    // that may not be terminal, i.e. they were closed and will
    // be reopened for overlapping issues.
    public HighlightCombinatorElement (byte type, int number,
                                       boolean terminal) {
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
	public String toHTML (Match match, FixedBitSet level, byte[] levelCache, HashSet joins) {

        // Opening
        if (this.type == 1) {
            StringBuilder sb = new StringBuilder();

            // This is the surrounding match mark
            if (this.number == -1) {
                sb.append("<mark>");
            }

			// This is context
			else if (this.number == CONTEXT) {
				// DO nothing
			}

			// This is a relation target
            else if (this.number < -1) {

				// Create id
				String id = escapeHTML(
					match.getPosID(match.getClassID(this.number))
					);

				// ID already in use - create join
				if (joins.contains(id)) {
					sb.append("<span xlink:show=\"other\" data-action=\"join\" xlink:href=\"#")
						.append(id)
						.append("\">");
				}

				// Not yet in use - create
				else {
					sb.append("<span xml:id=\"")
						.append(id)
						.append("\">");
					joins.add(id);
				};
            }

			// This is an annotation
            else if (this.number >= 256) {
                sb.append("<span ");
                if (this.number < 2048) {
                    sb.append("title=\"")
                            .append(escapeHTML(
                                    match.getAnnotationID(this.number)))
                            .append('"');
                }

				// This is a relation source
                else {
                    Relation rel = match.getRelationID(this.number);

					if (DEBUG) {
						log.trace("Annotation is a relation with id {}", this.number);
						log.trace("Resulting in relation {}: {}-{}", rel.annotation, rel.refStart, rel.refEnd);
					};

                    sb.append("xlink:title=\"")
						.append(escapeHTML(rel.annotation))
						.append("\" xlink:show=\"none\" xlink:href=\"#")
						.append(escapeHTML(match.getPosID(rel.refStart, rel.refEnd)))
						.append('"');
                };
                sb.append('>');
            }

            // This is a highlight
			// < 256
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

        // This is a Closing tag
        else if (this.type == 2) {
			if (this.number == CONTEXT)
				return "";
			
            if (this.number < -1 || this.number >= 256)
                return "</span>";

            if (this.number == -1)
                return "</mark>";

            if (this.terminal)
                level.set((int) levelCache[this.number]);
            return "</mark>";
        }

		// Empty element
		else if (this.type == 3) {
			return "<span class=\"pb\" data-after=\"" + number + "\"></span>";
		}
        
        // Marker
		else if (this.type == 4) {
            String[] parts = match.getAnnotationID(this.number).split(":", 2);
			return "<span class=\"inline-marker\" data-key=\"" + escapeHTML(parts[0]) + "\" data-value=\"" + escapeHTML(parts[1]) + "\"></span>";
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

			// This is context
			else if (this.number == CONTEXT) {
				// DO nothing
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
                        sb.append('>').append(rel.refStart);

						if (rel.refEnd != -1)
							sb.append('-').append(rel.refEnd);
                    };
                    sb.append(':');
                }
                else if (this.number != 0)
                    sb.append(this.number).append(':');
            };

            return sb.toString();
        }

        else if (this.type == 3) {
            StringBuilder sb = new StringBuilder();
            sb.append("{%");
            sb.append(this.number);
            sb.append("}");
            return sb.toString();
        }

        else if (this.type == 4) {
            String[] parts = match.getAnnotationID(this.number).split(":", 2);
            StringBuilder sb = new StringBuilder();
            sb.append("{*");
            sb.append(escapeBrackets(parts[0]));
            sb.append("=");
            sb.append(escapeBrackets(parts[1]));
            sb.append("}");
            return sb.toString();
        }

        else if (this.type == 2) {

			// This is context
			if (this.number == CONTEXT)
				return "";
			
            if (this.number == -1)
                return "]";
            return "}";
        };

        if (this.characters == null) {
            return "";
        };
        return escapeBrackets(this.characters);
    };
};
