package de.ids_mannheim.korap.util;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KrillConfiguration {

 // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillConfiguration.class);

    public int maxMatchTokens = 50;
    public int maxContextTokens = 25;
    public int maxContextChars = 500;
    
    public static KrillConfiguration createDefaultConfiguration() {
        return new KrillConfiguration();
    }
    
    public static KrillConfiguration createNewConfiguration (Properties prop) {
        KrillConfiguration config = new KrillConfiguration();

        String maxMatchTokens = prop.getProperty("krill.max.match.tokens",
                "50");
        String maxContextTokens = prop.getProperty("krill.max.context.tokens",
                "500");

        try {
            config.maxMatchTokens = Integer.parseInt(maxMatchTokens);
            config.maxContextTokens = Integer.parseInt(maxContextTokens);
        }
        catch (NumberFormatException e) {
            log.error("A Krill property expects numerical values: "+e.getMessage());
        };
        return config;
    }
    
    public int getMaxMatchTokens () {
        return maxMatchTokens;
    }
    
    public void setMaxMatchTokens (int maxMatchTokens) {
        this.maxMatchTokens = maxMatchTokens;
    }
    
    public int getMaxContextTokens () {
        return maxContextTokens;
    }
    
    public void setMaxContextTokens (int maxContextTokens) {
        this.maxContextTokens = maxContextTokens;
    }
    
}
