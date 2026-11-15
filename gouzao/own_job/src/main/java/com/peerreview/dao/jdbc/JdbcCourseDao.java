package com.peerreview.dao.jdbc;

import com.peerreview.dao.CourseDao;
import com.peerreview.db.ConnectionProvider;
import com.peerreview.domain.Course;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** {@link CourseDao} 的 JDBC 实现。 */
public final class JdbcCourseDao extends AbstractJdbcDao implements CourseDao {

    private static final String SELECT = "SELECT id, code, name, semester, teacher_id, created_at FROM course ";

    private Course map(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setId(rs.getLong("id"));
        course.setCode(rs.getString("code"));
        course.setName(rs.getString("name"));
        course.setSemester(rs.getString("semester"));
        course.setTeacherId(rs.getLong("teacher_id"));
        course.setCreatedAt(readDateTime(rs, "created_at"));
        return course;
    }

    public JdbcCourseDao(ConnectionProvider connections) {
        super(connections);
    }

    @Override
    public List<Course> findAll() {
        return withConnection(c -> query(c, SELECT + "ORDER BY created_at DESC, id DESC", NO_PARAMS, this::map));
    }

    @Override
    public List<Course> findByTeacher(long teacherId) {
        return withConnection(c -> query(c, SELECT + "WHERE teacher_id = ? ORDER BY created_at DESC, id DESC",
                ps -> ps.setLong(1, teacherId), this::map));
    }

    @Override
    public Optional<Course> findById(long id) {
        return withConnection(c -> queryOne(c, SELECT + "WHERE id = ?", ps -> ps.setLong(1, id), this::map));
    }

    @Override
    public long insert(Course course) {
        String sql = "INSERT INTO course (code, name, semester, teacher_id, created_at) VALUES (?,?,?,?,?)";
        return withConnection(c -> insert(c, sql, ps -> {
            ps.setString(1, course.getCode());
            ps.setString(2, course.getName());
            ps.setString(3, course.getSemester());
            ps.setLong(4, course.getTeacherId());
            setNullableTimestamp(ps, 5, course.getCreatedAt());
        }, "新增课程"));
    }

    @Override
    public void update(Course course) {
        String sql = "UPDATE course SET code = ?, name = ?, semester = ? WHERE id = ?";
        withConnection(c -> update(c, sql, ps -> {
            ps.setString(1, course.getCode());
            ps.setString(2, course.getName());
            ps.setString(3, course.getSemester());
            ps.setLong(4, course.getId());
        }));
    }

    @Override
    public void delete(long id) {
        withConnection(c -> update(c, "DELETE FROM course WHERE id = ?", ps -> ps.setLong(1, id)));
    }

    @Override
    public boolean existsCode(String code, Long excludeId) {
        String sql = "SELECT COUNT(*) FROM course WHERE code = ?" + (excludeId == null ? "" : " AND id <> ?");
        return withConnection(c -> {
            try (var ps = c.prepareStatement(sql)) {
                ps.setString(1, code);
                if (excludeId != null) {
                    ps.setLong(2, excludeId);
                }
                try (var rs = ps.executeQuery()) {
                    return rs.next() && rs.getLong(1) > 0;
                }
            }
        });
    }
}
