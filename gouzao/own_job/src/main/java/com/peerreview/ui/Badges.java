package com.peerreview.ui;

import com.peerreview.domain.AssignmentStatus;
import com.peerreview.domain.TaskStatus;
import com.peerreview.ui.component.Badge;

/** 状态徽标工厂：统一「某种状态用什么颜色」的判断，避免各页面各写一套。 */
public final class Badges {

    private Badges() {
    }

    public static Badge assignment(AssignmentStatus status) {
        return switch (status) {
            case DRAFT -> new Badge("草稿", Theme.MUTED, Theme.NEUTRAL_SOFT);
            case PUBLISHED -> new Badge("开放提交", Theme.SUCCESS, Theme.SUCCESS_SOFT);
            case ALLOCATED -> new Badge("互评中", Theme.PRIMARY, Theme.PRIMARY_SOFT);
            case CLOSED -> new Badge("已结束", Theme.MUTED, Theme.NEUTRAL_SOFT);
        };
    }

    public static Badge task(TaskStatus status) {
        return status == TaskStatus.COMPLETED
                ? new Badge("已完成", Theme.SUCCESS, Theme.SUCCESS_SOFT)
                : new Badge("待互评", Theme.WARNING, Theme.WARNING_SOFT);
    }

    public static Badge soft(String text, java.awt.Color color) {
        return new Badge(text, color, Theme.NEUTRAL_SOFT);
    }
}
