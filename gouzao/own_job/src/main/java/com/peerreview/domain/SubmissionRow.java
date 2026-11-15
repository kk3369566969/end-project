package com.peerreview.domain;

import java.time.LocalDateTime;

/**
 * 提交列表的读模型（提交 + 学生 + 课程/作业信息），供教师端检索与统计使用。
 *
 * <p>写操作走实体 {@link Submission}，展示与过滤使用该不可变视图，
 * 避免界面拿着实体到处改导致状态不一致。</p>
 */
public record SubmissionRow(
        long submissionId,
        long assignmentId,
        String assignmentTitle,
        long courseId,
        String courseName,
        long studentId,
        String studentName,
        String content,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt) {

    public boolean isEdited() {
        return updatedAt != null && submittedAt != null && !updatedAt.equals(submittedAt);
    }

    /** 匿名场景下的展示名。 */
    public String authorLabel(boolean anonymous) {
        return anonymous ? "匿名作业 #" + submissionId : studentName;
    }
}
