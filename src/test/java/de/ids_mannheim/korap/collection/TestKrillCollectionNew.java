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
        kc.and(kc.create().and(kc.term("author", "tree")).and(kc.term("title", "name")));
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

    @Test
    public void builderOrNestedSimple () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        kc.or(kc.create().or(kc.term("author", "tree")).or(kc.term("title", "name")));
        assertEquals("BooleanFilter(author:tree title:name)", kc.toString());
    };

    @Test
    public void builderGroups () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        kc.or(
              kc.create().or(kc.term("author", "tree1")).or(kc.term("title", "name1"))
        ).or(
              kc.create().and(kc.term("author", "tree2")).and(kc.term("title", "name2"))
        );
        assertEquals("BooleanFilter(BooleanFilter(author:tree1 title:name1) BooleanFilter(+author:tree2 +title:name2))", kc.toString());
    };

    @Test
    public void builderNegationRoot () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        kc.or(kc.term("author", "tree1")).or(kc.term("title", "name1"));
        assertEquals("BooleanFilter(author:tree1 title:name1)", kc.toString());
        assertFalse(kc.isNegative());

        kc = new CollectionBuilderNew();
        kc.andNot(
              kc.create().or(kc.term("author", "tree1")).or(kc.term("title", "name1"))
        );
        assertEquals("BooleanFilter(author:tree1 title:name1)", kc.toString());
        assertTrue(kc.isNegative());
    };

    @Test
    public void builderNegation () throws IOException {
        CollectionBuilderNew kc = new CollectionBuilderNew();
        kc.andNot(kc.term("author", "tree"));
        assertEquals("author:tree", kc.toString());
        assertTrue(kc.isNegative());

        kc = kc.create();
        kc.orNot(kc.term("author", "tree"));
        assertEquals("author:tree", kc.toString());
        assertTrue(kc.isNegative());

        /*
        kc = kc.create();
        // and-group of nots!
        // Todo: Use orNot!!!
        kc.not(kc.term("author", "tree")).not(kc.term("title", "name1"));
        assertEquals("BooleanFilter(+author:tree +title:name1)", kc.toString());
        assertTrue(kc.isNegative());

        kc = kc.create();
        kc.not(kc.term("author", "tree")).or(kc.term("title", "name1"));
        assertEquals("BooleanFilter(-author:tree title:name1)", kc.toString());
        assertFalse(kc.isNegative());
        */
    };
};
