package com.peerreview.domain.scoring;

import com.peerreview.domain.RubricItem;
import com.peerreview.domain.ScoringMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 计分策略白盒测试：重点覆盖加权计算与区间边界。
 */
@DisplayName("加权总分计算策略")
class ScoreCalculatorTest {

    private RubricItem item(long id, String name, int weight, double min, double max) {
        RubricItem item = new RubricItem(name, weight, min, max);
        item.setId(id);
        return item;
    }

    @Test
    @DisplayName("归一化策略：满分得 100，最低分得 0，中间分按比例折算")
    void normalizedStrategy() {
        List<RubricItem> items = List.of(
                item(1, "完整性", 60, 0, 10),
                item(2, "规范性", 40, 0, 5));
        ScoreCalculator calculator = ScoreCalculators.of(ScoringMode.NORMALIZED);

        assertEquals(100.0, calculator.calculate(items, Map.of(1L, 10.0, 2L, 5.0)), 1e-6);
        assertEquals(0.0, calculator.calculate(items, Map.of(1L, 0.0, 2L, 0.0)), 1e-6);
        // 两个维度都取一半 => 50
        assertEquals(50.0, calculator.calculate(items, Map.of(1L, 5.0, 2L, 2.5)), 1e-6);
        // 完整性满分、规范性 0 => 权重 60
        assertEquals(60.0, calculator.calculate(items, Map.of(1L, 10.0, 2L, 0.0)), 1e-6);
    }

    @Test
    @DisplayName("归一化策略：不同分值区间的维度可混合使用")
    void normalizedHandlesDifferentRanges() {
        List<RubricItem> items = List.of(
                item(1, "完整性", 50, 0, 10),
                item(2, "规范性", 50, 2, 6));
        ScoreCalculator calculator = ScoreCalculators.of(ScoringMode.NORMALIZED);
        // 完整性 5/10 = 0.5；规范性 4 在 [2,6] 上也是 0.5
        assertEquals(50.0, calculator.calculate(items, Map.of(1L, 5.0, 2L, 4.0)), 1e-6);
    }

    @Test
    @DisplayName("直接加权策略：按 得分 × 权重% 求和")
    void rawWeightedStrategy() {
        List<RubricItem> items = List.of(
                item(1, "完整性", 70, 0, 100),
                item(2, "规范性", 30, 0, 100));
        ScoreCalculator calculator = ScoreCalculators.of(ScoringMode.RAW_WEIGHTED);
        assertEquals(80.0, calculator.calculate(items, Map.of(1L, 80.0, 2L, 80.0)), 1e-6);
        assertEquals(56.0, calculator.calculate(items, Map.of(1L, 80.0, 2L, 0.0)), 1e-6);
    }

    @Test
    @DisplayName("异常输入：空细则返回 0，越界分数被裁剪到区间内")
    void edgeCases() {
        NormalizedWeightedCalculator calculator = new NormalizedWeightedCalculator();
        assertEquals(0.0, calculator.calculate(List.of(), Map.of()), 1e-6);

        List<RubricItem> items = List.of(item(1, "完整性", 100, 0, 10));
        assertEquals(100.0, calculator.calculate(items, Map.of(1L, 99.0)), 1e-6);
        assertEquals(0.0, calculator.calculate(items, Map.of(1L, -5.0)), 1e-6);
    }

    @Test
    @DisplayName("区间跨度为零时按满分处理，不会出现除零异常")
    void zeroSpanIsSafe() {
        List<RubricItem> items = List.of(item(1, "固定分", 100, 5, 5));
        assertEquals(100.0, new NormalizedWeightedCalculator().calculate(items, Map.of(1L, 5.0)), 1e-6);
    }
}
