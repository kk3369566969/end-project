package com.peerreview.dao;

import com.peerreview.domain.Submission;
import com.peerreview.domain.SubmissionRow;

import java.util.List;
import java.util.Optional;

/** 作业提交数据访问。 */
public interface SubmissionDao {

    long insert(Submission submission);

    void update(Submission submission);

    Optional<Submission> findByAssignmentAndStudent(long assignmentId, long studentId);

    /** 分配互评时需要的最小字段集合。 */
    List<Submission> listByAssignment(long assignmentId);

    /** 教师端检索用的联表视图。 */
    List<SubmissionRow> findRowsByAssignment(long assignmentId);

    List<SubmissionRow> findAllRows();

    void deleteByAssignment(long assignmentId);

    int countByAssignment(long assignmentId);
}
