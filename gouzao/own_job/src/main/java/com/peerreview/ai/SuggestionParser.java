package com.peerreview.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peerreview.common.AppException;
import com.peerreview.common.Text;
import com.peerreview.domain.RubricItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 响应解析与「防越界」校验。
 *
 * <p>大模型的输出是不可信输入，不能直接喂给业务层。所有字段都做校正：</p>
 * <ul>
 *   <li>维度名对不上细则（模型改名、编造维度）→ 丢弃该条，而不是塞一条无名分数进去；</li>
 *   <li>建议分超出 {@code [min, max]} → 裁剪到区间内并对齐 0.5；</li>
 *   <li>整体不是合法 JSON → 退化为「把整段文字当总评」，仍然可用而不是直接报错。</li>
 * </ul>
 *
 * <p>这样即使模型跑偏，界面拿到的也永远是能填进输入框、能通过
 * {@code ReviewService.validateScores} 的数据，不会出现「点了采纳却提交失败」。</p>
 */
final class SuggestionParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** 单条理由的展示上限，避免模型输出长段落把界面撑开。 */
    private static final int MAX_REASON_CHARS = 60;

    private SuggestionParser() {
    }

    static ReviewSuggestion parse(String content, String model, long elapsedMillis,
                                  int totalTokens, List<RubricItem> rubricItems) {
        String body = stripCodeFence(Text.trimToEmpty(content));
        if (body.isEmpty()) {
            throw new AppException("AI 返回内容为空，请重试");
        }

        JsonNode root = readTreeOrNull(body);
        if (root == null || !root.isObject()) {
            return degraded(body, model, elapsedMillis, totalTokens);
        }

        List<ReviewSuggestion.DimensionHint> hints = new ArrayList<>();
        for (JsonNode node : root.path("dimensions")) {
            RubricItem item = matchItem(node.path("name").asText(""), rubricItems);
            if (item == null) {
                continue;
            }
            double rawScore = node.path("score").asDouble(Double.NaN);
            if (!Double.isFinite(rawScore)) {
                continue;
            }
            String reason = Text.ellipsis(node.path("reason").asText(""), MAX_REASON_CHARS);
            hints.add(new ReviewSuggestion.DimensionHint(item.getName(), clampToRange(rawScore, item), reason));
        }

        String comment = Text.trimToEmpty(root.path("comment").asText(""));
        if (comment.isEmpty() && hints.isEmpty()) {
            throw new AppException("AI 返回结果无法解析，请重试");
        }
        return new ReviewSuggestion(hints, comment, model, elapsedMillis, totalTokens);
    }

    /** 不是 JSON 时把整段话当总评：模型偶发不守格式不应等于功能不可用。 */
    private static ReviewSuggestion degraded(String body, String model, long elapsed, int tokens) {
        return new ReviewSuggestion(List.of(), Text.ellipsis(body, 400), model, elapsed, tokens);
    }

    private static JsonNode readTreeOrNull(String body) {
        try {
            return MAPPER.readTree(body);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    /** 去掉模型习惯性包裹的 ```json 围栏。 */
    private static String stripCodeFence(String text) {
        String value = text.trim();
        if (!value.startsWith("```")) {
            return value;
        }
        int firstLineBreak = value.indexOf('\n');
        if (firstLineBreak < 0) {
            return value;
        }
        String inner = value.substring(firstLineBreak + 1);
        int closing = inner.lastIndexOf("```");
        return closing >= 0 ? inner.substring(0, closing).trim() : inner.trim();
    }

    /**
     * 按名称匹配细则：先精确比对，再退化为「互相包含」，
     * 兼容模型输出「模块划分（40%）」这类加了后缀的写法。
     */
    private static RubricItem matchItem(String rawName, List<RubricItem> items) {
        String target = normalize(rawName);
        if (target.isEmpty()) {
            return null;
        }
        for (RubricItem item : items) {
            if (normalize(item.getName()).equals(target)) {
                return item;
            }
        }
        for (RubricItem item : items) {
            String name = normalize(item.getName());
            if (!name.isEmpty() && (target.contains(name) || name.contains(target))) {
                return item;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s（(].*$", "").trim().toLowerCase(Locale.ROOT);
    }

    /** 裁剪到区间并对齐 0.5，保证结果一定能通过业务层校验。 */
    private static double clampToRange(double raw, RubricItem item) {
        double min = item.getMinScore();
        double max = item.getMaxScore();
        double clamped = Math.max(min, Math.min(max, raw));
        double snapped = Math.round(clamped * 2) / 2.0;
        return Math.max(min, Math.min(max, snapped));
    }
}
