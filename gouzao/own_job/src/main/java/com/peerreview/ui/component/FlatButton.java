package com.peerreview.ui.component;

import com.peerreview.ui.Theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * 扁平按钮：自带圆角背景与 hover/pressed 状态，不依赖具体 LAF 的按钮绘制逻辑。
 *
 * <p>按钮文字仍交给 Swing 渲染（保留字体度量与图标排版），只接管背景。</p>
 */
public final class FlatButton extends JButton {

    /** 四种语义样式，覆盖界面里的全部按钮场景。 */
    public enum Kind {
        PRIMARY, SECONDARY, DANGER, GHOST
    }

    private static final int ARC = 10;

    private final Kind kind;
    private final Color background;
    private final Color hoverBackground;
    private final Color pressedBackground;
    private final Color borderColor;

    public FlatButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        switch (kind) {
            case PRIMARY -> {
                background = Theme.PRIMARY;
                hoverBackground = Theme.PRIMARY_HOVER;
                pressedBackground = Theme.PRIMARY_PRESSED;
                borderColor = null;
            }
            case DANGER -> {
                background = Theme.DANGER_SOFT;
                hoverBackground = new Color(0xF9DDE0);
                pressedBackground = new Color(0xF4CBD0);
                borderColor = null;
            }
            case GHOST -> {
                background = new Color(0, 0, 0, 0);
                hoverBackground = Theme.NEUTRAL_SOFT;
                pressedBackground = new Color(0xE6EAF2);
                borderColor = null;
            }
            default -> {
                background = Theme.SURFACE;
                hoverBackground = new Color(0xF6F8FC);
                pressedBackground = new Color(0xEDF1F8);
                borderColor = Theme.BORDER;
            }
        }
        configure();
    }

    private void configure() {
        setFont(Theme.bold(13));
        setForeground(switch (kind) {
            case PRIMARY -> Color.WHITE;
            case DANGER -> Theme.DANGER;
            case GHOST -> Theme.MUTED;
            default -> Theme.TEXT;
        });
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(7, 16, 7, 16));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(7, 16, 7, 16));
    }

    /** 变小一号，用于表格上方的辅助操作。 */
    public FlatButton compact() {
        setFont(Theme.bold(12));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 12, 5, 12));
        return this;
    }

    public FlatButton withIcon(javax.swing.Icon icon) {
        setIcon(icon);
        setIconTextGap(6);
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth() - 1;
        int height = getHeight() - 1;
        Color fill = pickBackground();
        if (fill.getAlpha() > 0) {
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, width, height, ARC, ARC);
        }
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, width, height, ARC, ARC);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    private Color pickBackground() {
        if (!isEnabled()) {
            return blend(background, Theme.NEUTRAL_SOFT);
        }
        if (getModel().isPressed()) {
            return pressedBackground;
        }
        if (getModel().isRollover()) {
            return hoverBackground;
        }
        return background;
    }

    private Color blend(Color base, Color fallback) {
        if (base.getAlpha() == 0) {
            return fallback;
        }
        return new Color(
                (base.getRed() + fallback.getRed()) / 2,
                (base.getGreen() + fallback.getGreen()) / 2,
                (base.getBlue() + fallback.getBlue()) / 2);
    }
}
