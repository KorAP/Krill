package de.ids_mannheim.korap.response;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.util.KrillProperties;

@RunWith(JUnit4.class)
public class TestMatch {

    int maxMatchTokens = 50;
    
    public TestMatch () {
        KrillProperties.maxTokenMatchSize = 50;
    }
    
            
    @Test
    public void testNoMatch () {
        Match m = new Match(maxMatchTokens,"aaa", false);
        assertEquals(null, m.getID());
    };


    @Test
    public void testMatchBug () {
        Match m = new Match(maxMatchTokens,"match-PRO-DUD!PRO-DUD_KSTA-2013-01.7483-2013-01",
                false);
        assertEquals(null, m.getID());
    };


    @Test
    public void testMatchTextSigle1 () {
        Match m = new Match(maxMatchTokens,"match-GOE!GOE_AGK.00000-p60348-60349", false);
        assertEquals("GOE_AGK.00000", m.getTextSigle());
    };


    @Test
    public void testMatchTextSigle2 () {
        Match m = new Match(maxMatchTokens,"match-PRO-DUD!PRO-DUD_KSTA-2013-01.3651-p326-327",
                false);
        assertEquals("PRO-DUD_KSTA-2013-01.3651", m.getTextSigle());
    };

    @Test
    public void testMatchLong () {
        Match m = new Match(maxMatchTokens,"match-PRO-DUD!PRO-DUD_KSTA-2013-01.3651-p326-480",
                false);
        assertEquals(326, m.getStartPos());
        assertEquals(376, m.getEndPos());
    };   
};
