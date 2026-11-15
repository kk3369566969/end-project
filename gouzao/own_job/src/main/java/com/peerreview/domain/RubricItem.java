package com.peerreview.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 评分细则中的一个维度，例如「完整性，权重 40%，0~10 分」。
 *
 * <p>维度是作业的组成部分，随作业一起被加载与保存，因此这里保持为纯数据对象。</p>
 */
public class RubricItem {

    /** 单项得分的合法区间，供 UI 生成输入控件与业务层校验共用。 */
    public static final double MIN_ALLOWED = 0;
    public static final double MAX_ALLOWED = 100;

    private long id;
    private long assignmentId;
    private String name;
    private int weight;
    private double minScore;
    private double maxScore;
    private int sortNo;

    public RubricItem() {
    }

    public RubricItem(String name, int weight, double minScore, double maxScore) {
        this.name = name;
        this.weight = weight;
        this.minScore = minScore;
        this.maxScore = maxScore;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public int getSortNo() {
        return sortNo;
    }

    public void setSortNo(int sortNo) {
        this.sortNo = sortNo;
    }

    public double scoreSpan() {
        return maxScore - minScore;
    }

    /** 区间文字，如「0 ~ 10 分」。 */
    public String rangeLabel() {
        return trim(minScore) + " ~ " + trim(maxScore) + " 分";
    }

    public String weightLabel() {
        return weight + "%";
    }

    /** 只去掉无意义的小数位，避免界面出现 0.0000 之类的噪音。 */
    public static String trim(double value) {
        BigDecimal decimal = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return decimal.toPlainString();
    }

    @Override
    public String toString() {
        return name + "（" + weightLabel() + "）";
    }
}
