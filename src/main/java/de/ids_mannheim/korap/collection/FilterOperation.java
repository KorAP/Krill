package de.ids_mannheim.korap.collection;

import org.apache.lucene.search.Filter;

public class FilterOperation {
    private boolean extension;
    public Filter filter;
    
    public FilterOperation (Filter filter, boolean extension) {
	this.extension = extension;
	this.filter = filter;
    };

    public boolean isExtension () {
	return this.extension;
    };

    public boolean isFilter () {
	return !(this.extension);
    };

    @Override
    public Object clone () throws CloneNotSupportedException {
	return (Object) new FilterOperation(this.filter, this.extension);
    };

    @Override
    public String toString () {
	StringBuilder sb = new StringBuilder();
	if (this.extension) {
	    sb.append("extend with ");
	}
	else {
	    sb.append("filter with ");
	};
	sb.append(this.filter.toString());
	return sb.toString();
    };	
};
