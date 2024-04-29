package de.ids_mannheim.korap.util;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KrillConfiguration {

 // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillConfiguration.class);

    public int maxMatchTokens = 50;
    
    public static KrillConfiguration createDefaultConfiguration() {
        return new KrillConfiguration();
    }
    
    public static KrillConfiguration createNewConfiguration (Properties prop) {
        KrillConfiguration config = new KrillConfiguration();
        
         String maxMatchTokens = prop.getProperty("krill.match.max_token_size",
                 "50");

         if (maxMatchTokens != null) {
             try {
                 config.maxMatchTokens = Integer.parseInt(maxMatchTokens);
             }
             catch (NumberFormatException e) {
                 log.error(
                         "krill.match.max_token_size expected to be a numerical value");
             };
         };
         return config;
     }
    
    public int getMaxMatchTokens () {
        return maxMatchTokens;
    }
    
    public void setMaxMatchTokens (int maxMatchTokens) {
        this.maxMatchTokens = maxMatchTokens;
    }
    
    
}
