package com.peerreview.dao.jdbc;

import com.peerreview.dao.AssignmentDao;
import com.peerreview.db.ConnectionProvider;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.AssignmentStatus;
import com.peerreview.domain.RubricItem;
import com.peerreview.domain.ScoringMode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** {@link AssignmentDao} 的 JDBC 实现，作业与细则的写入保证在同一事务内。 */
public final class JdbcAssignmentDao extends AbstractJdbcDao implements AssignmentDao {

    private static final String SELECT = "SELECT a.id, a.course_id, a.title, a.description, a.deadline, "
            + "a.anonymous, a.reviewers_per_submission, a.scoring_mode, a.status, a.created_at, "
            + "c.name AS course_name FROM assignment a JOIN course c ON c.id = a.course_id ";

    private static final String INSERT_ASSIGNMENT = "INSERT INTO assignment (course_id, title, description, "
            + "deadline, anonymous, reviewers_per_submission, scoring_mode, status, created_at) "
            + "VALUES (?,?,?,?,?,?,?,?,?)";

    private static final String INSERT_RUBRIC = "INSERT INTO rubric_item (assignment_id, name, weight, "
            + "min_score, max_score, sort_no) VALUES (?,?,?,?,?,?)";

    public JdbcAssignmentDao(ConnectionProvider connections) {
        super(connections);
    }

    private Assignment map(ResultSet rs) throws SQLException {
        Assignment assignment = new Assignment();
        assignment.setId(rs.getLong("id"));
        assignment.setCourseId(rs.getLong("course_id"));
        assignment.setTitle(rs.getString("title"));
        assignment.setDescription(rs.getString("description"));
        assignment.setDeadline(readDateTime(rs, "deadline"));
        assignment.setAnonymous(rs.getBoolean("anonymous"));
        assignment.setReviewersPerSubmission(rs.getInt("reviewers_per_submission"));
        assignment.setScoringMode(parseMode(rs.getString("scoring_mode")));
        assignment.setStatus(parseStatus(rs.getString("status")));
        assignment.setCreatedAt(readDateTime(rs, "created_at"));
        assignment.setCourseName(rs.getString("course_name"));
        return assignment;
    }

    private ScoringMode parseMode(String value) {
        try {
            return ScoringMode.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return ScoringMode.NORMALIZED;
        }
    }

    private AssignmentStatus parseStatus(String value) {
        try {
            return AssignmentStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return AssignmentStatus.DRAFT;
        }
    }

    @Override
    public List<Assignment> findAll() {
        return withConnection(c -> query(c, SELECT + "ORDER BY a.created_at DESC, a.id DESC", NO_PARAMS, this::map));
    }

    @Override
    public List<Assignment> findByCourse(long courseId) {
        return withConnection(c -> query(c, SELECT + "WHERE a.course_id = ? ORDER BY a.created_at DESC, a.id DESC",
                ps -> ps.setLong(1, courseId), this::map));
    }

    @Override
    public Optional<Assignment> findById(long id) {
        return withConnection(c -> {
            Optional<Assignment> found = queryOne(c, SELECT + "WHERE a.id = ?", ps -> ps.setLong(1, id), this::map);
            if (found.isPresent()) {
                Assignment assignment = found.get();
                assignment.setRubricItems(findRubricItems(c, assignment.getId()));
            }
            return found;
        });
    }

    @Override
    public long insert(Assignment assignment) {
        return inTransaction(c -> {
            long id = insert(c, INSERT_ASSIGNMENT, ps -> bindAssignment(ps, assignment), "新增作业");
            assignment.setId(id);
            insertRubricItems(c, assignment);
            return id;
        });
    }

    @Override
    public void update(Assignment assignment) {
        inTransaction(c -> {
            String sql = "UPDATE assignment SET title = ?, description = ?, deadline = ?, anonymous = ?, "
                    + "reviewers_per_submission = ?, scoring_mode = ?, status = ? WHERE id = ?";
            update(c, sql, ps -> {
                ps.setString(1, assignment.getTitle());
                ps.setString(2, assignment.getDescription());
                setNullableTimestamp(ps, 3, assignment.getDeadline());
                ps.setBoolean(4, assignment.isAnonymous());
                ps.setInt(5, assignment.getReviewersPerSubmission());
                ps.setString(6, assignment.getScoringMode().name());
                ps.setString(7, assignment.getStatus().name());
                ps.setLong(8, assignment.getId());
            });
            update(c, "DELETE FROM rubric_item WHERE assignment_id = ?", ps -> ps.setLong(1, assignment.getId()));
            insertRubricItems(c, assignment);
            return null;
        });
    }

    @Override
    public void updateStatus(long assignmentId, AssignmentStatus status) {
        withConnection(c -> update(c, "UPDATE assignment SET status = ? WHERE id = ?", ps -> {
            ps.setString(1, status.name());
            ps.setLong(2, assignmentId);
        }));
    }

    @Override
    public void delete(long id) {
        inTransaction(c -> {
            update(c, "DELETE FROM rubric_item WHERE assignment_id = ?", ps -> ps.setLong(1, id));
            update(c, "DELETE FROM assignment WHERE id = ?", ps -> ps.setLong(1, id));
            return null;
        });
    }

    @Override
    public List<RubricItem> findRubricItems(long assignmentId) {
        return withConnection(c -> findRubricItems(c, assignmentId));
    }

    private List<RubricItem> findRubricItems(java.sql.Connection connection, long assignmentId) throws SQLException {
        String sql = "SELECT id, assignment_id, name, weight, min_score, max_score, sort_no "
                + "FROM rubric_item WHERE assignment_id = ? ORDER BY sort_no, id";
        return query(connection, sql, ps -> ps.setLong(1, assignmentId), rs -> {
            RubricItem item = new RubricItem();
            item.setId(rs.getLong("id"));
            item.setAssignmentId(rs.getLong("assignment_id"));
            item.setName(rs.getString("name"));
            item.setWeight(rs.getInt("weight"));
            item.setMinScore(rs.getDouble("min_score"));
            item.setMaxScore(rs.getDouble("max_score"));
            item.setSortNo(rs.getInt("sort_no"));
            return item;
        });
    }

    private void bindAssignment(java.sql.PreparedStatement ps, Assignment assignment) throws SQLException {
        ps.setLong(1, assignment.getCourseId());
        ps.setString(2, assignment.getTitle());
        ps.setString(3, assignment.getDescription());
        setNullableTimestamp(ps, 4, assignment.getDeadline());
        ps.setBoolean(5, assignment.isAnonymous());
        ps.setInt(6, assignment.getReviewersPerSubmission());
        ps.setString(7, assignment.getScoringMode().name());
        ps.setString(8, assignment.getStatus().name());
        setNullableTimestamp(ps, 9, assignment.getCreatedAt());
    }

    private void insertRubricItems(java.sql.Connection connection, Assignment assignment) throws SQLException {
        if (assignment.getRubricItems().isEmpty()) {
            return;
        }
        List<Binder> binders = new ArrayList<>();
        int sortNo = 1;
        for (RubricItem item : assignment.getRubricItems()) {
            int currentSort = sortNo++;
            item.setAssignmentId(assignment.getId());
            item.setSortNo(currentSort);
            binders.add(ps -> {
                ps.setLong(1, assignment.getId());
                ps.setString(2, item.getName());
                ps.setInt(3, item.getWeight());
                ps.setDouble(4, item.getMinScore());
                ps.setDouble(5, item.getMaxScore());
                ps.setInt(6, currentSort);
            });
        }
        executeBatch(connection, INSERT_RUBRIC, binders);
    }
}
