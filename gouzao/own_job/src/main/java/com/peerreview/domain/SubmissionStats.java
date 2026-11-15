package com.peerreview.domain;

import com.peerreview.common.Text;

/**
 * 单份提交的互评统计结果：评审份数 + 平均/最高/最低分。
 *
 * @param counted 参与统计的有效互评数量（为 0 时其余分数为 0，界面应展示「待互评」）
 */
public record SubmissionStats(
        long submissionId,
        String studentName,
        int counted,
        double average,
        double max,
        double min) {

    public static SubmissionStats empty(long submissionId, String studentName) {
        return new SubmissionStats(submissionId, studentName, 0, 0, 0, 0);
    }

    public String averageLabel() {
        return counted == 0 ? "待互评" : RubricItem.trim(average);
    }

    public String maxLabel() {
        return counted == 0 ? "—" : RubricItem.trim(max);
    }

    public String minLabel() {
        return counted == 0 ? "—" : RubricItem.trim(min);
    }

    public String shortName() {
        return Text.maskName(studentName);
    }
}
