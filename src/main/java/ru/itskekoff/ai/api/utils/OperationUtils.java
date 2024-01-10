package ru.itskekoff.ai.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class OperationUtils {
    private static final Logger logger = LoggerFactory.getLogger(OperationUtils.class);

    public static byte[] getBytesFromFile(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            logger.error("Error while getting bytes from file", e);
            return null;
        }
    }

    public static String getIdFromBytes(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(bytes);
            return new BigInteger(1, digest).toString(16).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getIdFromBase64(String base64) {
        return hashString(base64);
    }

    private static String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, digest).toString(16).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
