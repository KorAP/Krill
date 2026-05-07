package de.ids_mannheim.korap.util;

import java.util.*;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.fasterxml.jackson.core.StreamReadConstraints.DEFAULT_MAX_STRING_LEN;

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
    
    public static int maxTokenMatchSize = 50;
    public static int maxTokenContextSize = 60;
    public static int maxCharContextSize = 500;
    public static int leftContextMaxShrink = 0;
    public static int rightContextMaxShrink = 0;
    public static int kwicMaxToken = -1;
    public static int defaultSearchContextLength = 6;
    public static int maxTextSize = DEFAULT_MAX_STRING_LEN; // Default max text size
    
    public static boolean matchExpansionIncludeContextSize = false;
    
    public static String namedVCPath = "";
    public static boolean isTest = false;

    public static String secret = "";

    
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

    public static void updateConfigurations (Properties  prop) {
        String maxTokenMatchSize = prop.getProperty("krill.match.max.token");

        // TODO:
        // Should be separated for left and right!
        String maxTokenContextSize = prop.getProperty("krill.context.max.token");

        // Maximum number of tokens to shrink from context based on match size
        // (only affects token-based contexts)
        String leftContextMaxShrink = prop.getProperty("krill.context.left.maxShrink");
        String rightContextMaxShrink = prop.getProperty("krill.context.right.maxShrink");

        String kwicMaxToken = prop.getProperty("krill.kwic.max.token");

        // EM: not implemented yet
        // String maxCharContextSize = prop.getProperty("krill.context.max.char");
        String maxCharContextSize = prop.getProperty("krill.context.max.char");
        String defaultSearchContextLength = prop.getProperty("krill.search.context.default");
        String maxTextSizeValue = prop.getProperty("krill.index.textSize.max");

        try {
            if (maxTokenMatchSize != null) {
                KrillProperties.maxTokenMatchSize = Integer
                        .parseInt(maxTokenMatchSize);
            }
            if (maxTokenContextSize != null) {
                KrillProperties.maxTokenContextSize = Integer
                        .parseInt(maxTokenContextSize);
            }
            if (maxCharContextSize != null) {
                KrillProperties.maxCharContextSize = Integer
                        .parseInt(maxCharContextSize);
            }
            if (defaultSearchContextLength != null) {
                KrillProperties.defaultSearchContextLength = Integer
                        .parseInt(defaultSearchContextLength);
            }
            if (maxTextSizeValue != null) {
                int userMaxTextLength = Integer
                        .parseInt(maxTextSizeValue);
                if (userMaxTextLength < DEFAULT_MAX_STRING_LEN) {
                    log.warn("Specified krill.index.textSize.max is too small. Using default value: "
                            + DEFAULT_MAX_STRING_LEN);
                    KrillProperties.maxTextSize = DEFAULT_MAX_STRING_LEN;
                } else {
                    KrillProperties.maxTextSize = userMaxTextLength;
                }

            }
            if (leftContextMaxShrink != null) {
                if (leftContextMaxShrink.equals("max")) {
                    KrillProperties.leftContextMaxShrink = KrillProperties.maxTokenContextSize;
                } else {
                    KrillProperties.leftContextMaxShrink = Integer
                        .parseInt(leftContextMaxShrink);
                    if (KrillProperties.leftContextMaxShrink > KrillProperties.maxTokenContextSize)
                        KrillProperties.leftContextMaxShrink = KrillProperties.maxTokenContextSize;
                    else if (KrillProperties.leftContextMaxShrink < 0)
                        KrillProperties.leftContextMaxShrink = 0;
                };
            };
            if (rightContextMaxShrink != null) {
                if (rightContextMaxShrink.equals("max")) {
                    KrillProperties.rightContextMaxShrink = KrillProperties.maxTokenContextSize;
                } else {
                    KrillProperties.rightContextMaxShrink = Integer
                        .parseInt(rightContextMaxShrink);
                    if (KrillProperties.rightContextMaxShrink > KrillProperties.maxTokenContextSize)
                        KrillProperties.rightContextMaxShrink = KrillProperties.maxTokenContextSize;
                    else if (KrillProperties.rightContextMaxShrink < 0)
                        KrillProperties.rightContextMaxShrink = 0;
                };
            };

            if (kwicMaxToken != null) {
                KrillProperties.kwicMaxToken = Integer.parseInt(kwicMaxToken);

                if (leftContextMaxShrink != null || rightContextMaxShrink != null) {
                    log.warn("krill.kwic.max.token is set: individual "
                             + "krill.context.left.maxShrink / krill.context.right.maxShrink "
                             + "values will be ignored");
                };

                int totalAllowance = KrillProperties.maxTokenMatchSize
                    + 2 * KrillProperties.maxTokenContextSize;
                int totalShrink = Math.max(0,
                    Math.min(totalAllowance - KrillProperties.kwicMaxToken,
                             2 * KrillProperties.maxTokenContextSize));
                KrillProperties.leftContextMaxShrink = totalShrink / 2;
                KrillProperties.rightContextMaxShrink = totalShrink - totalShrink / 2;
            };

        }
        catch (NumberFormatException e) {
            log.error("A Krill property expects numerical values: "
                    + e.getMessage());
        };
        
        String p = prop.getProperty("krill.test", "false");
        isTest = Boolean.parseBoolean(p);
        
        namedVCPath = prop.getProperty("krill.namedVC", "");
        
        String matchExpansion = prop.getProperty(
                "krill.match." + "expansion.includeContextSize", "false");
        matchExpansionIncludeContextSize = Boolean.parseBoolean(matchExpansion);

        secret = prop.getProperty("krill.secretB64", "");
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
