package com.peerreview.dao.jdbc;

import com.peerreview.dao.ReviewDao;
import com.peerreview.db.ConnectionProvider;
import com.peerreview.domain.Review;
import com.peerreview.domain.ReviewRow;
import com.peerreview.domain.ReviewScore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** {@link ReviewDao} 的 JDBC 实现。 */
public final class JdbcReviewDao extends AbstractJdbcDao implements ReviewDao {

    private static final String SELECT_ROW = "SELECT r.id AS review_id, r.task_id, r.submission_id, "
            + "u.display_name AS reviewer_name, r.total_score, r.comment, r.created_at "
            + "FROM review r JOIN app_user u ON u.id = r.reviewer_id ";

    private static final String INSERT_SCORE = "INSERT INTO review_score (review_id, rubric_item_id, item_name, score) "
            + "VALUES (?,?,?,?)";

    public JdbcReviewDao(ConnectionProvider connections) {
        super(connections);
    }

    private ReviewRow mapRow(ResultSet rs) throws SQLException {
        return new ReviewRow(
                rs.getLong("review_id"),
                rs.getLong("task_id"),
                rs.getLong("submission_id"),
                rs.getString("reviewer_name"),
                rs.getDouble("total_score"),
                rs.getString("comment"),
                readDateTime(rs, "created_at"));
    }

    @Override
    public long insert(Review review) {
        return inTransaction(c -> {
            String sql = "INSERT INTO review (task_id, submission_id, reviewer_id, total_score, comment, created_at) "
                    + "VALUES (?,?,?,?,?,?)";
            long reviewId = insert(c, sql, ps -> {
                ps.setLong(1, review.getTaskId());
                ps.setLong(2, review.getSubmissionId());
                ps.setLong(3, review.getReviewerId());
                ps.setDouble(4, review.getTotalScore());
                ps.setString(5, review.getComment());
                setNullableTimestamp(ps, 6, review.getCreatedAt());
            }, "保存互评");

            List<Binder> binders = new ArrayList<>();
            for (ReviewScore score : review.getScores()) {
                binders.add(ps -> {
                    ps.setLong(1, reviewId);
                    ps.setLong(2, score.getRubricItemId());
                    ps.setString(3, score.getItemName());
                    ps.setDouble(4, score.getScore());
                });
            }
            executeBatch(c, INSERT_SCORE, binders);
            return reviewId;
        });
    }

    @Override
    public Optional<Review> findByTask(long taskId) {
        String sql = "SELECT id, task_id, submission_id, reviewer_id, total_score, comment, created_at "
                + "FROM review WHERE task_id = ?";
        return withConnection(c -> {
            Optional<Review> found = queryOne(c, sql, ps -> ps.setLong(1, taskId), rs -> {
                Review review = new Review();
                review.setId(rs.getLong("id"));
                review.setTaskId(rs.getLong("task_id"));
                review.setSubmissionId(rs.getLong("submission_id"));
                review.setReviewerId(rs.getLong("reviewer_id"));
                review.setTotalScore(rs.getDouble("total_score"));
                review.setComment(rs.getString("comment"));
                review.setCreatedAt(readDateTime(rs, "created_at"));
                return review;
            });
            if (found.isPresent()) {
                found.get().setScores(findScores(c, found.get().getId()));
            }
            return found;
        });
    }

    @Override
    public List<ReviewScore> findScores(long reviewId) {
        return withConnection(c -> findScores(c, reviewId));
    }

    private List<ReviewScore> findScores(java.sql.Connection connection, long reviewId) throws SQLException {
        String sql = "SELECT id, review_id, rubric_item_id, item_name, score FROM review_score "
                + "WHERE review_id = ? ORDER BY id";
        return query(connection, sql, ps -> ps.setLong(1, reviewId), rs -> {
            ReviewScore score = new ReviewScore();
            score.setId(rs.getLong("id"));
            score.setReviewId(rs.getLong("review_id"));
            score.setRubricItemId(rs.getLong("rubric_item_id"));
            score.setItemName(rs.getString("item_name"));
            score.setScore(rs.getDouble("score"));
            return score;
        });
    }

    @Override
    public List<ReviewRow> findRowsBySubmission(long submissionId) {
        return withConnection(c -> query(c, SELECT_ROW + "WHERE r.submission_id = ? ORDER BY r.total_score DESC",
                ps -> ps.setLong(1, submissionId), this::mapRow));
    }

    @Override
    public List<ReviewRow> findRowsByAssignment(long assignmentId) {
        return withConnection(c -> query(c, SELECT_ROW + "WHERE r.submission_id IN "
                        + "(SELECT id FROM submission WHERE assignment_id = ?) ORDER BY r.submission_id, r.id",
                ps -> ps.setLong(1, assignmentId), this::mapRow));
    }

    @Override
    public int countBySubmission(long submissionId) {
        return withConnection(c -> {
            try (var ps = c.prepareStatement("SELECT COUNT(*) FROM review WHERE submission_id = ?")) {
                ps.setLong(1, submissionId);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public void deleteByAssignment(long assignmentId) {
        inTransaction(c -> {
            String scoreSql = "DELETE FROM review_score WHERE review_id IN "
                    + "(SELECT id FROM review WHERE submission_id IN "
                    + "(SELECT id FROM submission WHERE assignment_id = ?))";
            update(c, scoreSql, ps -> ps.setLong(1, assignmentId));
            update(c, "DELETE FROM review WHERE submission_id IN "
                    + "(SELECT id FROM submission WHERE assignment_id = ?)", ps -> ps.setLong(1, assignmentId));
            return null;
        });
    }
}
