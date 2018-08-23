package de.ids_mannheim.korap.constants;

public enum RelationDirection {
    LEFT("<:"), RIGHT(">:");

    private String value;


    RelationDirection (String value) {
        this.value = value;
    }


    public String value () {
        return value;
    }
}
