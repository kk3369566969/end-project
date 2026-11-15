package com.peerreview.ui.model;

/**
 * 下拉框选项：把「显示文字」与「业务值」分开。
 *
 * <p>筛选项需要「全部作业」这种没有实际 id 的选项，用 {@code value == null} 表达，
 * 避免在界面里到处判空或塞哨兵对象。</p>
 *
 * @param label 展示文字
 * @param value 业务值；{@code null} 表示不过滤
 */
public record Choice<T>(String label, T value) {

    public static <T> Choice<T> of(String label, T value) {
        return new Choice<>(label, value);
    }

    public static <T> Choice<T> all(String label) {
        return new Choice<>(label, null);
    }

    public boolean isAll() {
        return value == null;
    }

    @Override
    public String toString() {
        return label;
    }
}
