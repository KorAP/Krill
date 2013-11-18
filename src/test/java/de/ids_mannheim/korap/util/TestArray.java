import java.util.*;
import static de.ids_mannheim.korap.util.KorapArray.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class TestArray {

    @Test
    public void StringJoin1 () {
	String[] test = new String[]{"a", "bc", "def"};
	assertEquals(join(",", test), "a,bc,def");
    };

    @Test
    public void StringJoin2 () {
	assertEquals(join(",", "a", "bc", "def"), "a,bc,def");
    };

    @Test
    public void StringJoin3 () {
	assertEquals(join(',', "a", "bc", "def"), "a,bc,def");
    };

    @Test
    public void StringJoin4 () {
	assertEquals(join("--", "a", "bc", "def"), "a--bc--def");
    };


};
