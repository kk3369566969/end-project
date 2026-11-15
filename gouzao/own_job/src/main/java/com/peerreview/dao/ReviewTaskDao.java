package com.peerreview.dao;

import com.peerreview.domain.ReviewTask;
import com.peerreview.domain.TaskRow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 互评任务数据访问。 */
public interface ReviewTaskDao {

    long insert(ReviewTask task);

    Optional<ReviewTask> findById(long id);

    List<ReviewTask> findByAssignment(long assignmentId);

    /** 学生端「我的互评任务」列表（联表带出作业标题、课程、作业内容与已得总分）。 */
    List<TaskRow> findRowsByReviewer(long reviewerId);

    void markCompleted(long taskId, LocalDateTime completedAt);

    void deleteByAssignment(long assignmentId);

    int countByAssignment(long assignmentId);

    /** 冲突检测：同一评审人是否已领到同一份作业。 */
    boolean exists(long submissionId, long reviewerId);
}
