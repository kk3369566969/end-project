package com.peerreview.common;

/**
 * 数据访问异常：把检查型的 {@link java.sql.SQLException} 统一转换为运行时异常，
 * 让 DAO 之外的分层不必感知 JDBC 细节。
 */
public class DataAccessException extends AppException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
