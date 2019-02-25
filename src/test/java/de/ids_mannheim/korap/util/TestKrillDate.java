package de.ids_mannheim.korap.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import de.ids_mannheim.korap.util.KrillDate;
import de.ids_mannheim.korap.util.QueryException;
import java.time.LocalDate;


/**
 * @author diewald
 */
public class TestKrillDate {

    @Test
    public void testByString () {
        KrillDate kd = new KrillDate("2005-06-03");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(3, kd.day);

        kd = new KrillDate("2005-06");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(0, kd.day);

        kd = new KrillDate("2005");
        assertEquals(2005, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);

        kd = new KrillDate();
        assertEquals(0, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);

        kd = new KrillDate("hui");
        assertEquals(0, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);

        kd = new KrillDate("9999-99-99");
        assertEquals(9999, kd.year);
        assertEquals(99, kd.month);
        assertEquals(99, kd.day);
    };


    @Test
    public void testWithCeil () {
        KrillDate kd = new KrillDate("2005-06-03");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(3, kd.day);
        assertEquals(20050603, kd.ceil());

        kd = new KrillDate("2005-06");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(0, kd.day);
        assertEquals(20050699, kd.ceil());

        kd = new KrillDate("2005");
        assertEquals(2005, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals(20059999, kd.ceil());

        kd = new KrillDate();
        assertEquals(0, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals(99999999, kd.ceil());
    };


    @Test
    public void testWithFloor () {
        KrillDate kd = new KrillDate("2005-06-03");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(3, kd.day);
        assertEquals(20050603, kd.floor());

        kd = new KrillDate("2005-06");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(0, kd.day);
        assertEquals(20050600, kd.floor());

        kd = new KrillDate("2005");
        assertEquals(2005, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals(20050000, kd.floor());

        kd = new KrillDate();
        assertEquals(0, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals(0, kd.floor());
    };


    @Test
    public void testToString () {
        KrillDate kd = new KrillDate("2005-06-03");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(3, kd.day);
        assertEquals("20050603", kd.toString());

        kd = new KrillDate("2005-06");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(0, kd.day);
        assertEquals("20050600", kd.toString());

        kd = new KrillDate("2005");
        assertEquals(2005, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals("20050000", kd.toString());

        kd = new KrillDate();
        assertEquals(0, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals(null, kd.toString());
    };


    @Test
    public void testToCeilString () {
        KrillDate kd = new KrillDate("2005-06-03");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(3, kd.day);
        assertEquals("20050603", kd.toCeilString());

        kd = new KrillDate("2005-06");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(0, kd.day);
        assertEquals("20050699", kd.toCeilString());

        kd = new KrillDate("2005");
        assertEquals(2005, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals("20059999", kd.toCeilString());

        kd = new KrillDate();
        assertEquals(0, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals("99999999", kd.toCeilString());
    };


    @Test
    public void testToFloorString () {
        KrillDate kd = new KrillDate("2005-06-03");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(3, kd.day);
        assertEquals("20050603", kd.toFloorString());

        kd = new KrillDate("2005-06");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(0, kd.day);
        assertEquals("20050600", kd.toFloorString());

        kd = new KrillDate("2005");
        assertEquals(2005, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals("20050000", kd.toFloorString());

        kd = new KrillDate();
        assertEquals(0, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals("0", kd.toFloorString());
    };


    @Test
    public void testDisplay () {
        KrillDate kd = new KrillDate("2005-06-03");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(3, kd.day);
        assertEquals("2005-06-03", kd.toDisplay());

        kd = new KrillDate("2005-06");
        assertEquals(2005, kd.year);
        assertEquals(6, kd.month);
        assertEquals(0, kd.day);
        assertEquals("2005-06", kd.toDisplay());

        kd = new KrillDate("2005");
        assertEquals(2005, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals("2005", kd.toDisplay());

        kd = new KrillDate();
        assertEquals(0, kd.year);
        assertEquals(0, kd.month);
        assertEquals(0, kd.day);
        assertEquals("", kd.toDisplay());
    };

    @Test
    public void testWithLocalDate () {
        KrillDate kd =
            new KrillDate(
                LocalDate.of(2012, 12, 12)
                );
        assertEquals("2012-12-12", kd.toDisplay());

        kd = new KrillDate(
            LocalDate.of(2014, 1, 2)
            );
        assertEquals("2014-01-02", kd.toDisplay());
    };
};
