package com.peerreview.dao.jdbc;

import com.peerreview.dao.SubmissionDao;
import com.peerreview.db.ConnectionProvider;
import com.peerreview.domain.Submission;
import com.peerreview.domain.SubmissionRow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** {@link SubmissionDao} 的 JDBC 实现。 */
public final class JdbcSubmissionDao extends AbstractJdbcDao implements SubmissionDao {

    private static final String SELECT_ROW = "SELECT s.id AS submission_id, s.assignment_id, "
            + "a.title AS assignment_title, c.id AS course_id, c.name AS course_name, s.student_id, "
            + "u.display_name AS student_name, s.content, s.submitted_at, s.updated_at "
            + "FROM submission s "
            + "JOIN assignment a ON a.id = s.assignment_id "
            + "JOIN course c ON c.id = a.course_id "
            + "JOIN app_user u ON u.id = s.student_id ";

    public JdbcSubmissionDao(ConnectionProvider connections) {
        super(connections);
    }

    private SubmissionRow mapRow(ResultSet rs) throws SQLException {
        return new SubmissionRow(
                rs.getLong("submission_id"),
                rs.getLong("assignment_id"),
                rs.getString("assignment_title"),
                rs.getLong("course_id"),
                rs.getString("course_name"),
                rs.getLong("student_id"),
                rs.getString("student_name"),
                rs.getString("content"),
                readDateTime(rs, "submitted_at"),
                readDateTime(rs, "updated_at"));
    }

    private Submission mapEntity(ResultSet rs) throws SQLException {
        Submission submission = new Submission();
        submission.setId(rs.getLong("id"));
        submission.setAssignmentId(rs.getLong("assignment_id"));
        submission.setStudentId(rs.getLong("student_id"));
        submission.setContent(rs.getString("content"));
        submission.setSubmittedAt(readDateTime(rs, "submitted_at"));
        submission.setUpdatedAt(readDateTime(rs, "updated_at"));
        return submission;
    }

    @Override
    public long insert(Submission submission) {
        String sql = "INSERT INTO submission (assignment_id, student_id, content, submitted_at, updated_at) "
                + "VALUES (?,?,?,?,?)";
        return withConnection(c -> insert(c, sql, ps -> {
            ps.setLong(1, submission.getAssignmentId());
            ps.setLong(2, submission.getStudentId());
            ps.setString(3, submission.getContent());
            setNullableTimestamp(ps, 4, submission.getSubmittedAt());
            setNullableTimestamp(ps, 5, submission.getUpdatedAt());
        }, "提交作业"));
    }

    @Override
    public void update(Submission submission) {
        withConnection(c -> update(c, "UPDATE submission SET content = ?, updated_at = ? WHERE id = ?", ps -> {
            ps.setString(1, submission.getContent());
            setNullableTimestamp(ps, 2, submission.getUpdatedAt());
            ps.setLong(3, submission.getId());
        }));
    }

    @Override
    public Optional<Submission> findByAssignmentAndStudent(long assignmentId, long studentId) {
        String sql = "SELECT id, assignment_id, student_id, content, submitted_at, updated_at FROM submission "
                + "WHERE assignment_id = ? AND student_id = ?";
        return withConnection(c -> queryOne(c, sql, ps -> {
            ps.setLong(1, assignmentId);
            ps.setLong(2, studentId);
        }, this::mapEntity));
    }

    @Override
    public List<Submission> listByAssignment(long assignmentId) {
        String sql = "SELECT id, assignment_id, student_id, content, submitted_at, updated_at FROM submission "
                + "WHERE assignment_id = ? ORDER BY submitted_at";
        return withConnection(c -> query(c, sql, ps -> ps.setLong(1, assignmentId), this::mapEntity));
    }

    @Override
    public List<SubmissionRow> findRowsByAssignment(long assignmentId) {
        return withConnection(c -> query(c, SELECT_ROW + "WHERE s.assignment_id = ? ORDER BY s.submitted_at",
                ps -> ps.setLong(1, assignmentId), this::mapRow));
    }

    @Override
    public List<SubmissionRow> findAllRows() {
        return withConnection(c -> query(c, SELECT_ROW + "ORDER BY s.submitted_at DESC", NO_PARAMS, this::mapRow));
    }

    @Override
    public void deleteByAssignment(long assignmentId) {
        withConnection(c -> update(c, "DELETE FROM submission WHERE assignment_id = ?",
                ps -> ps.setLong(1, assignmentId)));
    }

    @Override
    public int countByAssignment(long assignmentId) {
        return withConnection(c -> {
            try (var ps = c.prepareStatement("SELECT COUNT(*) FROM submission WHERE assignment_id = ?")) {
                ps.setLong(1, assignmentId);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }
}
