package com.peerreview.domain;

/**
 * 一次作业的整体统计概览，用于教师端顶部指标卡。
 *
 * @param submissions 提交份数
 * @param reviewCount 已完成的互评条数
 * @param average     各提交平均分的均值（仅统计已有互评的提交）
 * @param max         各提交平均分中的最高值
 * @param min         各提交平均分中的最低值
 */
public record AssignmentSummary(int submissions, int reviewCount, double average, double max, double min) {

    public static AssignmentSummary empty() {
        return new AssignmentSummary(0, 0, 0, 0, 0);
    }

    public String averageLabel() {
        return reviewCount == 0 ? "暂无" : RubricItem.trim(average);
    }

    public String maxLabel() {
        return reviewCount == 0 ? "暂无" : RubricItem.trim(max);
    }

    public String minLabel() {
        return reviewCount == 0 ? "暂无" : RubricItem.trim(min);
    }

    public String completionLabel() {
        return reviewCount == 0 ? "等待学生完成互评" : reviewCount + " 条互评";
    }

    public String submissionLabel() {
        return submissions + " 份提交";
    }
}
