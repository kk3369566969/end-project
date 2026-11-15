package com.peerreview.ai;

import java.util.List;

/**
 * AI 生成的评审参考意见（草稿）。
 *
 * <p>这是「建议」而不是「结果」：调用方必须由学生本人确认后才写入数据库，
 * 系统从不因为 AI 给过分就自动提交互评。数据结构上也能体现这一点——
 * 它只是一份可展示、可部分采纳的草稿，没有任何持久化入口。</p>
 *
 * @param hints         逐维度建议分，可能为空（模型未遵循格式时降级）
 * @param comment       总评文字，可直接作为评语草稿
 * @param model         实际使用的模型标识，便于日志追溯
 * @param elapsedMillis 端到端耗时，用于在界面提示「耗时 xx 秒」
 * @param totalTokens   本次消耗的 token 数，便于估算成本
 */
public record ReviewSuggestion(
        List<DimensionHint> hints,
        String comment,
        String model,
        long elapsedMillis,
        int totalTokens) {

    public ReviewSuggestion {
        hints = hints == null ? List.of() : List.copyOf(hints);
        comment = comment == null ? "" : comment.trim();
    }

    /**
     * 单个维度的建议。
     *
     * @param dimension 维度名称，已与作业细则对齐
     * @param score     建议分，已裁剪到该维度的合法区间内
     * @param reason    判断依据
     */
    public record DimensionHint(String dimension, double score, String reason) {
    }

    /** 完全没有可用内容（例如模型返回空串）时视为失败，界面据此展示重试。 */
    public boolean isEmpty() {
        return hints.isEmpty() && comment.isEmpty();
    }

    /** 耗时的人性化表达，例如 {@code 2.3 秒}。 */
    public String elapsedLabel() {
        return String.format("%.1f 秒", elapsedMillis / 1000.0);
    }
}
