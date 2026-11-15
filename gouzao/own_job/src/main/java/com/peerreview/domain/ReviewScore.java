package com.peerreview.domain;

/**
 * 单个维度的打分记录。
 *
 * <p>刻意冗余保存 {@code itemName}：即使教师事后调整了评分细则，历史互评记录仍然可读、可追溯。</p>
 */
public class ReviewScore {

    private long id;
    private long reviewId;
    private long rubricItemId;
    private String itemName;
    private double score;

    public ReviewScore() {
    }

    public ReviewScore(long rubricItemId, String itemName, double score) {
        this.rubricItemId = rubricItemId;
        this.itemName = itemName;
        this.score = score;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getReviewId() {
        return reviewId;
    }

    public void setReviewId(long reviewId) {
        this.reviewId = reviewId;
    }

    public long getRubricItemId() {
        return rubricItemId;
    }

    public void setRubricItemId(long rubricItemId) {
        this.rubricItemId = rubricItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
