package com.peerreview.domain.scoring;

import com.peerreview.domain.ScoringMode;

import java.util.EnumMap;
import java.util.Map;

/**
 * 计分策略注册表：按 {@link ScoringMode} 返回对应策略实例。
 *
 * <p>策略实例无状态，可安全复用；新增策略只需在此登记一次。</p>
 */
public final class ScoreCalculators {

    private static final Map<ScoringMode, ScoreCalculator> REGISTRY = new EnumMap<>(ScoringMode.class);

    static {
        register(new NormalizedWeightedCalculator());
        register(new RawWeightedCalculator());
    }

    private ScoreCalculators() {
    }

    private static void register(ScoreCalculator calculator) {
        REGISTRY.put(calculator.mode(), calculator);
    }

    public static ScoreCalculator of(ScoringMode mode) {
        ScoreCalculator calculator = REGISTRY.get(mode == null ? ScoringMode.NORMALIZED : mode);
        if (calculator == null) {
            throw new IllegalArgumentException("未注册的计分方式：" + mode);
        }
        return calculator;
    }
}
