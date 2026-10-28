package de.ids_mannheim.korap.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestKrillProperties {

    @Before
    public void setUp () {
        KrillProperties.setProp(null);
        KrillProperties.loadDefaultProperties();
    }


    @After
    public void tearDown () {
        KrillProperties.setProp(null);
        KrillProperties.loadDefaultProperties();
    }


    @Test
    public void testLoadDefaultPropertiesAfterCustomProperties ()
            throws IOException {
        File tempPropertiesFile = File.createTempFile("krill", ".properties");
        try (FileWriter writer = new FileWriter(tempPropertiesFile)) {
            writer.write("krill.index.textSize.max = 25000000\n");
        }

        try {
            KrillProperties.loadProperties(tempPropertiesFile.getAbsolutePath());

            assertEquals("", KrillProperties.namedVCPath);
            assertTrue(!KrillProperties.isTest);

            assertNotNull(KrillProperties.loadDefaultProperties());
            assertEquals("queries/collections/named-vcs/",
                    KrillProperties.namedVCPath);
            assertTrue(KrillProperties.isTest);
        }
        finally {
            tempPropertiesFile.delete();
        }
    }
}
