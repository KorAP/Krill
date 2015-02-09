package de.ids_mannheim.korap.model;

import java.util.*;
import de.ids_mannheim.korap.model.MultiTermToken;
import de.ids_mannheim.korap.model.MultiTermTokenStream;
import de.ids_mannheim.korap.util.CorpusDataException;
import java.io.IOException;
import org.apache.lucene.util.BytesRef;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author diewald
 */

@RunWith(JUnit4.class)
public class TestMultiTermTokenStream {

    @Test
    public void multiTermTokenStreamSimple () throws CorpusDataException {
        MultiTermTokenStream ts = new MultiTermTokenStream();

        MultiTermToken mtt = new MultiTermToken("a:b#0-2");
        mtt.add("b:c");
        ts.addMultiTermToken(mtt);
        mtt = new MultiTermToken('c', "d");
        mtt.add('d', "e");
        ts.addMultiTermToken(mtt);
        assertEquals("[a:b#0-2|b:c]"+
                     "[c:d|d:e]",
                     ts.toString());

        ts = new MultiTermTokenStream();

        mtt = new MultiTermToken('s', "Er#0-2");
        mtt.add('i', "er");
        mtt.add('p', "PPER");
        mtt.add('l', "er");
        mtt.add('m', "c:nom");
        mtt.add('m', "p:3");
        mtt.add('m', "n:sg");
        mtt.add('m', "g:masc");
        ts.addMultiTermToken(mtt);

        mtt = new MultiTermToken('s', "nahm");
        mtt.add('i', "nahm");
        mtt.add('p', "VVFIN$stts");
        mtt.add('l', "nehmen");
        mtt.add('m', "p:3#3-7");
        mtt.add('m', "n:sg");
        mtt.add('m', "t:past");
        mtt.add('m', "m:ind");
        ts.addMultiTermToken(mtt);

        assertEquals("[s:Er#0-2|i:er|p:PPER|l:er|"+
                     "m:c:nom|m:p:3|m:n:sg|m:g:masc]"+
                     "[s:nahm|i:nahm|p:VVFIN$stts|"+
                     "l:nehmen|m:p:3#3-7|m:n:sg|m:t:past|m:m:ind]",
                     ts.toString());

        ts.addMeta("paragraphs", 4);
        ts.addMeta("sentences", "34");

        assertEquals("[s:Er#0-2|i:er|p:PPER|l:er|m:c:nom|"+
                     "m:p:3|m:n:sg|m:g:masc|"+
                     "-:paragraphs$   |-:sentences$34]"+
                     "[s:nahm|i:nahm|p:VVFIN$stts|l:nehmen|"+
                     "m:p:3#3-7|m:n:sg|m:t:past|m:m:ind]",
                     ts.toString());

        ts = new MultiTermTokenStream(
          "[s:den#0-3|i:den|p:DET|l:der|m:c:acc|m:n:sg|m:masc]"
        );

        assertEquals(
            "[s:den#0-3|i:den|p:DET|l:der|m:c:acc|m:n:sg|m:masc]",
            ts.toString()
        );
    };


    @Test
    public void multiTermTokenStreamComplex () throws CorpusDataException {
        MultiTermTokenStream ts = new MultiTermTokenStream(
            "[(0-3)s:den#0-3|i:den|p:DET|l:der|m:c:acc|m:n:sg|m:masc]" +
            "[(3-5)s:der#3-5|i:die|p:DET|l:die|m:c:gen|m:n:sg|m:fem]"
        );

        assertEquals(
                     "[s:den#0-3|i:den|p:DET|l:der|m:c:acc|m:n:sg|m:masc]",
                     ts.get(0).toString()
                     );

        assertEquals(
                     "[s:der#3-5|i:die|p:DET|l:die|m:c:gen|m:n:sg|m:fem]",
                     ts.get(1).toString()
                     );

        assertEquals(2, ts.getSize());
        assertEquals("[s:den#0-3|i:den|p:DET|l:der|m:c:acc|m:n:sg|m:masc]"+
                     "[s:der#3-5|i:die|p:DET|l:die|m:c:gen|m:n:sg|m:fem]",
                     ts.toString());
    };

    @Test
    public void multiTermTokenStreamSort () throws CorpusDataException {
        MultiTermTokenStream ts = new MultiTermTokenStream(
            "[(0-3)s:h|<>:a#0-27$<i>6|<>:a#0-18$<i>3|<>:a#0-36$<i>9]" +
            "[(3-6)s:h]" +
            "[(12-15)s:i]" +
            "[(15-18)s:j]" +
            "[(18-21)s:h]" +
            "[(21-24)s:i]" +
            "[(24-27)s:j]" +
            "[(27-30)s:h]" +
            "[(30-33)s:i]" +
            "[(33-36)s:j]");

        MultiTermToken mtt = ts.get(0);
        assertEquals(18, mtt.get(1).end);
        assertEquals(27, mtt.get(2).end);
        assertEquals(36, mtt.get(3).end);
    };

    @Test
    public void multiTermTokenStreamFailingPayload () throws CorpusDataException {

        // The payload should be ignored
        MultiTermTokenStream ts = new MultiTermTokenStream(
            "[(0-3)s:h|<>:a#0-27$<i>6|<>:a#0-18<i>3|<>:a#0-36$<i>9]" +
            "[(3-6)s:h]" +
            "[(12-15)s:i]" +
            "[(15-18)s:j]" +
            "[(18-21)s:h]" +
            "[(21-24)s:i]" +
            "[(24-27)s:j]" +
            "[(27-30)s:h]" +
            "[(30-33)s:i]" +
            "[(33-36)s:j]");

        MultiTermToken mtt = ts.get(0);
        assertEquals(3, mtt.getSize());
        assertEquals(27, mtt.get(1).end);
        assertEquals(36, mtt.get(2).end);
    };

};
