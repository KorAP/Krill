package de.ids_mannheim.korap.response.match;

/**
 * Class for relational highlights.
 */   
public class Relation {
    public int ref;
    public String annotation;
    public Relation (String annotation, int ref) {
        this.annotation = annotation;
        this.ref = ref;
    };
};
