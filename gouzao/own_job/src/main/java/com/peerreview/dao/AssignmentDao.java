package com.peerreview.dao;

import com.peerreview.domain.Assignment;
import com.peerreview.domain.AssignmentStatus;
import com.peerreview.domain.RubricItem;

import java.util.List;
import java.util.Optional;

/**
 * 作业与评分细则数据访问。
 *
 * <p>细则项生命周期完全从属于作业，因此不单独暴露 DAO，避免调用方绕过作业直接改细则。</p>
 */
public interface AssignmentDao {

    List<Assignment> findAll();

    List<Assignment> findByCourse(long courseId);

    Optional<Assignment> findById(long id);

    /** 插入作业及其评分细则。 */
    long insert(Assignment assignment);

    /** 更新作业字段，并整体重写评分细则。 */
    void update(Assignment assignment);

    void updateStatus(long assignmentId, AssignmentStatus status);

    void delete(long id);

    List<RubricItem> findRubricItems(long assignmentId);
}
