package com.peerreview.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 作业：课程下的一次互评任务单元，包含评分细则、匿名开关、每人评审份数与计分方式。
 */
public class Assignment {

    private long id;
    private long courseId;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private boolean anonymous;
    private int reviewersPerSubmission;
    private ScoringMode scoringMode = ScoringMode.NORMALIZED;
    private AssignmentStatus status = AssignmentStatus.DRAFT;
    private LocalDateTime createdAt;
    private List<RubricItem> rubricItems = new ArrayList<>();

    /** 非持久化字段：列表展示用的课程信息。 */
    private String courseName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCourseId() {
        return courseId;
    }

    public void setCourseId(long courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public int getReviewersPerSubmission() {
        return reviewersPerSubmission;
    }

    public void setReviewersPerSubmission(int reviewersPerSubmission) {
        this.reviewersPerSubmission = reviewersPerSubmission;
    }

    public ScoringMode getScoringMode() {
        return scoringMode;
    }

    public void setScoringMode(ScoringMode scoringMode) {
        this.scoringMode = scoringMode;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<RubricItem> getRubricItems() {
        return rubricItems;
    }

    public void setRubricItems(List<RubricItem> rubricItems) {
        this.rubricItems = rubricItems == null ? new ArrayList<>() : rubricItems;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    /** 权重合计，用于细则合法性校验与界面提示。 */
    public int totalWeight() {
        return rubricItems.stream().mapToInt(RubricItem::getWeight).sum();
    }

    /** 是否已过截止时间（无截止时间视为未过期）。 */
    public boolean isOverdue() {
        return deadline != null && LocalDateTime.now().isAfter(deadline);
    }

    @Override
    public String toString() {
        return title;
    }
}
