package com.peerreview.domain.scoring;

import com.peerreview.domain.RubricItem;
import com.peerreview.domain.ScoringMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 归一化加权策略（默认）：先把每个维度得分按「得分区间」映射到 0~1，再按权重求和，折算为百分制。
 *
 * <p>公式：{@code 总分 = Σ( (得分 - 最低分) / (最高分 - 最低分) × 权重 ) / Σ权重 × 100}</p>
 *
 * <p>适用于各维度分值区间不一致的情况（如「完整性 0~10 分、规范性 0~5 分」）。</p>
 */
public final class NormalizedWeightedCalculator implements ScoreCalculator {

    private static final double FULL_SCORE = 100.0;

    @Override
    public double calculate(List<RubricItem> items, Map<Long, Double> scoresByItemId) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        double weightedSum = 0;
        double weightTotal = 0;
        for (RubricItem item : items) {
            double raw = scoresByItemId.getOrDefault(item.getId(), item.getMinScore());
            weightTotal += item.getWeight();
            weightedSum += normalize(item, raw) * item.getWeight();
        }
        if (weightTotal <= 0) {
            return 0;
        }
        return round(weightedSum / weightTotal * FULL_SCORE);
    }

    /** 把原始分压缩到 0~1；区间非法（跨度 <= 0）时按满分维度处理，避免除零。 */
    private double normalize(RubricItem item, double raw) {
        double span = item.scoreSpan();
        if (span <= 0) {
            return 1.0;
        }
        double ratio = (raw - item.getMinScore()) / span;
        return Math.max(0, Math.min(1, ratio));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public ScoringMode mode() {
        return ScoringMode.NORMALIZED;
    }
}
