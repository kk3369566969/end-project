package com.peerreview.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 一次已完成的互评：包含各项维度得分、加权总分与人工评语。
 *
 * <p>总分在保存时由 {@code ScoreCalculator} 计算并落库，统计时直接读取，避免口径漂移。</p>
 */
public class Review {

    private long id;
    private long taskId;
    private long submissionId;
    private long reviewerId;
    private double totalScore;
    private String comment;
    private LocalDateTime createdAt;
    private List<ReviewScore> scores = new ArrayList<>();

    /** 非持久化字段：列表/详情展示用。 */
    private String reviewerName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
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

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ReviewScore> getScores() {
        return scores;
    }

    public void setScores(List<ReviewScore> scores) {
        this.scores = scores == null ? new ArrayList<>() : scores;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }
}
