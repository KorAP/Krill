package de.ids_mannheim.korap.response.match;

/**
 * Class for relational highlights.
 */
public class Relation {
    public int refStart;
	public int refEnd;
    public String annotation;

    public Relation (String annotation, int refStart, int refEnd) {
        this.annotation = annotation;
        this.refStart = refStart;
        this.refEnd = refEnd;
    };
};
