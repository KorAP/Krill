package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillIndex;


@RunWith(JUnit4.class)
public class TestPosToOffset {

    @Test
    public void indexExample1 () throws IOException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "a b c", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>3]"
                + "[(2-3)s:b|i:b|_1$<i>2<i>3]" + "[(4-5)s:c|i:c|_2$<i>4<i>5]");
        ki.addDoc(fd);

        fd = new FieldDocument();
        fd.addTV("base", "x  y  z", "[(0-1)s:x|i:x|_0$<i>0<i>2|-:t$<i>3]"
                + "[(3-4)s:y|i:y|_1$<i>3<i>4]" + "[(6-7)s:z|i:z|_2$<i>6<i>7]"); // 3
        ki.addDoc(fd);
        ki.commit();

        String field = "base";
        for (LeafReaderContext atomic : ki.reader().leaves()) {
            PositionsToOffset pto = new PositionsToOffset(atomic, field);

            pto.add(0, 1);
            pto.add(0, 2);
            pto.add(1, 2);
            pto.add(1, 1);
            pto.add(1, 20);

            assertEquals("Start 0,1", pto.start(0, 1), 2);
            assertEquals("End 0,1", pto.end(0, 1), 3);

            assertEquals("Start 0,2", pto.start(0, 2), 4);
            assertEquals("End 0,2", pto.end(0, 2), 5);

            assertEquals("Start 1,2", pto.start(1, 2), 6);
            assertEquals("End 1,2", pto.end(1, 2), 7);

            assertEquals("Start 1,1", pto.start(1, 1), 3);
            assertEquals("End 1,1", pto.end(1, 1), 4);

            assertEquals("Start 1,20", pto.start(1, 20), 0);
            assertEquals("End 1,20", pto.end(1, 20), -1);
        };
    };
};
