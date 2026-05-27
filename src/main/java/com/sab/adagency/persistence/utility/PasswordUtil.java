package com.sab.adagency.persistence.utility;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Хешування та перевірка паролів користувачів системи (SHA-256). */
public final class PasswordUtil {

    private PasswordUtil() {}

    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 недоступний", e);
        }
    }

    public static boolean matches(String raw, String passwordHash) {
        return sha256(raw).equals(passwordHash);
    }
}
