package com.peerreview.dao;

import com.peerreview.domain.Review;
import com.peerreview.domain.ReviewRow;
import com.peerreview.domain.ReviewScore;

import java.util.List;
import java.util.Optional;

/** 互评记录数据访问。 */
public interface ReviewDao {

    /** 保存互评主记录与各维度得分（同一事务内完成）。 */
    long insert(Review review);

    Optional<Review> findByTask(long taskId);

    List<ReviewScore> findScores(long reviewId);

    List<ReviewRow> findRowsBySubmission(long submissionId);

    /** 统计与评语检索用：一次取回某作业下所有互评。 */
    List<ReviewRow> findRowsByAssignment(long assignmentId);

    int countBySubmission(long submissionId);

    void deleteByAssignment(long assignmentId);
}
