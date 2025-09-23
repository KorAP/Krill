package de.ids_mannheim.korap.response;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillMeta;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.util.KrillProperties;

@RunWith(JUnit4.class)
public class TestKwicCap {

  private int oldKwic;
  private int oldMatch;
  private int oldContext;

  @Before
  public void saveOldProps() {
    oldKwic = KrillProperties.maxTokenKwicSize;
    oldMatch = KrillProperties.maxTokenMatchSize;
    oldContext = KrillProperties.maxTokenContextSize;
  }

  @After
  public void restoreOldProps() {
    KrillProperties.maxTokenKwicSize = oldKwic;
    KrillProperties.maxTokenMatchSize = oldMatch;
    KrillProperties.maxTokenContextSize = oldContext;
  }

  private static int kwicTotal(ObjectNode tok) {
    int kwic = 0;
    if (tok == null)
      return 0;
    if (tok.has("left"))
      kwic += tok.get("left").size();
    if (tok.has("match"))
      kwic += tok.get("match").size();
    if (tok.has("right"))
      kwic += tok.get("right").size();
    return kwic;
  }

  @Test
  public void kwicCapRespectedWithLargeContextRequests() throws IOException {
    // Configure: explicit small cap independent from match/context sizes
    Properties p = new Properties();
    p.setProperty("krill.kwic.max.token", "51");
    p.setProperty("krill.match.max.token", "200");
    p.setProperty("krill.context.max.token", "200");
    KrillProperties.setProp(p);
    KrillProperties.updateConfigurations(p);

    // Build small index
    KrillIndex ki = new KrillIndex();
    for (String i : new String[] { "00001" }) {
      ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"), true);
    }
    ki.commit();

    // Query a common token and ask for huge context
    QueryBuilder qb = new QueryBuilder("tokens");
    Krill k = new Krill(qb.seg("s:der"));
    KrillMeta meta = k.getMeta();
    meta.setSnippets(true);
    meta.setTokens(true);
    meta.getContext().left.setLength((short) 500);
    meta.getContext().right.setLength((short) 500);

    Result r = k.apply(ki);
    assertTrue(r.getTotalResults() > 0);

    Match m = r.getMatch(0);
    ObjectNode tok = m.getSnippetTokens();

    int total = kwicTotal(tok);
    assertEquals(
        "KWIC token total should equal configured cap",
        KrillProperties.maxTokenKwicSize,
        total);
    // Ensure cap actually set as requested
    assertEquals(51, KrillProperties.maxTokenKwicSize);
  }

  @Test
  public void kwicCapRespectedForVeryLongMatches() throws IOException {
    // Cap smaller than long match length to force trimming of match/right
    Properties p = new Properties();
    p.setProperty("krill.kwic.max.token", "51");
    p.setProperty("krill.match.max.token", "500");
    p.setProperty("krill.context.max.token", "10");
    KrillProperties.setProp(p);
    KrillProperties.updateConfigurations(p);

    KrillIndex ki = new KrillIndex();
    FieldDocument fd = ki.addDoc(1, getClass().getResourceAsStream("/goe/AGX-00002.json"), false);
    ki.commit();
    assertEquals("GOE_AGX.00002", fd.getTextSigle());

    // Query that yields the long span (as used in existing tests)
    QueryBuilder qb = new QueryBuilder("tokens");
    Krill k = new Krill(qb.tag("xy/z:long"));
    KrillMeta meta = k.getMeta();
    meta.setTokens(true);
    meta.setSnippets(true);
    meta.getContext().left.setLength((short) 200);
    meta.getContext().right.setLength((short) 200);

    Result r = k.apply(ki);
    assertEquals(1, r.getTotalResults());
    Match m = r.getMatch(0);
    ObjectNode tok = m.getSnippetTokens();
    int total = kwicTotal(tok);

    assertEquals(
        "KWIC token total should equal configured cap",
        KrillProperties.maxTokenKwicSize,
        total);
    assertEquals(51, KrillProperties.maxTokenKwicSize);

  }

  @Test
  public void kwicCapExactForVariousValues_ContextHeavy() throws IOException {
    // Build index once
    KrillIndex ki = new KrillIndex();
    for (String i : new String[] { "00001" }) {
      ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"), true);
    }
    ki.commit();

    int[] caps = new int[] { 11, 31, 51, 77 };
    for (int cap : caps) {
      Properties p = new Properties();
      p.setProperty("krill.kwic.max.token", Integer.toString(cap));
      p.setProperty("krill.match.max.token", "200");
      p.setProperty("krill.context.max.token", "200");
      KrillProperties.setProp(p);
      KrillProperties.updateConfigurations(p);

      QueryBuilder qb = new QueryBuilder("tokens");
      Krill k = new Krill(qb.seg("s:der"));
      KrillMeta meta = k.getMeta();
      meta.setSnippets(true);
      meta.setTokens(true);
      meta.getContext().left.setLength((short) 500);
      meta.getContext().right.setLength((short) 500);

      Result r = k.apply(ki);
      assertTrue(r.getTotalResults() > 0);
      ObjectNode tok = r.getMatch(0).getSnippetTokens();
      int total = kwicTotal(tok);
      assertEquals("KWIC token total should equal configured cap for cap=" + cap,
          cap, total);
    }
  }

  @Test
  public void kwicCapExactForVariousValues_LongMatch() throws IOException {
    // Build index once
    KrillIndex ki = new KrillIndex();
    FieldDocument fd = ki.addDoc(1, getClass().getResourceAsStream("/goe/AGX-00002.json"), false);
    ki.commit();
    assertEquals("GOE_AGX.00002", fd.getTextSigle());

    int[] caps = new int[] { 11, 31, 51, 77 };
    for (int cap : caps) {
      Properties p = new Properties();
      p.setProperty("krill.kwic.max.token", Integer.toString(cap));
      p.setProperty("krill.match.max.token", "500");
      p.setProperty("krill.context.max.token", "200");
      KrillProperties.setProp(p);
      KrillProperties.updateConfigurations(p);

      QueryBuilder qb = new QueryBuilder("tokens");
      Krill k = new Krill(qb.tag("xy/z:long"));
      KrillMeta meta = k.getMeta();
      meta.setTokens(true);
      meta.setSnippets(true);
      meta.getContext().left.setLength((short) 500);
      meta.getContext().right.setLength((short) 500);

      Result r = k.apply(ki);
      assertEquals(1, r.getTotalResults());
      ObjectNode tok = r.getMatch(0).getSnippetTokens();
      int total = kwicTotal(tok);
      assertEquals("KWIC token total should equal configured cap for cap=" + cap,
          cap, total);
    }
  }
}
