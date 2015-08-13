package de.ids_mannheim.korap.util;

import java.util.*;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.ids_mannheim.korap.Krill;

// Todo: Properties may be loaded twice - althogh Java may cache automatically
public class KrillProperties {

    // Logger
    private final static Logger log = LoggerFactory.getLogger(Krill.class);


    // Load properties from file
    public static Properties loadProperties () {
        return loadProperties("krill.properties");
    };


    // Load properties from file
    public static Properties loadProperties (String propFile) {
        InputStream file;
        Properties prop;
        try {
            file = new FileInputStream(propFile);
            prop = new Properties();
            prop.load(file);
        }
        catch (IOException t) {
            try {
                file = KrillProperties.class.getClassLoader()
                        .getResourceAsStream(propFile);

                if (file == null) {
                    log.error(
                            "Cannot find {}. Please create it using \"{}.info\" as template.",
                            propFile, propFile);
                    return null;
                };

                prop = new Properties();
                prop.load(file);
            }
            catch (IOException e) {
                log.error(e.getLocalizedMessage());
                return null;
            };
        };
        return prop;
    };
};
