package com.peerreview.domain.scoring;

import com.peerreview.domain.RubricItem;
import com.peerreview.domain.ScoringMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 直接加权求和策略：{@code 总分 = Σ(得分 × 权重%)}。
 *
 * <p>适合所有维度使用同一量纲（如全部为 0~100 分）的细则；相比归一化策略，计算更直观，
 * 但要求教师在设定权重时自行保证量纲一致。</p>
 */
public final class RawWeightedCalculator implements ScoreCalculator {

    @Override
    public double calculate(List<RubricItem> items, Map<Long, Double> scoresByItemId) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (RubricItem item : items) {
            double raw = scoresByItemId.getOrDefault(item.getId(), item.getMinScore());
            total += raw * item.getWeight() / 100.0;
        }
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public ScoringMode mode() {
        return ScoringMode.RAW_WEIGHTED;
    }
}
