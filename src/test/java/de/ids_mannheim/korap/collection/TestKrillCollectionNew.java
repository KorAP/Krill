package de.ids_mannheim.korap.collection;

import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.collection.CollectionBuilderNew;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class TestKrillCollectionNew {

    @Test
    public void builderConstruction () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("", kc.toString());
    };

    @Test
    public void builderRegex () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("QueryWrapperFilter(author:/tre*?/)",
                     kc.re("author", "tre*?").toString());
    };

    @Test
    public void builderTerm () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        assertEquals("author:tree",
                     kc.term("author", "tree").toString());
    };

    @Test
    public void builderAndSimple () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        kc.and(kc.term("author", "tree"));
        assertEquals("author:tree", kc.toString());
    };

    @Test
    public void builderAndCombined () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        kc.and(kc.term("author", "tree")).and(kc.term("title", "name"));
        assertEquals("BooleanFilter(+author:tree +title:name)", kc.toString());
    };

    @Test
    public void builderAndNestedSimple () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        kc.and(new CollectionBuilderNew().and(kc.term("author", "tree")).and(kc.term("title", "name")));
        assertEquals("BooleanFilter(+author:tree +title:name)", kc.toString());
    };

    @Test
    public void builderOrSimple () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        kc.or(kc.term("author", "tree"));
        assertEquals("author:tree", kc.toString());
    };


    @Test
    public void builderOrCombined () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        kc.or(kc.term("author", "tree")).or(kc.term("title", "name"));
        assertEquals("BooleanFilter(author:tree title:name)", kc.toString());
    };

};
