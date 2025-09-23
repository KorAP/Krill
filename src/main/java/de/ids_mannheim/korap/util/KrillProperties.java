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
    // Total KWIC size cap (match + left + right)
    // Default derived from legacy match/context sizes
    public static int maxTokenKwicSize = (2 * maxTokenContextSize) + maxTokenMatchSize;
    public static int maxCharContextSize = 500;
    public static int defaultSearchContextLength = 6;
    public static int maxTextSize = DEFAULT_MAX_STRING_LEN; // Default max text size
    
    public static boolean matchExpansionIncludeContextSize = false;
    // Testing/diagnostic flag: whether kwic cap was derived from deprecated properties
    public static boolean kwicDerivedFromDeprecatedProperties = false;
    
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
        String maxTokenContextSize = prop.getProperty("krill.context.max.token");
        String maxTokenKwicSize = prop.getProperty("krill.kwic.max.token");
        // EM: not implemented yet
//        String maxCharContextSize = prop.getProperty("krill.context.max.char");
        String defaultSearchContextLength = prop.getProperty("krill.search.context.default");
        String maxTextSizeValue = prop.getProperty("krill.index.textSize.max");
        // Removed properties: krill.kwic.max.char, krill.kwic.enforce.html, krill.snippet.safeCharBounds

        try {
            if (maxTokenMatchSize != null) {
                KrillProperties.maxTokenMatchSize = Integer.parseInt(maxTokenMatchSize);
            }
            if (maxTokenContextSize != null) {
                KrillProperties.maxTokenContextSize = Integer.parseInt(maxTokenContextSize);
            }
            if (maxTokenKwicSize != null) {
                KrillProperties.maxTokenKwicSize = Integer.parseInt(maxTokenKwicSize);
            }
//            if (maxCharContextSize != null) {
//                KrillProperties.maxCharContextSize = Integer
//                        .parseInt(maxCharContextSize);
//            }
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
            // Removed handling for deprecated snippet properties
        }
        catch (NumberFormatException e) {
            log.error("A Krill property expects numerical values: "
                    + e.getMessage());
        };

        // Compute kwic cap from deprecated properties if explicit kwic cap is unset
        boolean kwicCapExplicit = (maxTokenKwicSize != null && !maxTokenKwicSize.isEmpty());
        boolean deprecatedProvided = (maxTokenMatchSize != null && !maxTokenMatchSize.isEmpty())
                || (maxTokenContextSize != null && !maxTokenContextSize.isEmpty());

        if (!kwicCapExplicit && deprecatedProvided) {
            int computedKwic = (2 * KrillProperties.maxTokenContextSize) + KrillProperties.maxTokenMatchSize;
            KrillProperties.maxTokenKwicSize = computedKwic;
            KrillProperties.kwicDerivedFromDeprecatedProperties = true;
            log.warn(
                "Deprecated properties 'krill.match.max.token' and 'krill.context.max.token' are used to derive 'krill.kwic.max.token' = {} (2*{} + {}). Please set 'krill.kwic.max.token' and remove deprecated properties.",
                computedKwic, KrillProperties.maxTokenContextSize, KrillProperties.maxTokenMatchSize
            );
        }
        else if (kwicCapExplicit && deprecatedProvided) {
            KrillProperties.kwicDerivedFromDeprecatedProperties = false;
            log.warn(
                "Deprecated properties 'krill.match.max.token' and 'krill.context.max.token' are ignored because 'krill.kwic.max.token' is set. Please remove deprecated properties."
            );
        }
        else {
            KrillProperties.kwicDerivedFromDeprecatedProperties = false;
        }

        // Keep default unless explicitly overridden by property
        
        String p = prop.getProperty("krill.test", "false");
        isTest = Boolean.parseBoolean(p);
        
        namedVCPath = prop.getProperty("krill.namedVC", "");
        
        String matchExpansion = prop.getProperty(
                "krill.match." + "expansion.includeContextSize", "false");
        matchExpansionIncludeContextSize = Boolean.parseBoolean(matchExpansion);

        secret = prop.getProperty("krill.secretB64", "");

        log.info("Effective krill.kwic.max.token = {}", KrillProperties.maxTokenKwicSize);
    }

    public static int getMaxTokenKwicSize() {
        return maxTokenKwicSize;
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
