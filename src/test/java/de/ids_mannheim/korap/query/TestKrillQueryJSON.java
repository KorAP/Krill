package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.getJsonQuery;
import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

@RunWith(JUnit4.class)
public class TestKrillQueryJSON {

    @Test
    public void queryJSONBsp1 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp1.jsonld").getFile());

        // There is a repetition in here
        // ([base=foo]|[base=bar])[base=foobar]
        assertEquals(sqwi.toQuery().toString(),
                "spanOr([tokens:base:foo, spanRepetition(spanNext(tokens:base:foo, tokens:base:bar){1,100})])");
        assertTrue(sqwi.isOptional());
    };


    @Test
    public void queryJSONBsp1Disjunction () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp1c.jsonld").getFile());

        // There is a repetition in here
        // ([base=foo]|[base=bar])[base=foobar]
        assertEquals(sqwi.toQuery().toString(),
                "spanOr([tokens:base:foo, spanRepetition(spanNext(tokens:base:foo, tokens:base:bar){1,100})])");
        assertTrue(sqwi.isOptional());
    };


    @Test
    public void queryJSONBsp1b () throws QueryException {

        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp1b.jsonld").getFile());

        // [base=foo]|([base=foo][base=bar]) meta author=Goethe&year=1815
        assertEquals(sqwi.toQuery().toString(),
                "spanOr([tokens:mate/l:foo, spanNext(tokens:mate/l:foo, tokens:mate/l:bar)])");
    };


    @Test
    public void queryJSONBsp2 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp2.jsonld").getFile());

        // ([base=foo]|[base=bar])[base=foobar]
        assertEquals(sqwi.toQuery().toString(),
                "spanNext(spanOr([tokens:mate/l:foo, tokens:mate/l:bar]), tokens:mate/l:foobar)");
    };


    @Test
    public void queryJSONBsp3 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp3.jsonld").getFile());

        // focus({[base=Mann]})
        assertEquals(sqwi.toQuery().toString(),
                "focus(1: {1: tokens:mate/l:Mann})");
    };


    @Test
    public void queryJSONBsp4 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp4.jsonld").getFile());

        // focus({[base=foo]}[orth=bar])
        assertEquals(sqwi.toQuery().toString(),
                "focus(1: spanNext({1: tokens:mate/l:foo}, tokens:s:bar))");
    };


    @Test
    public void queryJSONBsp5 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp5.jsonld").getFile());

        // focus(1:[base=Der]{1:[base=Mann]}) 
        assertEquals(sqwi.toQuery().toString(),
                "focus(1: spanNext(tokens:mate/l:Der, {1: tokens:mate/l:Mann}))");
    };


    @Test
    public void queryJSONBsp6 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp6.jsonld").getFile());

        // [base=katze]
        assertEquals(sqwi.toQuery().toString(), "tokens:mate/l:Katze");
    };


    @Test
    public void queryJSONBsp7 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp7.jsonld").getFile());

        // [!base=Katze]
        assertEquals("tokens:mate/l:Katze", sqwi.toQuery().toString());
        assertTrue(sqwi.isNegative());
    };


    @Test
    public void queryJSONBsp9 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp9.jsonld").getFile());

        // [base=Katze&orth=Katzen]
        assertEquals(sqwi.toQuery().toString(),
                "spanSegment(tokens:mate/l:Katze, tokens:s:Katzen)");
    };


    @Test
    public void queryJSONBsp9b () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp9b.jsonld").getFile());

        // [base=Katze&orth=Katzen]
        assertEquals(sqwi.toQuery().toString(),
                "spanSegment(tokens:mate/m:number:pl, tokens:tt/p:NN)");
    };


    @Test
    public void queryJSONBsp10 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp10.jsonld").getFile());

        // [base=Katze][orth=und][orth=Hunde]
        assertEquals(sqwi.toQuery().toString(),
                "spanNext(spanNext(tokens:mate/l:Katze, tokens:s:und), tokens:s:Hunde)");
    };


    @Test
    public void queryJSONBsp11 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp11.jsonld").getFile());

        // [base!=Katze | orth!=Katzen]
        /*
          Imagine a([^b]|[^c])d
          Matches abd and acd
          Interpretation would be not(spanAnd(...))
        */
        assertEquals(sqwi.toQuery().toString(),
                "spanOr([tokens:mate/l:Katze, tokens:s:Katzen])");
        assertTrue(sqwi.isNegative());
    };


    @Test
    public void queryJSONBsp12 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp12.jsonld").getFile());

        // contains(<np>,[base=Mann])
        assertEquals(sqwi.toQuery().toString(),
                "spanContain(<tokens:np />, tokens:mate/l:Mann)");
    };


    @Test
    public void queryJSONBsp13 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp13.jsonld").getFile());

        assertEquals(sqwi.toQuery().toString(),
                "spanStartsWith(<tokens:np />, tokens:p:Det)");
    };


    @Test
    public void queryJSONBsp13b () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp13b.jsonld").getFile());

        // startswith(<np>,[pos=Det])
        assertEquals(sqwi.toQuery().toString(),
                "spanStartsWith(<tokens:np />, tokens:mate/p:Det)");
    };


    @Test
    public void queryJSONBsp14 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp14.jsonld").getFile());

        // 'vers{2,3}uch'
        assertEquals(sqwi.toQuery().toString(),
                "SpanMultiTermQueryWrapper(tokens:/s:vers{2,3}uch/)");
    };


    @Test
    public void queryJSONBsp15 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp15.jsonld").getFile());

        // [orth='vers.*ch']
        assertEquals(sqwi.toQuery().toString(),
                "SpanMultiTermQueryWrapper(tokens:/s:vers.*ch/)");
    };


    @Test
    public void queryJSONBsp16 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp16.jsonld").getFile());

        // [(base=bar|base=foo)&orth=foobar]
        assertEquals(sqwi.toQuery().toString(),
                "spanSegment(spanOr([tokens:mate/l:bar, tokens:mate/l:foo]), tokens:s:foobar)");
    };


    @Test
    public void queryJSONBsp17 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp17.jsonld").getFile());

        // within(<np>,[base=Mann])
        assertEquals(sqwi.toQuery().toString(),
                "spanContain(<tokens:np />, tokens:mate/l:Mann)");
    };


    @Test
    public void queryJSONDemo () throws QueryException {
        SpanQueryWrapper sqwi = new KrillQuery("tokens").fromKoral(
                "{ \"query\" : { \"@type\" : \"koral:token\", \"wrap\" : { \"@type\" : \"koral:term\", \"foundry\" : \"base\", \"layer\" : \"p\", \"key\" : \"foo\", \"match\" : \"match:eq\" }}}");

        assertEquals(sqwi.toQuery().toString(), "tokens:base/p:foo");
    };


    @Test
    public void queryJSONBspClass () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bsp-class.jsonld").getFile());

        // within(<np>,[base=Mann])
        assertEquals(sqwi.toQuery().toString(),
                "{1: spanNext(tokens:tt/p:ADJA, tokens:mate/p:NN)}");
    };


    @Test
    public void queryJSONcosmas3 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/cosmas3.json").getFile());

        // "das /+w1:3 Buch"
        assertEquals(sqwi.toQuery().toString(),
                "spanDistance(tokens:s:das, tokens:s:Buch, [(w[1:3], ordered, notExcluded)])");
    };


    @Test
    public void queryJSONcosmas4 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/cosmas4.json").getFile());

        // "das /+w1:3,s1:1 Buch"
        assertEquals(sqwi.toQuery().toString(),
                "spanMultipleDistance(tokens:s:das, tokens:s:Buch, [(w[1:3], ordered, notExcluded), (base/s:s[1:1], ordered, notExcluded)])");
    };


    @Test
    public void queryJSONcosmas4b () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/cosmas4b.json").getFile());

        // "das /+w1:3,s1 Buch"
        assertEquals(sqwi.toQuery().toString(),
                "spanMultipleDistance(tokens:s:das, tokens:s:Buch, [(w[1:3], ordered, notExcluded), (base/s:s[0:1], ordered, notExcluded)])");
    };


    @Test
    public void queryJSONcosmas10 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/cosmas10.json").getFile());

        // "Institut für $deutsche Sprache"
        assertEquals(sqwi.toQuery().toString(),
                "spanNext(spanNext(spanNext(tokens:s:Institut, tokens:s:für), tokens:i:deutsche), tokens:s:Sprache)");
    };


    @Test
    public void queryJSONcosmas10b () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/cosmas10b.json").getFile());

        // "Institut $FÜR $deutsche Sprache"
        assertEquals(sqwi.toQuery().toString(),
                "spanNext(spanNext(spanNext(tokens:s:Institut, tokens:i:für), tokens:i:deutsche), tokens:s:Sprache)");
    };


    @Test
    public void queryJSONcosmas16 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/cosmas16.json").getFile());

        // "$wegen #IN(L) <s>"
        assertEquals(sqwi.toQuery().toString(),
                "focus(1: spanStartsWith(<tokens:s />, {1: tokens:i:wegen}))");
    };


    @Test
    public void queryJSONcosmas17 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/cosmas17.json").getFile());

        // "#BED($wegen , +sa)"
        assertEquals(sqwi.toQuery().toString(),
                "spanStartsWith(<tokens:s />, tokens:i:wegen)");
    };


    @Test
    public void queryJSONcosmas20 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/cosmas20.json").getFile());

        //     "MORPH(V) #IN(R) #ELEM(S)"
        assertEquals(sqwi.toQuery().toString(),
                "focus(1: spanEndsWith(<tokens:s />, {1: tokens:p:V}),sorting)");
    };

    @Test
    public void queryJSONcosmas21 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/cosmas21.json").getFile());

        // die %+w1:1 Gegenwart
        assertEquals(sqwi.toQuery().toString(),
                "spanDistance({129: tokens:s:die}, {129: tokens:s:Gegenwart}, [(w[1:1], ordered, excluded)])");
    };

    

    @Test
    public void queryJSONrepetition () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(getClass()
                .getResource("/queries/bsp-repetition.jsonld").getFile());

        // der[cnx/p=A]{0,2}[tt/p=NN]
        assertEquals(sqwi.toQuery().toString(),
                "spanNext(tokens:s:der, spanOr([tokens:tt/p:NN, spanNext(spanRepetition(tokens:cnx/p:A{1,2}), tokens:tt/p:NN)]))");
    };


    @Test
    public void queryJSONboundaryBug () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(getClass()
                .getResource("/queries/bsp-boundary.jsonld").getFile());

        // Tal []{1,} Wald
        assertEquals(sqwi.toQuery().toString(),
                "spanDistance(tokens:s:Tal, tokens:s:Wald, [(w[2:101], ordered, notExcluded)])");
    };


    @Test
    public void queryJSONcosmasBoundaryBug () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(getClass()
                .getResource("/queries/bugs/cosmas_boundary.jsonld").getFile());

        // Namen /s1 Leben
        assertEquals(sqwi.toQuery().toString(),
                "focus(129: spanElementDistance({129: tokens:s:Namen}, {129: tokens:s:Leben}, [(base/s:s[0:1], notOrdered, notExcluded)]),sorting)");
    };


    @Test
    public void queryJSONfoundryForOrthBug () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bugs/foundry_for_orth.jsonld")
                        .getFile());

        // opennlp/orth:Baum
        assertEquals(sqwi.toQuery().toString(), "tokens:s:Baum");
    };


    @Test
    public void queryJSONfoundryForOrthBug2 () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(getClass()
                .getResource("/queries/bugs/foundry_for_orth_2.jsonld")
                .getFile());

        // baum/i
        assertEquals(sqwi.toQuery().toString(), "tokens:i:baum");
    };


    @Test
    public void queryJSONunderspecifiedTokenBug () {
        // ((MORPH(APPR) ODER MORPH(APPRART)) /+w1 Urlaub
        try {
            String json = getJsonString(getClass()
                    .getResource("/queries/bugs/underspecified_token.jsonld")
                    .getFile());
            new KrillQuery("tokens").fromKoral(json);
        }
        catch (QueryException e) {
            assertEquals(701, e.getErrorCode());
        };
    };


    @Test
    public void queryJSONspecialLayerBug () throws QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(getClass()
                .getResource("/queries/bugs/special_layer.jsonld").getFile());
        assertEquals(sqwi.toQuery().toString(),
                "spanNext(spanNext(spanNext(tokens:s:Baum, tokens:cnx/p:CC), tokens:tt/l:Baum), <tokens:xip/c:MC />)");
    };


    @Test
    public void queryJSONrepetitionGroupRewriteBug () throws QueryException {
        // ([cnx/p="A"][]){2}
        SpanQueryWrapper sqwi = getJsonQuery(getClass()
                .getResource("/queries/bugs/repetition_group_rewrite.jsonld")
                .getFile());

        assertEquals(sqwi.toQuery().toString(),
                "spanRepetition(spanExpansion(SpanMultiTermQueryWrapper(tokens:/cnx/p:A/), []{1, 1}, right){2,2})");
    };


    @Test
    public void queryJSONoverlapsFrameWorkaround () throws QueryException {
        // overlaps(<s>,[tt/p=CARD][tt/p="N.*"])
        SpanQueryWrapper sqwi = getJsonQuery(getClass()
                .getResource("/queries/bugs/overlaps_frame_workaround.jsonld")
                .getFile());

        assertEquals(sqwi.toQuery().toString(),
                "spanOverlap(<tokens:s />, spanNext(tokens:tt/p:CARD, SpanMultiTermQueryWrapper(tokens:/tt/p:N.*/)))");
    };


    @Test
    public void queryJSONflags1 () throws QueryException {
        // buchstabe/i
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/flags/caseInsensitive.jsonld")
                        .getFile());

        assertEquals(sqwi.toQuery().toString(), "tokens:i:buchstabe");
    };


    @Test
    public void queryJSONspanWrapDeserializationBug () throws QueryException {
        // contains(<s>, Erde  []* Sonne)
        SpanQueryWrapper sqwi = getJsonQuery(getClass()
                .getResource("/queries/bugs/unspecified_key_bug.jsonld")
                .getFile());

        assertEquals(sqwi.toQuery().toString(),
                "spanContain(<tokens:s />, spanDistance(tokens:s:Erde, tokens:s:Sonne, [(w[1:101], ordered, notExcluded)]))");
    };


    @Test
    public void queryJSONflags2 () throws QueryException {
        // buchstabe/i
        try {
            String json = getJsonString(getClass()
                    .getResource("/queries/flags/unknown1.jsonld").getFile());
            KrillQuery kq = new KrillQuery("tokens");
            assertEquals(kq.fromKoral(json).toQuery().toString(),
                    "tokens:s:buchstabe");
            assertEquals(kq.getWarning(0).getCode(), 748);

            json = getJsonString(getClass()
                    .getResource("/queries/flags/unknown2.jsonld").getFile());
            kq = new KrillQuery("tokens");
            assertEquals(kq.fromKoral(json).toQuery().toString(),
                    "tokens:i:buchstabe");
            assertEquals(kq.getWarning(0).getCode(), 748);

            json = getJsonString(getClass()
                    .getResource("/queries/flags/unknown3.jsonld").getFile());
            kq = new KrillQuery("tokens");
            assertEquals(kq.fromKoral(json).toQuery().toString(),
                    "tokens:i:buchstabe");
            assertEquals(kq.getWarning(0).getCode(), 748);

        }
        catch (QueryException e) {
            fail(e.getMessage());
        };
    };


    @Test
    public void queryJSONelement () throws QueryException {
        // <base/s=s>
        try {
            String json = getJsonString(getClass()
                    .getResource("/queries/element/simple-element.jsonld")
                    .getFile());
            KrillQuery kq = new KrillQuery("tokens");

            assertEquals(kq.fromKoral(json).toQuery().toString(),
                    "<tokens:base/s:s />");
        }
        catch (QueryException e) {
            fail(e.getMessage());
        };
    };


    @Test
    public void queryJSONinfiniteExpansion () throws QueryException {
        // der []*
        try {
            String json = getJsonString(getClass()
                    .getResource("/queries/bugs/expansion_bug_3.jsonld")
                    .getFile());
            KrillQuery kq = new KrillQuery("tokens");

            assertEquals(kq.fromKoral(json).toQuery().toString(),
                    "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:s:c, []{0, 4}, right)}))");
        }
        catch (QueryException e) {
            fail(e.getMessage());
        };
    };


    @Test
    public void queryJSONcomplexSpanOrTerm () throws QueryException {
        // startsWith(<base/s=s>, { lassen | laufen })
        try {
            String json = getJsonString(getClass()
                    .getResource("/queries/bugs/span_or_bug.jsonld").getFile());
            KrillQuery kq = new KrillQuery("tokens");

            assertEquals(kq.fromKoral(json).toQuery().toString(),
                    "spanStartsWith(<tokens:base/s:s />, spanOr([tokens:s:Er, tokens:s:Sie]))");
        }
        catch (QueryException e) {
            fail(e.getMessage());
        };
    };


    @Test
    public void queryJSONdistancesWithRegexes () throws QueryException {
        // "der" []{2,3} [opennlp/p="NN"]
        try {
            String json = getJsonString(getClass()
                    .getResource(
                            "/queries/bugs/distances_with_regex_bug.jsonld")
                    .getFile());
            KrillQuery kq = new KrillQuery("tokens");

            assertEquals(kq.fromKoral(json).toQuery().toString(),
                    "spanDistance(SpanMultiTermQueryWrapper(tokens:/s:der/), SpanMultiTermQueryWrapper(tokens:/opennlp/p:NN/), [(w[3:4], ordered, notExcluded)])");
        }
        catch (QueryException e) {
            fail(e.getMessage());
        };
    };

    @Test
    public void queryJSONtermVector () throws QueryException {
        // base=foo|base=bar|base=xyz|base=abc
        try {
            String json = getJsonString(getClass()
                    .getResource(
                            "/queries/segment/vector.jsonld")
                    .getFile());
            KrillQuery kq = new KrillQuery("tokens");

            assertEquals("spanOr([tokens:s:foo, tokens:s:bar, tokens:s:xyz, tokens:s:abc])",
                         kq.fromKoral(json).toQuery().toString());
        }
        catch (QueryException e) {
            fail(e.getMessage());
        };
    };

    @Test
    public void queryJSONtermVectorCaseInsensitive () throws QueryException {
        // base=fOo|base=bAr|base=xYz|base=aBc
        try {
            String json = getJsonString(getClass()
                    .getResource(
                            "/queries/segment/vector-caseinsensitive.jsonld")
                    .getFile());
            KrillQuery kq = new KrillQuery("tokens");

            assertEquals("spanOr([tokens:i:foo, tokens:i:bar, tokens:i:xyz, tokens:i:abc])",
                         kq.fromKoral(json).toQuery().toString());
        }
        catch (QueryException e) {
            fail(e.getMessage());
        };
    };

    
    @Test
    public void queryJSONwildcardVector () throws QueryException {
        // base=f?o|base=bar|base=x*z|base=abc
        try {
            String json = getJsonString(getClass()
                    .getResource(
                            "/queries/segment/vector-wildcards.jsonld")
                    .getFile());
            KrillQuery kq = new KrillQuery("tokens");

            assertEquals("spanOr([" +
                         "SpanMultiTermQueryWrapper(tokens:s:f?o), " +
                         "SpanMultiTermQueryWrapper(tokens:s:bar), " +
                         "SpanMultiTermQueryWrapper(tokens:s:x*z), " +
                         "SpanMultiTermQueryWrapper(tokens:s:abc)" +
                         "])",
                         kq.fromKoral(json).toQuery().toString());
        }
        catch (QueryException e) {
            fail(e.getMessage());
        };
    };    


    @Test
    public void queryJSONregexVector () throws QueryException {
        // base=f.?o|base=b[au]r|base=x(yz)*|base=ab+c
        try {
            String json = getJsonString(getClass()
                    .getResource(
                            "/queries/segment/vector-regex.jsonld")
                    .getFile());
            KrillQuery kq = new KrillQuery("tokens");

            assertEquals("spanOr([" +
                         "SpanMultiTermQueryWrapper(tokens:/s:f.?o/), " +
                         "SpanMultiTermQueryWrapper(tokens:/s:b[au]r/), " +
                         "SpanMultiTermQueryWrapper(tokens:/s:x(yz)*/), " +
                         "SpanMultiTermQueryWrapper(tokens:/s:ab+c/)" +
                         "])",
                         kq.fromKoral(json).toQuery().toString());
        }
        catch (QueryException e) {
            fail(e.getMessage());
        };
    };        
    
    
    @Test
    public void queryJSONregexRewrite1 () throws QueryException {
        // "der" [.+?]
        String json = getJsonString(getClass()
                .getResource("/queries/sequence/regex-rewrite-1.jsonld")
                .getFile());
        KrillQuery kq = new KrillQuery("tokens");

        assertEquals(kq.fromKoral(json).toQuery().toString(),
                "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:s:der, []{1, 1}, right)}))");
    };

    @Test
    public void queryJSONregexFail () {
        // "Leserin.{,3}"
        String json = getJsonString(getClass()
                .getResource("/queries/segment/regex-simple.jsonld")
                .getFile());
        KrillQuery kq = new KrillQuery("tokens");

        try {
            String res = kq.fromKoral(json).toQuery().toString();
            fail("Regex not expected to work");
        }
        catch (QueryException e) {
        };
    };

    @Test
    public void queryJSONnegationInGroup () throws QueryException {
        // [orth=laufe/i & base!=Lauf]
        String json = getJsonString(getClass()
                                    .getResource("/queries/segment/negation-in-group.jsonld")
                                    .getFile());

        KrillQuery kq = new KrillQuery("tokens");
        assertEquals("spanNot(tokens:i:laufe, tokens:tt/l:Lauf, 0, 0)",
                     kq.fromKoral(json).toQuery().toString());
    };   

    @Test
    public void queryJSONnegationInGroupAlt () throws QueryException {
        // [orth=laufe/i & base!=Lauf & opennlp/l!=Lauf]
        String json = getJsonString(getClass()
                                    .getResource("/queries/segment/negation-in-group-alt.jsonld")
                                    .getFile());

        KrillQuery kq = new KrillQuery("tokens");
        assertEquals("spanNot(tokens:i:laufe, spanOr([tokens:tt/l:Lauf, tokens:opennlp/l:Lauf]), 0, 0)",
                     kq.fromKoral(json).toQuery().toString());
    };   

    @Test
    public void queryJSONnegationInGroupAlt2 () throws QueryException {
        // [orth=laufe/i & base!=Lauf & opennlp/l=Lauf]
        String json = getJsonString(getClass()
                                    .getResource("/queries/segment/negation-in-group-alt-2.jsonld")
                                    .getFile());

        KrillQuery kq = new KrillQuery("tokens");
        assertEquals("spanNot(spanSegment(tokens:i:laufe, tokens:opennlp/l:Lauf), tokens:tt/l:Lauf, 0, 0)",
                     kq.fromKoral(json).toQuery().toString());
    };   
    
    @Test
    public void queryJSONnegationInGroupRegex () throws QueryException {
        // [orth=laufe/i & base!=/Lauf/]
        String json = getJsonString(getClass()
                                    .getResource("/queries/segment/negation-in-group-regex.jsonld")
                                    .getFile());

        KrillQuery kq = new KrillQuery("tokens");
        assertEquals("spanNot(tokens:i:laufe, SpanMultiTermQueryWrapper(tokens:/tt/l:Lauf/), 0, 0)",
                     kq.fromKoral(json).toQuery().toString());
    };   
    
    @Test
    public void queryJSONregexVectorRewrite () throws QueryException {
        // der [base=f.?o|base=b[au]r|base=.*|base=ab+c]
        try {
            String json = getJsonString(
                getClass()
                .getResource("/queries/sequence/regex-rewrite-vector.jsonld")
                .getFile());
            KrillQuery kq = new KrillQuery("tokens");

            assertEquals("focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:s:der, []{1, 1}, right)}))",
                         kq.fromKoral(json).toQuery().toString());
        }
        catch (QueryException e) {
            fail(e.getMessage());
        };
    };    

    @Test
    public void queryJSONmerge () throws QueryException {
        // treat merging gracefully
        String json = getJsonString(getClass()
                .getResource("/queries/merge.jsonld")
                .getFile());
        KrillQuery kq = new KrillQuery("tokens");
        assertEquals(kq.fromKoral(json).toQuery().toString(),
                "spanNext(tokens:s:der, tokens:s:Baum)");
		assertEquals(kq.getWarning(0).getCode(), 774);
    };

    @Test
    public void queryJSONqueryref1 () {       
        try {
            String json = getJsonString(getClass()
                    .getResource("/queries/queryref1.jsonld")
                    .getFile());
            new KrillQuery("tokens").fromKoral(json);
        }
        catch (QueryException e) {
            assertEquals(713, e.getErrorCode());
            assertEquals("Query type is not supported", e.getMessage());
        };
    };

    @Test
    public void queryJSONqueryref2 () {       
        try {
            String json = getJsonString(getClass()
                    .getResource("/queries/queryref2.jsonld")
                    .getFile());
            new KrillQuery("tokens").fromKoral(json);
        }
        catch (QueryException e) {
            // assertEquals(713, e.getErrorCode());
            assertEquals("Query type is not supported", e.getMessage());
        };
    };
};
