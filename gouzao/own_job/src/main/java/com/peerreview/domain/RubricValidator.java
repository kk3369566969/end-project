package com.peerreview.domain;

import com.peerreview.common.Text;
import com.peerreview.common.ValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 评分细则校验器：把「权重合计 100%、区间合法、名称唯一」这些规则从服务层独立出来。
 *
 * <p>独立成领域对象后，界面（保存前预校验）、服务层（写入前校验）与单元测试都复用它，
 * 规则只需要维护一处。</p>
 */
public final class RubricValidator {

    /** 单次作业允许的最大维度数：过多维度会让评审人难以认真打分。 */
    public static final int MAX_ITEMS = 12;

    private RubricValidator() {
    }

    /**
     * 校验细则是完整、可用的。
     *
     * @throws ValidationException 任一规则不满足时抛出，message 可直接展示给用户
     */
    public static void validate(List<RubricItem> items) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("请至少添加一条评分细则");
        }
        if (items.size() > MAX_ITEMS) {
            throw new ValidationException("评分细则最多 " + MAX_ITEMS + " 条，过多维度会增加评审负担");
        }
        Set<String> names = new HashSet<>();
        int totalWeight = 0;
        for (RubricItem item : items) {
            item.setName(Text.requireText(item.getName(), "评分维度名称", 32));
            if (!names.add(item.getName())) {
                throw new ValidationException("评分维度「" + item.getName() + "」重复");
            }
            if (item.getWeight() <= 0 || item.getWeight() > 100) {
                throw new ValidationException("维度「" + item.getName() + "」的权重应在 1 ~ 100 之间");
            }
            if (item.getMinScore() < RubricItem.MIN_ALLOWED || item.getMaxScore() > RubricItem.MAX_ALLOWED) {
                throw new ValidationException("维度「" + item.getName() + "」的分值应在 0 ~ 100 之间");
            }
            if (item.getMaxScore() <= item.getMinScore()) {
                throw new ValidationException("维度「" + item.getName() + "」的最高分必须大于最低分");
            }
            totalWeight += item.getWeight();
        }
        if (totalWeight != 100) {
            throw new ValidationException("评分细则权重合计必须为 100%，当前为 " + totalWeight + "%");
        }
    }

    /** 权重合计，供界面实时提示。 */
    public static int totalWeight(List<RubricItem> items) {
        return items == null ? 0 : items.stream().mapToInt(RubricItem::getWeight).sum();
    }
}
