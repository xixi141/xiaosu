package com.xiaosu.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Sha256Util {

    private Sha256Util() {
    }

    public static String hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest.digest(bytes)) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
