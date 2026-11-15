package com.peerreview.common;

/**
 * 应用内所有可预期异常的基类。
 *
 * <p>业务层不直接抛受检异常，避免上层被大量 {@code try/catch} 污染；
 * 同时保留原始异常作为 cause，便于日志定位。</p>
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
