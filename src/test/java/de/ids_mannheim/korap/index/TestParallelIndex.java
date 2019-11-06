package de.ids_mannheim.korap.index;
import static org.junit.Assert.assertEquals;

import java.util.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.*;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;

import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.Match;

import org.apache.lucene.store.MMapDirectory;


public class TestParallelIndex {

    private KrillIndex sample;

    private KrillIndex getSampleIndex () throws IOException {
        return new KrillIndex(new MMapDirectory(
                Paths.get(getClass().getResource("/sample-index").getFile())));

    };

    @Test
    public void TestSampleIndexSearchParallel () throws IOException, QueryException, InterruptedException, ExecutionException {

        sample = getSampleIndex();
        // The sample index is global

        final SpanQuery sq1 = new QueryBuilder("tokens").seg("s:meine").toQuery();
        final SpanQuery sq2 = new QueryBuilder("tokens").seg("s:ihre").toQuery();
        final SpanQuery sq3 = new QueryBuilder("tokens").seg("s:unseres").toQuery();

        Callable<String> req1 = new Callable<String>(){
                @Override
                public String call() throws Exception {

                    Result kr = sample.search(sq1, (short) 10);

                    if (kr.getMatch(0).getStartPos() != 131) {
                        return "1-1StartPos=" + kr.getMatch(0).getStartPos();
                    }

                    if (kr.getMatch(0).getEndPos() != 132) {
                        return "1-1EndPos=" + kr.getMatch(0).getEndPos();
                    }

                    if (kr.getMatch(1).getStartPos() != 803) {
                        return "1-2StartPos=" + kr.getMatch(1).getStartPos();
                    }

                    if (kr.getMatch(1).getEndPos() != 804) {
                        return "1-2EndPos=" + kr.getMatch(1).getEndPos();
                    }

                    if (!kr.getMatch(1).getSnippetBrackets().equals(
                            "... der Jesuiten Tun und Wesen hält [[meine]] Betrachtungen fest. Kirchen, Türme, Gebäude haben ..."
                            )) {
                        return "1-Snippet=" + kr.getMatch(1).getSnippetBrackets();
                    }
                    
                    return "ok";
                }
            };

        Callable<String> req2 = new Callable<String>(){
                @Override
                public String call() throws Exception {
                    Result kr = sample.search(sq2, (short) 10);

                    if (kr.getMatch(0).getStartPos() != 471) {
                        return "2-1StartPos=" + kr.getMatch(0).getStartPos();
                    }

                    if (kr.getMatch(0).getEndPos() != 472) {
                        return "2-1EndPos=" + kr.getMatch(0).getEndPos();
                    }

                    if (kr.getMatch(1).getStartPos() != 715) {
                        return "2-2StartPos=" + kr.getMatch(1).getStartPos();
                    }

                    if (kr.getMatch(1).getEndPos() != 716) {
                        return "2-2EndPos=" + kr.getMatch(1).getEndPos();
                    }

                    if (!kr.getMatch(1).getSnippetBrackets().equals(
                            "... und wie durch gefälligen Prunk sich [[ihre]] Kirchen auszeichnen, so bemächtigen sich die ..."
                            )) {
                        return "2-Snippet=" + kr.getMatch(1).getSnippetBrackets();
                    }
                    
                    return "ok";
                }
            };

        Callable<String> req3 = new Callable<String>(){
                @Override
                public String call() throws Exception {
                    Result kr = sample.search(sq3, (short) 10);

                    if (kr.getMatch(0).getStartPos() != 69582) {
                        return "3-1StartPos=" + kr.getMatch(0).getStartPos();
                    }

                    if (kr.getMatch(0).getEndPos() != 69583) {
                        return "3-1EndPos=" + kr.getMatch(0).getEndPos();
                    }

                    if (kr.getMatch(1).getStartPos() != 70671) {
                        return "3-2StartPos=" + kr.getMatch(1).getStartPos();
                    }

                    if (kr.getMatch(1).getEndPos() != 70672) {
                        return "3-2EndPos=" + kr.getMatch(1).getEndPos();
                    }

                    if (!kr.getMatch(1).getSnippetBrackets().equals(
                            "... Blatt gibt euch bloß ein Zeugnis [[unseres]] Unvermögens, diese Gegenstände genugsam zu fassen ..."
                            )) {
                        return "3-Snippet=" + kr.getMatch(1).getSnippetBrackets();
                    }
                    
                    return "ok";
                }
            };
        

        // Create a pool with n threads
        ExecutorService executor = Executors.newFixedThreadPool(16);

        for (int i = 0; i < 2000; i++) {
            Future<String> res3 = executor.submit(req3);
            Future<String> res1 = executor.submit(req1);
            Future<String> res2 = executor.submit(req2);

            String value1 = res1.get();
            String value2 = res2.get();
            String value3 = res3.get();

            if (!value1.equals("ok")) {
                System.err.println("at "+ i);
                assertEquals("ok", value1);
                break;
            }
            if (!value2.equals("ok")) {
                System.err.println("at "+ i);
                assertEquals("ok", value2);
                break;
            }
            if (!value3.equals("ok")) {
                System.err.println("at "+ i);
                assertEquals("ok", value3);
                break;
            }
            System.err.println("Run "+i);
        };
        
        executor.shutdown();
    };

    @Test
    public void TestSampleIndexMatchinfoParallel () throws IOException, QueryException, InterruptedException, ExecutionException {

        sample = getSampleIndex();
        // The sample index is global

        Callable<String> req1 = new Callable<String>(){
                @Override
                public String call() throws Exception {

                    Match km = sample.getMatchInfo(
                        "match-GOE/AGI/00000-p1075-1076",
                        "tokens",
                        true,
                        (ArrayList) null,
                        (ArrayList) null,
                        false,
                        true,
                        false
                        );

                    if (km.getStartPos() != 1075) {
                        return "1-StartPos=" + km.getStartPos();
                    }
                    if (km.getEndPos() != 1076) {
                        return "1-EndPos=" + km.getEndPos();
                    }

                    if (!km.getSnippetBrackets().equals(
                            "... [[{corenlp/p:PPOSAT:{marmot/m:case:acc:{marmot/m:gender:neut:{marmot/m:number:pl:{marmot/p:PPOSAT:{opennlp/p:PPOSAT:{tt/l:mein:{tt/p:PPOSAT:meine}}}}}}}}]] ..."
                            )) {
                        return "1-Snippet=" + km.getSnippetBrackets();
                    }
                    return "ok";
                }
            };

        Callable<String> req2 = new Callable<String>(){
                @Override
                public String call() throws Exception {

                    Match km = sample.getMatchInfo(
                        "match-GOE/AGD/00000-p142169-142170",
                        "tokens",
                        true,
                        (ArrayList) null,
                        (ArrayList) null,
                        false,
                        true,
                        false
                        );

                    if (km.getStartPos() != 142169) {
                        return "2-StartPos=" + km.getStartPos();
                    }
                    if (km.getEndPos() != 142170) {
                        return "2-EndPos=" + km.getEndPos();
                    }

                    if (!km.getSnippetBrackets().equals(
                            "... [[{corenlp/p:NN:{marmot/m:case:acc:{marmot/m:gender:masc:{marmot/m:number:sg:{marmot/p:NN:{opennlp/p:NN:{tt/l:Baum:{tt/p:NN:Baum}}}}}}}}]] ..."
                            )) {
                        return "2-Snippet=" + km.getSnippetBrackets();
                    }
                    return "ok";
                }
            };

        Callable<String> req3 = new Callable<String>(){
                @Override
                public String call() throws Exception {

                    Match km = sample.getMatchInfo(
                        "match-GOE/AGI/04846-p42348-42349",
                        "tokens",
                        true,
                        (ArrayList) null,
                        (ArrayList) null,
                        false,
                        true,
                        false
                        );

                    if (km.getStartPos() != 42348) {
                        return "3-StartPos=" + km.getStartPos();
                    }
                    if (km.getEndPos() != 42349) {
                        return "3-EndPos=" + km.getEndPos();
                    }

                    if (!km.getSnippetBrackets().equals(
                            "... [[{corenlp/p:NN:{marmot/m:case:nom:{marmot/m:gender:fem:{marmot/m:number:sg:{marmot/p:NN:{opennlp/p:NN:{tt/l:Straße:{tt/p:NN:Straße}}}}}}}}]] ..."
                            )) {
                        return "3-Snippet=" + km.getSnippetBrackets();
                    }
                    return "ok";
                }
            };        
        

        // Create a pool with n threads
        ExecutorService executor = Executors.newFixedThreadPool(16);

        for (int i = 0; i < 200; i++) {
            Future<String> res1 = executor.submit(req1);
            Future<String> res2 = executor.submit(req2);
            Future<String> res3 = executor.submit(req3);
            
            String value1 = res1.get();
            String value2 = res2.get();
            String value3 = res3.get();

            if (!value1.equals("ok")) {
                System.err.println("at "+ i);
                assertEquals("ok", value1);
                break;
            }
            if (!value2.equals("ok")) {
                System.err.println("at "+ i);
                assertEquals("ok", value2);
                break;
            }
            if (!value3.equals("ok")) {
                System.err.println("at "+ i);
                assertEquals("ok", value3);
                break;
            }
            System.err.println("Run "+i);
        };
        
        executor.shutdown();
    };
};
