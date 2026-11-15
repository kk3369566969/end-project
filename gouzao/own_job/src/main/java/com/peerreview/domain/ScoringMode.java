package com.peerreview.domain;

/**
 * 计分方式（策略模式的策略标识）。
 *
 * <p>不同课程对「维度权重」的理解不同，因此把算法做成可配置项：</p>
 * <ul>
 *   <li>{@link #NORMALIZED}：各维度先按分值区间归一化到 0~1，再按权重求和折算百分制；</li>
 *   <li>{@link #RAW_WEIGHTED}：直接用「得分 × 权重」求和，适合所有维度同量纲的细则。</li>
 * </ul>
 */
public enum ScoringMode {

    NORMALIZED("归一化加权（百分制）"),
    RAW_WEIGHTED("直接加权求和");

    private final String label;

    ScoringMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
