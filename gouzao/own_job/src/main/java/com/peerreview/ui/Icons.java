package com.peerreview.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 轻量线性图标库：用矢量绘制代替位图资源，任意缩放不糊，且不引入额外依赖。
 *
 * <p>所有图标在 20×20 的坐标系里描述，绘制时按目标尺寸等比缩放。</p>
 */
public final class Icons {

    private static final float BASE = 20f;

    private Icons() {
    }

    public static Icon book(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            g.draw(new RoundRectangle2D.Float(3f, 3.2f, 14f, 13.6f, 3f, 3f));
            g.draw(new java.awt.geom.Line2D.Float(7.4f, 3.4f, 7.4f, 16.6f));
            g.draw(new java.awt.geom.Line2D.Float(10.4f, 7f, 14f, 7f));
            g.draw(new java.awt.geom.Line2D.Float(10.4f, 10.4f, 14f, 10.4f));
        });
    }

    public static Icon upload(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            g.draw(new java.awt.geom.Line2D.Float(10f, 13.5f, 10f, 4f));
            g.draw(new java.awt.geom.Line2D.Float(6.2f, 7.6f, 10f, 3.8f));
            g.draw(new java.awt.geom.Line2D.Float(13.8f, 7.6f, 10f, 3.8f));
            g.draw(new java.awt.geom.Line2D.Float(4.2f, 16.4f, 15.8f, 16.4f));
        });
    }

    public static Icon list(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            for (int i = 0; i < 3; i++) {
                float y = 5.5f + i * 4.6f;
                g.fill(new Ellipse2D.Float(4.2f, y - 1.3f, 2.6f, 2.6f));
                g.draw(new java.awt.geom.Line2D.Float(9f, y, 16f, y));
            }
        });
    }

    public static Icon star(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            Path2D.Float star = new Path2D.Float();
            double cx = 10, cy = 10.4, outer = 7.2, inner = 3.1;
            for (int i = 0; i < 10; i++) {
                double radius = i % 2 == 0 ? outer : inner;
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                float x = (float) (cx + radius * Math.cos(angle));
                float y = (float) (cy + radius * Math.sin(angle));
                if (i == 0) {
                    star.moveTo(x, y);
                } else {
                    star.lineTo(x, y);
                }
            }
            star.closePath();
            g.draw(star);
        });
    }

    public static Icon chart(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            g.draw(new java.awt.geom.Line2D.Float(3.6f, 16.4f, 16.4f, 16.4f));
            g.fill(new RoundRectangle2D.Float(4.6f, 10.4f, 3f, 4.6f, 1.4f, 1.4f));
            g.fill(new RoundRectangle2D.Float(8.6f, 7f, 3f, 8f, 1.4f, 1.4f));
            g.fill(new RoundRectangle2D.Float(12.6f, 4.2f, 3f, 10.8f, 1.4f, 1.4f));
        });
    }

    public static Icon logout(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            Path2D.Float door = new Path2D.Float();
            door.moveTo(12.4f, 4f);
            door.lineTo(5f, 4f);
            door.lineTo(5f, 16f);
            door.lineTo(12.4f, 16f);
            g.draw(door);
            g.draw(new java.awt.geom.Line2D.Float(9.4f, 10f, 16.4f, 10f));
            g.draw(new java.awt.geom.Line2D.Float(13.4f, 7.2f, 16.4f, 10f));
            g.draw(new java.awt.geom.Line2D.Float(13.4f, 12.8f, 16.4f, 10f));
        });
    }

    public static Icon plus(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            g.draw(new java.awt.geom.Line2D.Float(10f, 4.6f, 10f, 15.4f));
            g.draw(new java.awt.geom.Line2D.Float(4.6f, 10f, 15.4f, 10f));
        });
    }

    public static Icon search(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            g.draw(new Ellipse2D.Float(4f, 4f, 9f, 9f));
            g.draw(new java.awt.geom.Line2D.Float(11.6f, 11.6f, 16f, 16f));
        });
    }

    public static Icon refresh(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            g.draw(new java.awt.geom.Arc2D.Float(4.4f, 4.4f, 11.2f, 11.2f, 60, 260, java.awt.geom.Arc2D.OPEN));
            g.draw(new java.awt.geom.Line2D.Float(13.6f, 3.4f, 15.4f, 6.6f));
            g.draw(new java.awt.geom.Line2D.Float(15.4f, 6.6f, 12f, 7.4f));
        });
    }

    public static Icon check(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            g.draw(new java.awt.geom.Line2D.Float(4.6f, 10.4f, 8.4f, 14.2f));
            g.draw(new java.awt.geom.Line2D.Float(8.4f, 14.2f, 15.6f, 6.4f));
        });
    }

    public static Icon user(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            g.draw(new Ellipse2D.Float(7.2f, 4f, 5.6f, 5.6f));
            g.draw(new java.awt.geom.Arc2D.Float(4.2f, 10.6f, 11.6f, 10f, 0, 180, java.awt.geom.Arc2D.OPEN));
        });
    }

    public static Icon lock(int size, Color color) {
        return new LineIcon(size, color, (g, s) -> {
            g.draw(new RoundRectangle2D.Float(5f, 9f, 10f, 7.6f, 2.4f, 2.4f));
            g.draw(new java.awt.geom.Arc2D.Float(7f, 4.2f, 6f, 7f, 0, 180, java.awt.geom.Arc2D.OPEN));
        });
    }

    /** 图标绘制器：在缩放后的 20×20 坐标系内作画。 */
    private interface Painter {
        void paint(Graphics2D g, float scale);
    }

    private record LineIcon(int size, Color color, Painter painter) implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            float scale = size / BASE;
            g2.translate(x, y);
            g2.scale(scale, scale);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.7f / scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            painter.paint(g2, scale);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
