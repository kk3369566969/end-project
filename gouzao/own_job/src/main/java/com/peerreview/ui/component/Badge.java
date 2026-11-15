package com.peerreview.ui.component;

import com.peerreview.ui.Theme;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * 状态胶囊：把「互评中 / 已完成」这类短状态渲染成带底色的圆角标签。
 *
 * <p>作为纯展示控件，也可以作为表格单元格的值使用（渲染器会识别它）。</p>
 */
public final class Badge extends JLabel {

    private static final int ARC = 999;

    private final Color background;
    private final Color foreground;

    public Badge(String text, Color foreground, Color background) {
        super(text);
        this.foreground = foreground;
        this.background = background;
        setFont(Theme.bold(12));
        setForeground(foreground);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 10, 3, 10));
        setOpaque(false);
    }

    public Color background() {
        return background;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(background);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
        g2.dispose();
        super.paintComponent(g);
    }

    /** 深色文字 + 浅色底，用于表格内的状态列。 */
    public static Badge soft(String text, Color foreground, Color background) {
        return new Badge(text, foreground, background);
    }

    @Override
    public String toString() {
        return getText();
    }
}
