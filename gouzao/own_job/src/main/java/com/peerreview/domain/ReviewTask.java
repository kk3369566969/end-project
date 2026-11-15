package com.peerreview.domain;

import java.time.LocalDateTime;

/** 一条互评任务：某位评审人需要评审某一份提交。 */
public class ReviewTask {

    private long id;
    private long assignmentId;
    private long submissionId;
    private long reviewerId;
    private TaskStatus status = TaskStatus.PENDING;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public ReviewTask() {
    }

    public ReviewTask(long assignmentId, long submissionId, long reviewerId) {
        this.assignmentId = assignmentId;
        this.submissionId = submissionId;
        this.reviewerId = reviewerId;
        this.createdAt = LocalDateTime.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(long submissionId) {
        this.submissionId = submissionId;
    }

    public long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }
}
