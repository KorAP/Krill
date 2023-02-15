package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.ids_mannheim.korap.TestSimple;

@RunWith(JUnit4.class)
public class TestFuzzyHelper {

    @Test
    public void annotatedFuzzyFieldDocTest () {

        List<String> chars = Arrays.asList("a", "b", "c");

        FieldDocument f;

        for (int i = 0; i < 10; i++) {
            f = TestSimple.annotatedFuzzyFieldDoc(chars, 2, 8);
            assertTrue(Pattern.matches("^[abc]:[abc]{2,8}$", f.getFieldValue("test")));
            assertTrue(Pattern.matches("^[abc]:.*<>:base\\/s:t\\$.*[as]:[abc]\\|_[1-7]\\$<i>[1-7]<i>[1-8]\\]$", f.getFieldValue("plain")));
        };
    };

    @Test
    public void annotatedFuzzyWithSentencesFieldDocTest () {

        List<String> chars = Arrays.asList("a", "b", "c");

        FieldDocument f;

        for (int i = 0; i < 10; i++) {
            f = TestSimple.annotatedFuzzyWithSentencesFieldDoc(chars, 2, 8);
            assertTrue(Pattern.matches("^[abc]:~([abc]+\\~)*[abc]+$", f.getFieldValue("test")));
        };
    };

    @Test
    public void regexInTeststringTest () {

        String x = "a:aba~abbc~acbbb";
        assertEquals(TestSimple.countAllMatches(x, "a", "a[^~]*b"), 6);
        // 1 . a:[ab]a~abbc~acbbb
        // 2 . a:aba~[abb]c~acbbb
        // 3 . a:aba~[ab]bc~acbbb
        // 4 . a:aba~abbc~[acbbb]
        // 5 . a:aba~abbc~[acbb]b
        // 6 . a:aba~abbc~[acb]bb

        x = "a:abbaba~ab";
        assertEquals(TestSimple.countAllMatches(x, "a", "a[^~]*b"), 5);
        // 1 . a:[abbab]a~ab
        // 2 . a:[abb]aba~ab
        // 3 . a:[ab]baba~ab
        // 4 . a:abb[ab]a~ab
        // 5 . a:abbaba~[ab]

        x = "abbaba~ab";
        assertEquals(TestSimple.countAllMatches(x, "", "a[^~]*b"), 5);
        // 1 . [abbab]a~ab
        // 2 . [abb]aba~ab
        // 3 . [ab]baba~ab
        // 4 . abb[ab]a~ab
        // 5 . abbaba~[ab]
    };
};
