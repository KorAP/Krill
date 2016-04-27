package de.ids_mannheim.korap.util;

import java.util.*;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.ids_mannheim.korap.Krill;

// Todo: Properties may be loaded twice - althogh Java may cache automatically
public class KrillProperties {

    public static String file  = "krill.properties";
    private static String info = "krill.info";
    private static Properties prop;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(Krill.class);


    // Load properties from file
    public static Properties loadProperties () {
        if (prop != null)
            return prop;

        prop = loadProperties(file);
        return prop;
    };


    // Load properties from file
    public static Properties loadProperties (String propFile) {
        if (propFile == null)
            return loadProperties();

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
                              "Cannot find {}. Please create it using \"{}.info\" as template.",
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
        return prop;
    };


    // Load version info from file
    public static Properties loadInfo () {
        try {
            InputStream iFile = KrillProperties.class.getClassLoader()
                .getResourceAsStream(info);

            if (iFile == null) {
                log.error("Cannot find {}.", info);
                return null;
            };

            Properties prop = new Properties();
            prop.load(iFile);
            iFile.close();
        }
        catch (IOException e) {
            log.error(e.getLocalizedMessage());
            return null;
        };
        return prop;
    };
};
