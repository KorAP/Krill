package de.ids_mannheim.korap.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fingerprinter {

    private final static Logger log = LoggerFactory
            .getLogger(Fingerprinter.class);

    private static MessageDigest md;

    public static String create (String key) {
        try {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
            return e.getMessage();
        };

        md.update(key.getBytes());
        String code = new String(Base64.getEncoder().encode(md.digest()));
        md.reset();
        return code;

    }

}
