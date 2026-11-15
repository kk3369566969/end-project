package com.peerreview.domain;

import java.time.LocalDateTime;

/**
 * 学生端「互评任务」列表的读模型。
 *
 * <p>盲评要求界面拿不到被评学生的真实姓名，因此这里默认只给出 {@code anonymous} 标记，
 * 由展示层调用 {@link #authorLabel()} 决定是否脱敏，业务层不参与「是否显示姓名」的决策。</p>
 */
public record TaskRow(
        long taskId,
        long assignmentId,
        String assignmentTitle,
        String courseName,
        long submissionId,
        long authorId,
        String authorName,
        String content,
        TaskStatus status,
        boolean anonymous,
        LocalDateTime completedAt,
        Double totalScore) {

    public String authorLabel() {
        return anonymous ? "匿名同学" : authorName;
    }

    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }

    public String statusLabel() {
        return status == null ? "" : status.label();
    }

    public String scoreLabel() {
        return totalScore == null ? "—" : RubricItem.trim(totalScore);
    }
}
