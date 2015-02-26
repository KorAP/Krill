package de.ids_mannheim.korap.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import static de.ids_mannheim.korap.util.KrillByte.*;
import de.ids_mannheim.korap.util.QueryException;
import java.nio.ByteBuffer;

/**
 * @author diewald
 */
public class TestKrillByte {

    @Test
    public void testConversion() {
        assertEquals(4, byte2int(int2byte(4)));
        assertEquals(
            byte2int(ByteBuffer.allocate(4).putInt(4).array()),
            byte2int(int2byte(4))
        );

        assertEquals(
            byte2int(ByteBuffer.allocate(4).putInt(99999).array()),
            byte2int(int2byte(99999))
        );

        assertEquals(128, byte2int(int2byte(128)));
        assertEquals(1024, byte2int(int2byte(1024)));
        assertEquals(66_666, byte2int(int2byte(66_666)));
        assertEquals(66_666, byte2int(int2byte(66_666)), 0);

        byte[] bb = ByteBuffer.allocate(12).putInt(99999).putInt(666).putInt(1234).array();

        assertEquals(99999, byte2int(bb,0));
        assertEquals(666,   byte2int(bb,4));
        assertEquals(1234,  byte2int(bb,8));
    };
};
