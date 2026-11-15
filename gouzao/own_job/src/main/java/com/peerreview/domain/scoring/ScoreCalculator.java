package com.peerreview.domain.scoring;

import com.peerreview.domain.RubricItem;
import com.peerreview.domain.ScoringMode;

import java.util.List;
import java.util.Map;

/**
 * 总分计算策略（策略模式）。
 *
 * <p>把「怎么由各维度得分算出总分」抽象成独立策略，作业可以选择不同算法，
 * 而 {@code ReviewService} 只依赖该接口，新增算法无需修改业务代码 —— 满足开闭原则。</p>
 */
public interface ScoreCalculator {

    /**
     * 计算加权总分。
     *
     * @param items           作业的评分细则（含权重与分值区间）
     * @param scoresByItemId  维度 id → 评审人给出的原始分
     * @return 总分，已按人民币式四舍五入保留两位小数
     */
    double calculate(List<RubricItem> items, Map<Long, Double> scoresByItemId);

    /** 该策略对应的计分方式标识。 */
    ScoringMode mode();
}
