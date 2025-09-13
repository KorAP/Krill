package de.ids_mannheim.korap.util;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestKrillPropertiesKwicFallback {

    private int oldMatch;
    private int oldContext;
    private int oldKwic;

    @Before
    public void saveOldValues() {
        oldMatch = KrillProperties.maxTokenMatchSize;
        oldContext = KrillProperties.maxTokenContextSize;
        oldKwic = KrillProperties.maxTokenKwicSize;
    }

    @After
    public void restoreOldValues() {
        KrillProperties.maxTokenMatchSize = oldMatch;
        KrillProperties.maxTokenContextSize = oldContext;
        KrillProperties.maxTokenKwicSize = oldKwic;
    }

    private static class MemoryAppender extends AbstractAppender {
        private final java.util.ArrayList<LogEvent> events = new java.util.ArrayList<>();

        protected MemoryAppender(String name) {
            super(name, (Filter) null, (Layout<? extends Serializable>) null, false);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }

        public List<LogEvent> getEvents() {
            return events;
        }
    }

    @Test
    public void fallbackFromDeprecatedPropertiesComputesKwicAndLogsWarning() {
        Properties p = new Properties();
        p.setProperty("krill.match.max.token", "30");
        p.setProperty("krill.context.max.token", "10");
        // Intentionally omit krill.kwic.max.token

        // Attach a memory appender to capture warnings
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig rootLoggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

        Level oldLevel = rootLoggerConfig.getLevel();
        MemoryAppender appender = new MemoryAppender("TestKwicFallbackAppender");
        appender.start();
        config.addAppender(appender);
        rootLoggerConfig.addAppender(appender, Level.WARN, null);
        rootLoggerConfig.setLevel(Level.ALL);
        ctx.updateLoggers();

        try {
            KrillProperties.updateConfigurations(p);

            assertEquals(30, KrillProperties.maxTokenMatchSize);
            assertEquals(10, KrillProperties.maxTokenContextSize);
            assertEquals(50, KrillProperties.maxTokenKwicSize);

            assertTrue(KrillProperties.kwicDerivedFromDeprecatedProperties);

            boolean warned = false;
            for (LogEvent e : appender.getEvents()) {
                if (e.getLevel().intLevel() >= Level.WARN.intLevel()
                        && e.getMessage().getFormattedMessage().contains("Deprecated properties 'krill.match.max.token'")) {
                    warned = true;
                    break;
                }
            }
            assertTrue("Expected deprecation warning to be logged", warned);
        }
        finally {
            rootLoggerConfig.removeAppender(appender.getName());
            rootLoggerConfig.setLevel(oldLevel);
            ctx.updateLoggers();
            appender.stop();
        }
    }

    @Test
    public void explicitKwicOverridesDeprecatedProperties() {
        Properties p = new Properties();
        p.setProperty("krill.match.max.token", "30");
        p.setProperty("krill.context.max.token", "10");
        p.setProperty("krill.kwic.max.token", "77");

        KrillProperties.updateConfigurations(p);

        assertEquals(30, KrillProperties.maxTokenMatchSize);
        assertEquals(10, KrillProperties.maxTokenContextSize);
        assertEquals(77, KrillProperties.maxTokenKwicSize);
    }
}


