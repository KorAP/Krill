package de.ids_mannheim.korap.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fingerprinter {

    private final static Logger log = LoggerFactory
            .getLogger(Fingerprinter.class);

    public static String create (String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());
            return new String(Base64.getUrlEncoder().encode(md.digest()));
        }
        catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
            return e.getMessage();
        }
    }
}
