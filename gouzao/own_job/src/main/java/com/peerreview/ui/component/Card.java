package com.peerreview.ui.component;

import com.peerreview.ui.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * 圆角卡片容器：白色底 + 细边框 + 极轻投影，是页面里所有内容区块的载体。
 */
public class Card extends JPanel {

    private static final int ARC = 16;
    private static final Color SHADOW = new Color(26, 40, 70, 14);

    public Card() {
        this(null);
    }

    public Card(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBackground(Theme.SURFACE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();

        g2.setColor(SHADOW);
        g2.fillRoundRect(1, 3, width - 3, height - 5, ARC, ARC);

        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, width - 2, height - 3, ARC, ARC);

        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, width - 3, height - 4, ARC, ARC);
        g2.dispose();

        super.paintComponent(g);
    }
}
