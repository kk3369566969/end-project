package com.peerreview.dao.jdbc;

import com.peerreview.common.DataAccessException;
import com.peerreview.db.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC DAO 的公共骨架：统一连接的获取/关闭、语句绑定、结果映射与异常转换。
 *
 * <p>子类只关心「SQL 长什么样」和「一行怎么变成对象」，
 * 不再重复写 try-with-resources 与 SQLException 包装，降低样板代码与出错概率。</p>
 */
public abstract class AbstractJdbcDao {

    @FunctionalInterface
    protected interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    protected interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    /** 空绑定器，用于无参 SQL。 */
    protected static final Binder NO_PARAMS = statement -> {
    };

    protected final ConnectionProvider connections;

    protected AbstractJdbcDao(ConnectionProvider connections) {
        this.connections = connections;
    }

    /** 在一条连接内执行工作单元，自动关闭连接并把 SQLException 转成业务可读异常。 */
    protected final <T> T withConnection(SqlWork<T> work) {
        try (Connection connection = connections.getConnection()) {
            return work.run(connection);
        } catch (SQLException ex) {
            throw new DataAccessException("数据库操作失败：" + ex.getMessage(), ex);
        }
    }

    /**
     * 在单个事务内执行工作单元：任一步失败则整体回滚，避免出现「作业已改、细则丢失」的中间态。
     */
    protected final <T> T inTransaction(SqlWork<T> work) {
        return withConnection(connection -> {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        });
    }

    protected final long insert(Connection connection, String sql, Binder binder, String action)
            throws SQLException {        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException(action + "失败：未获取到自增主键");
    }

    protected final int update(Connection connection, String sql, Binder binder) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    protected final <T> List<T> query(Connection connection, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        }
    }

    protected final <T> Optional<T> queryOne(Connection connection, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        List<T> rows = query(connection, sql, binder, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** 批量执行（用于评分细则整体重写）。 */
    protected final void executeBatch(Connection connection, String sql, List<Binder> binders) throws SQLException {
        if (binders.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Binder binder : binders) {
                binder.bind(ps);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    protected static void setNullableTimestamp(PreparedStatement ps, int index, LocalDateTime value)
            throws SQLException {
        if (value == null) {
            ps.setTimestamp(index, null);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    protected static LocalDateTime readDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
