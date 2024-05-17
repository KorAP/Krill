package de.ids_mannheim.korap.util;

import java.util.*;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Todo: Properties may be loaded twice - although Java may cache automatically
 * 
 * @author diewald, margaretha
 *
 */
public class KrillProperties {

    public static final String DEFAULT_PROPERTIES_LOCATION = "krill.properties";
    public static final String DEFAULT_INFO_LOCATION = "krill.info";
    private static Properties prop, info;
    
    public static int maxMatchTokens = 50;
    public static int maxTokenContextLength = 60;
    public static int maxCharContextLength = 500;
    
    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillProperties.class);

    // Load properties from file
    public static Properties loadDefaultProperties () {
        if (prop != null)
            return prop;

        prop = loadProperties(DEFAULT_PROPERTIES_LOCATION);
        return prop;
    };


    // Load properties from file
    public static Properties loadProperties (String propFile) {
        if (propFile == null)
            return loadDefaultProperties();

        InputStream iFile;
        try {
            iFile = new FileInputStream(propFile);
            prop = new Properties();
            prop.load(iFile);

        }
        catch (IOException t) {
            try {
                iFile = KrillProperties.class.getClassLoader()
                        .getResourceAsStream(propFile);
                if (iFile == null) {
                    log.warn(
                            "Cannot find {}. Please create it using "
                            + "\"src/main/resources/krill.properties.info\" as template.",
                            propFile, propFile);
                    return null;
                };

                prop = new Properties();
                prop.load(iFile);
                iFile.close();
            }
            catch (IOException e) {
                log.error(e.getLocalizedMessage());
                return null;
            };
        };
        updateConfigurations(prop);
        return prop;
    };

    private static void updateConfigurations (Properties  prop) {
        String maxMatchTokens = prop.getProperty("krill.max.token.match");
        String maxContextTokens = prop.getProperty("krill.max.token.context");

        try {
            if (maxMatchTokens != null) {
                KrillProperties.maxMatchTokens = Integer
                        .parseInt(maxMatchTokens);
            }
            if (maxContextTokens != null) {
                KrillProperties.maxTokenContextLength = Integer
                        .parseInt(maxContextTokens);
            }
        }
        catch (NumberFormatException e) {
            log.error("A Krill property expects numerical values: "
                    + e.getMessage());
        };
    }
    

    // Load version info from file
    public static Properties loadInfo () {
        try {
            info = new Properties();
            InputStream iFile = KrillProperties.class.getClassLoader()
                    .getResourceAsStream(DEFAULT_INFO_LOCATION);

            if (iFile == null) {
                log.error("Cannot find {}.", DEFAULT_INFO_LOCATION);
                return null;
            };

            info.load(iFile);
            iFile.close();
        }
        catch (IOException e) {
            log.error(e.getLocalizedMessage());
            return null;
        };
        return info;
    };
    
    public static void setProp (Properties prop) {
        KrillProperties.prop = prop;
    }
};
