package de.ids_mannheim.korap.index;

import java.util.*;
import org.apache.lucene.util.BytesRef;

public class TermInfo {

    private String prefix, foundry, layer, value;
    private int pos = 0;
    private BytesRef payload;

    // Temporary:
    private String name;

    public TermInfo (String name, int pos, BytesRef payload) {
	this.name = name;
	this.pos = pos;
	this.payload = payload;
    };
};
