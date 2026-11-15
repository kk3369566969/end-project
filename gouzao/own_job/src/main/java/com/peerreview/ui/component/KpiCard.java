package com.peerreview.ui.component;

import com.peerreview.ui.Theme;

import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * 指标卡：一个数值 + 一句说明，用于统计页顶部的关键指标区。
 */
public final class KpiCard extends Card {

    private final Color accent;
    private final JLabel valueLabel = new JLabel("—");
    private final JLabel hintLabel = new JLabel(" ");

    public KpiCard(String caption, Color accent) {
        super(new BorderLayout(0, 8));
        this.accent = accent;
        setBorder(new javax.swing.border.EmptyBorder(16, 18, 16, 18));

        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(Theme.plain(12));
        captionLabel.setForeground(Theme.MUTED);

        valueLabel.setFont(new Font(Theme.bold(26).getName(), Font.BOLD, 26));
        valueLabel.setForeground(accent);

        hintLabel.setFont(Theme.plain(12));
        hintLabel.setForeground(Theme.FAINT);

        add(captionLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
        add(hintLabel, BorderLayout.SOUTH);
    }

    public void set(String value, String hint) {
        String text = value == null || value.isBlank() ? "暂无" : value;
        boolean numeric = text.chars().anyMatch(Character::isDigit);
        valueLabel.setText(text);
        // 数值用大号强调色，占位文案（暂无）用小号灰字，避免视觉重心错位
        valueLabel.setFont(new Font(Theme.bold(numeric ? 26 : 16).getName(), Font.BOLD, numeric ? 26 : 16));
        valueLabel.setForeground(numeric ? accent : Theme.FAINT);
        hintLabel.setText(hint == null ? " " : hint);
    }

    public static KpiCard primary(String caption) {
        return new KpiCard(caption, Theme.PRIMARY);
    }

    public static KpiCard success(String caption) {
        return new KpiCard(caption, Theme.SUCCESS);
    }

    public static KpiCard neutral(String caption) {
        return new KpiCard(caption, Theme.TEXT);
    }

    /** 便捷方法：指标卡 + 固定高度的外层容器，保证卡片等高。 */
    public KpiCard fixedHeight(int height) {
        setPreferredSize(new java.awt.Dimension(200, height));
        return this;
    }

    /** 兼容仅需要数值、不需要说明的场景。 */
    public void setValue(String value) {
        set(value, null);
    }
}
