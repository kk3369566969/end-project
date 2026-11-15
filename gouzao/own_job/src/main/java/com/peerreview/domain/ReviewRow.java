package com.peerreview.domain;

import java.time.LocalDateTime;

/** 教师端查看「某份提交收到了哪些评语」时的读模型。 */
public record ReviewRow(
        long reviewId,
        long taskId,
        long submissionId,
        String reviewerName,
        double totalScore,
        String comment,
        LocalDateTime createdAt) {

    public String scoreLabel() {
        return RubricItem.trim(totalScore);
    }
}
