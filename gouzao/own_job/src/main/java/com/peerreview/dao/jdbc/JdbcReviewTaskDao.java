package com.peerreview.dao.jdbc;

import com.peerreview.dao.ReviewTaskDao;
import com.peerreview.db.ConnectionProvider;
import com.peerreview.domain.ReviewTask;
import com.peerreview.domain.TaskRow;
import com.peerreview.domain.TaskStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** {@link ReviewTaskDao} 的 JDBC 实现。 */
public final class JdbcReviewTaskDao extends AbstractJdbcDao implements ReviewTaskDao {

    private static final String SELECT_TASK = "SELECT id, assignment_id, submission_id, reviewer_id, status, "
            + "created_at, completed_at FROM review_task ";

    /** 学生端任务列表：并上作业、课程、提交内容与已完成互评的总分。 */
    private static final String SELECT_ROW = "SELECT t.id AS task_id, t.assignment_id, a.title AS assignment_title, "
            + "c.name AS course_name, t.submission_id, s.student_id AS author_id, u.display_name AS author_name, "
            + "s.content, t.status, a.anonymous, t.completed_at, r.total_score "
            + "FROM review_task t "
            + "JOIN assignment a ON a.id = t.assignment_id "
            + "JOIN course c ON c.id = a.course_id "
            + "JOIN submission s ON s.id = t.submission_id "
            + "JOIN app_user u ON u.id = s.student_id "
            + "LEFT JOIN review r ON r.task_id = t.id ";

    public JdbcReviewTaskDao(ConnectionProvider connections) {
        super(connections);
    }

    private ReviewTask mapTask(ResultSet rs) throws SQLException {
        ReviewTask task = new ReviewTask();
        task.setId(rs.getLong("id"));
        task.setAssignmentId(rs.getLong("assignment_id"));
        task.setSubmissionId(rs.getLong("submission_id"));
        task.setReviewerId(rs.getLong("reviewer_id"));
        task.setStatus(TaskStatus.valueOf(rs.getString("status")));
        task.setCreatedAt(readDateTime(rs, "created_at"));
        task.setCompletedAt(readDateTime(rs, "completed_at"));
        return task;
    }

    private TaskRow mapRow(ResultSet rs) throws SQLException {
        double total = rs.getDouble("total_score");
        Double score = rs.wasNull() ? null : total;
        return new TaskRow(
                rs.getLong("task_id"),
                rs.getLong("assignment_id"),
                rs.getString("assignment_title"),
                rs.getString("course_name"),
                rs.getLong("submission_id"),
                rs.getLong("author_id"),
                rs.getString("author_name"),
                rs.getString("content"),
                TaskStatus.valueOf(rs.getString("status")),
                rs.getBoolean("anonymous"),
                readDateTime(rs, "completed_at"),
                score);
    }

    @Override
    public long insert(ReviewTask task) {
        String sql = "INSERT INTO review_task (assignment_id, submission_id, reviewer_id, status, created_at) "
                + "VALUES (?,?,?,?,?)";
        return withConnection(c -> insert(c, sql, ps -> {
            ps.setLong(1, task.getAssignmentId());
            ps.setLong(2, task.getSubmissionId());
            ps.setLong(3, task.getReviewerId());
            ps.setString(4, task.getStatus().name());
            setNullableTimestamp(ps, 5, task.getCreatedAt());
        }, "创建互评任务"));
    }

    @Override
    public Optional<ReviewTask> findById(long id) {
        return withConnection(c -> queryOne(c, SELECT_TASK + "WHERE id = ?", ps -> ps.setLong(1, id), this::mapTask));
    }

    @Override
    public List<ReviewTask> findByAssignment(long assignmentId) {
        return withConnection(c -> query(c, SELECT_TASK + "WHERE assignment_id = ? ORDER BY id",
                ps -> ps.setLong(1, assignmentId), this::mapTask));
    }

    @Override
    public List<TaskRow> findRowsByReviewer(long reviewerId) {
        return withConnection(c -> query(c, SELECT_ROW + "WHERE t.reviewer_id = ? ORDER BY t.status, t.id DESC",
                ps -> ps.setLong(1, reviewerId), this::mapRow));
    }

    @Override
    public void markCompleted(long taskId, LocalDateTime completedAt) {
        withConnection(c -> update(c, "UPDATE review_task SET status = ?, completed_at = ? WHERE id = ?", ps -> {
            ps.setString(1, TaskStatus.COMPLETED.name());
            setNullableTimestamp(ps, 2, completedAt);
            ps.setLong(3, taskId);
        }));
    }

    @Override
    public void deleteByAssignment(long assignmentId) {
        withConnection(c -> update(c, "DELETE FROM review_task WHERE assignment_id = ?",
                ps -> ps.setLong(1, assignmentId)));
    }

    @Override
    public int countByAssignment(long assignmentId) {
        return withConnection(c -> {
            try (var ps = c.prepareStatement("SELECT COUNT(*) FROM review_task WHERE assignment_id = ?")) {
                ps.setLong(1, assignmentId);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public boolean exists(long submissionId, long reviewerId) {
        return withConnection(c -> {
            try (var ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM review_task WHERE submission_id = ? AND reviewer_id = ?")) {
                ps.setLong(1, submissionId);
                ps.setLong(2, reviewerId);
                try (var rs = ps.executeQuery()) {
                    return rs.next() && rs.getLong(1) > 0;
                }
            }
        });
    }
}
