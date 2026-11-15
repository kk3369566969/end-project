package com.peerreview.db;

import com.peerreview.common.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 基于 {@link DriverManager} 的连接实现：每次调用取一条新连接，用完即关。
 *
 * <p>课程项目并发量小，先保证实现简单、行为可预期；
 * 需要连接池时只需新增一个 {@link ConnectionProvider} 实现，调用方无需改动。</p>
 */
public final class DriverManagerConnectionProvider implements ConnectionProvider {

    private final String url;
    private final String username;
    private final String password;
    private final String description;

    public DriverManagerConnectionProvider(String url, String username, String password, String description) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.description = description;
    }

    /**
     * 探测连接是否可用，便于启动阶段给出明确错误提示。
     *
     * @throws DataAccessException 连不上时抛出，携带可读原因
     */
    public void verify() {
        try (Connection ignored = getConnection()) {
            // 仅验证能否建立连接
        } catch (SQLException ex) {
            throw new DataAccessException(friendlyMessage(ex), ex);
        }
    }

    private String friendlyMessage(SQLException ex) {
        return "无法连接数据库：" + description + "\n"
                + "原因：" + ex.getMessage() + "\n"
                + diagnose(ex);
    }

    /**
     * 把 JDBC 错误码翻译成可执行的排查建议。
     *
     * <p>不同厂商错误码不通用，这里以 SQLState 为主、厂商码为辅：认证失败与「库不存在」
     * 是最常见的两类启动故障，单独给出结论，避免用户面对一句「请检查连接参数」无从下手。</p>
     */
    private String diagnose(SQLException ex) {
        String state = ex.getSQLState() == null ? "" : ex.getSQLState();
        int code = ex.getErrorCode();
        String message = ex.getMessage() == null ? "" : ex.getMessage();

        if (code == 1045 || "28000".equals(state)) {
            return "诊断：用户名或密码错误。请核对 config/config.properties 中的 db.username 与 db.password。";
        }
        if (code == 1049 || message.contains("Unknown database")) {
            return "诊断：目标数据库不存在。启动时会尝试自动创建，若仍失败请手动执行建库语句。";
        }
        if (code == 1044 || code == 1142) {
            return "诊断：当前账号无权访问该数据库，请用具有建表权限的账号。";
        }
        if ("08S01".equals(state) || "08001".equals(state) || message.contains("Communications link failure")) {
            return "诊断：无法建立网络连接。请确认数据库服务已启动、端口正确，并检查防火墙。";
        }
        if (message.contains("Public Key Retrieval")) {
            return "诊断：MySQL 8 的认证方式需要允许获取公钥，请在 db.url 中加上 allowPublicKeyRetrieval=true。";
        }
        return "诊断：请检查 config/config.properties 中的连接参数，或确认数据库服务已启动。";
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public String describe() {
        return description;
    }

    @Override
    public void close() {
        // 无池化资源需要释放
    }
}
