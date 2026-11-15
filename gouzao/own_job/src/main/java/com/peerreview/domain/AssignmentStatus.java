package com.peerreview.domain;

/**
 * 作业生命周期状态，构成明确的状态流转：
 *
 * <pre>
 *  DRAFT（草稿） --发布--> PUBLISHED（开放提交）
 *  PUBLISHED --分配互评--> ALLOCATED（互评中）
 *  ALLOCATED --关闭--> CLOSED（已结束）
 * </pre>
 */
public enum AssignmentStatus {

    DRAFT("草稿"),
    PUBLISHED("开放提交"),
    ALLOCATED("互评中"),
    CLOSED("已结束");

    private final String label;

    AssignmentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 是否允许学生提交/修改作业内容。 */
    public boolean acceptsSubmission() {
        return this == PUBLISHED;
    }

    /** 是否允许教师发起互评分配。 */
    public boolean acceptsAllocation() {
        return this == PUBLISHED || this == ALLOCATED;
    }
}
