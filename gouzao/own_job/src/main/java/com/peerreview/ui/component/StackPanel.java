package com.peerreview.ui.component;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Component;

/**
 * 自动留白的线性布局面板：每次 add 自动在元素之间插入固定间距。
 *
 * <p>BoxLayout 默认把子组件按 alignmentX=0.5 居中，会让标题、表单错位；
 * 这里统一改成左对齐（需要居中的场合显式再设置一次即可）。</p>
 */
public class StackPanel extends JPanel {

    private final int gap;
    private final int axis;

    public StackPanel(int axis, int gap) {
        this.axis = axis;
        this.gap = gap;
        setLayout(new BoxLayout(this, axis));
        setOpaque(false);
    }

    @Override
    public Component add(Component component) {
        if (component instanceof javax.swing.JComponent child && child.getAlignmentX() == CENTER_ALIGNMENT) {
            child.setAlignmentX(LEFT_ALIGNMENT);
        }
        if (getComponentCount() > 0 && gap > 0) {
            super.add(axis == BoxLayout.Y_AXIS ? Box.createVerticalStrut(gap) : Box.createHorizontalStrut(gap));
        }
        return super.add(component);
    }
}
