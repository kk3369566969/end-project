package com.peerreview.ai;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学生「采纳」AI 草稿后要回填到打分窗口的内容。
 *
 * <p>刻意做成一个不可变的小对象，而不是直接改界面控件：采纳是界面层的显式动作，
 * {@code ReviewDialog} 收到它之后才决定往哪些输入框里填什么，AI 模块对界面一无所知。</p>
 *
 * @param scores  维度 ID → 建议分；只采纳评语时为空
 * @param comment 评语草稿，可能已被学生在弹窗内改过
 */
public record AiAdoption(Map<Long, Double> scores, String comment) {

    public AiAdoption {
        scores = scores == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(scores));
        comment = comment == null ? "" : comment.trim();
    }

    public boolean hasComment() {
        return !comment.isEmpty();
    }
}
