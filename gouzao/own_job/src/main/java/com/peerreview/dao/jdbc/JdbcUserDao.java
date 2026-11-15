package com.peerreview.dao.jdbc;

import com.peerreview.dao.UserDao;
import com.peerreview.db.ConnectionProvider;
import com.peerreview.domain.Role;
import com.peerreview.domain.User;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** {@link UserDao} 的 JDBC 实现。 */
public final class JdbcUserDao extends AbstractJdbcDao implements UserDao {

    private static final String SELECT = "SELECT id, username, password_hash, display_name, role, created_at "
            + "FROM app_user ";

    private final RowMapper<User> mapper = rs -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setDisplayName(rs.getString("display_name"));
        user.setRole(Role.fromCode(rs.getString("role")));
        user.setCreatedAt(readDateTime(rs, "created_at"));
        return user;
    };

    public JdbcUserDao(ConnectionProvider connections) {
        super(connections);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return withConnection(c ->
                queryOne(c, SELECT + "WHERE username = ?", ps -> ps.setString(1, username), mapper));
    }

    @Override
    public Optional<User> findById(long id) {
        return withConnection(c ->
                queryOne(c, SELECT + "WHERE id = ?", ps -> ps.setLong(1, id), mapper));
    }

    @Override
    public List<User> findByRole(Role role) {
        return withConnection(c -> query(c, SELECT + "WHERE role = ? ORDER BY id",
                ps -> ps.setString(1, role.name()), mapper));
    }

    @Override
    public boolean existsByUsername(String username) {
        return withConnection(c -> queryOne(c, "SELECT 1 FROM app_user WHERE username = ?",
                ps -> ps.setString(1, username), rs -> Boolean.TRUE)).isPresent();
    }

    @Override
    public User insert(User user) {
        String sql = "INSERT INTO app_user (username, password_hash, display_name, role, created_at) "
                + "VALUES (?,?,?,?,?)";
        return withConnection(c -> {
            LocalDateTime createdAt = user.getCreatedAt() == null ? LocalDateTime.now() : user.getCreatedAt();
            long id = insert(c, sql, ps -> {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getDisplayName());
                ps.setString(4, user.getRole().code());
                ps.setTimestamp(5, Timestamp.valueOf(createdAt));
            }, "创建用户");
            user.setId(id);
            user.setCreatedAt(createdAt);
            return user;
        });
    }
}
