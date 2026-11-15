package com.peerreview.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 口令摘要工具。
 *
 * <p>课程项目不引入完整加密库，这里使用 SHA-256 + 固定盐，避免数据库中存放明文口令。
 * 真实生产系统应替换为 BCrypt / Argon2 等带随机盐的慢哈希算法。</p>
 */
public final class Passwords {

    private static final String SALT = "peer-review-2026-";

    private Passwords() {
    }

    public static String hash(String rawPassword) {
        String input = SALT + Text.trimToEmpty(rawPassword);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", ex);
        }
    }

    public static boolean matches(String rawPassword, String storedHash) {
        return storedHash != null && storedHash.equals(hash(rawPassword));
    }
}
