package de.ids_mannheim.korap.index;

import java.util.*;

public class TermInfo {

    private String prefix, foundry, layer, value;
    private int pos = 0;
    private byte[] payload;

    // Temporary:
    private String name;

    public TermInfo (String name, int pos, byte[] payload) {
	this.name = name;
	this.pos = pos;
	this.payload = payload;
    };
};
