package de.ids_mannheim.korap.response;

import de.ids_mannheim.korap.response.Match;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestMatch {

    @Test
    public void testNoMatch () {
        Match m = new Match("aaa", false);
        assertEquals(null, m.getID());
    };


    @Test
    public void testMatchBug () {
        Match m = new Match("match-PRO-DUD!PRO-DUD_KSTA-2013-01.7483-2013-01",
                false);
        assertEquals(null, m.getID());
    };


    @Test
    public void testMatchTextSigle1 () {
        Match m = new Match("match-GOE!GOE_AGK.00000-p60348-60349", false);
        assertEquals("GOE_AGK.00000", m.getTextSigle());
    };


    @Test
    public void testMatchTextSigle2 () {
        Match m = new Match("match-PRO-DUD!PRO-DUD_KSTA-2013-01.3651-p326-327",
                false);
        assertEquals("PRO-DUD_KSTA-2013-01.3651", m.getTextSigle());
    };
};
