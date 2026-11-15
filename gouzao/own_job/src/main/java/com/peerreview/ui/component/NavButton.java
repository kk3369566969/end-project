package com.peerreview.ui.component;

import com.peerreview.ui.Theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * 侧栏导航项：选中时显示浅色底 + 左侧高亮竖条，未选中时保持透明。
 */
public final class NavButton extends JButton {

    private boolean selected;
    private final Color activeForeground = Theme.PRIMARY;
    private final Color idleForeground = new Color(0x5A6678);

    public NavButton(String text, javax.swing.Icon icon) {
        super(text, icon);
        setHorizontalAlignment(LEFT);
        setIconTextGap(10);
        setFont(Theme.bold(13));
        setForeground(idleForeground);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 16, 10, 12));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        setForeground(selected ? activeForeground : idleForeground);
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth() - 1;
        int height = getHeight() - 1;
        if (selected) {
            g2.setColor(Theme.PRIMARY_SOFT);
            g2.fillRoundRect(0, 0, width, height, 12, 12);
            g2.setColor(Theme.PRIMARY);
            g2.fillRoundRect(0, height / 2 - 9, 3, 18, 3, 3);
        } else if (getModel().isRollover()) {
            g2.setColor(Theme.NEUTRAL_SOFT);
            g2.fillRoundRect(0, 0, width, height, 12, 12);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
