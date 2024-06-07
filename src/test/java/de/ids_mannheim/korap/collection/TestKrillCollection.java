package de.ids_mannheim.korap.collection;

import java.io.IOException;

import org.junit.Test;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.util.KrillProperties;
import de.ids_mannheim.korap.util.QueryException;

public class TestKrillCollection {

    @Test
    public void testLoadVCFromStore () throws QueryException, IOException {
        KrillProperties.loadProperties("krill.properties.info");
        KrillCollection kc = new KrillCollection();
        kc = kc.fromStore("unknown-vc");
        KrillProperties.loadDefaultProperties();
    }
}
