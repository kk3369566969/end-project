package com.peerreview.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.peerreview.ui.component.Badge;
import com.peerreview.ui.component.Card;
import com.peerreview.ui.component.FlatButton;
import com.peerreview.ui.component.StackPanel;
import com.peerreview.ui.model.SimpleTableModel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.util.List;

/**
 * 界面构件工厂：把「间距、字号、圆角、表格样式」等重复代码收敛到一处。
 *
 * <p>页面代码只表达结构与业务含义，不再到处写 {@code setBorder(new EmptyBorder(...))}。</p>
 */
public final class Ui {

    /** 表格单元格左内边距，保证文字与表头对齐。 */
    private static final int CELL_PADDING_LEFT = 14;
    private static final Color ROW_STRIPE = new Color(0xFAFBFD);

    private Ui() {
    }

    // ———————————————————— 容器 ————————————————————

    public static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /** 纵向排列，元素间距固定。 */
    public static StackPanel column(int gap) {
        return new StackPanel(BoxLayout.Y_AXIS, gap);
    }

    /** 横向排列，元素间距固定。 */
    public static StackPanel row(int gap) {
        return new StackPanel(BoxLayout.X_AXIS, gap);
    }

    /** 在横向行里插入可伸缩空档，把后续元素推到右侧。 */
    public static Component stretch() {
        return Box.createHorizontalGlue();
    }

    /** 标准内容卡片：白底圆角 + 20px 内边距 + 纵向排列。 */
    public static Card card() {
        Card card = new Card(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        return card;
    }

    public static Card card(LayoutManager layout) {
        Card card = new Card(layout);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        return card;
    }

    public static void pad(JComponent component, int top, int left, int bottom, int right) {
        component.setBorder(new EmptyBorder(top, left, bottom, right));
    }

    public static Component gap(int width, int height) {
        return Box.createRigidArea(new Dimension(width, height));
    }

    public static JComponent divider() {
        JPanel line = new JPanel();
        line.setBackground(Theme.BORDER);
        line.setPreferredSize(new Dimension(0, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return line;
    }

    // ———————————————————— 文字 ————————————————————

    public static JLabel h1(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.bold(19));
        label.setForeground(Theme.TEXT);
        return label;
    }

    public static JLabel h2(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.bold(15));
        label.setForeground(Theme.TEXT);
        return label;
    }

    public static JLabel body(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.plain(13));
        label.setForeground(Theme.TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.plain(12));
        label.setForeground(Theme.MUTED);
        return label;
    }

    public static JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.plain(12));
        label.setForeground(Theme.FAINT);
        return label;
    }

    /** 表单字段标签：小号加粗灰字。 */
    public static JLabel boldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.bold(12));
        label.setForeground(Theme.MUTED);
        return label;
    }

    /** 标题 + 副标题的上下结构。 */
    public static JPanel sectionHeader(String title, String subtitle) {
        StackPanel panel = column(4);
        panel.add(h2(title));
        if (subtitle != null && !subtitle.isBlank()) {
            panel.add(muted(subtitle));
        }
        return panel;
    }

    // ———————————————————— 按钮 ————————————————————

    public static FlatButton primary(String text) {
        return new FlatButton(text, FlatButton.Kind.PRIMARY);
    }

    public static FlatButton secondary(String text) {
        return new FlatButton(text, FlatButton.Kind.SECONDARY);
    }

    public static FlatButton danger(String text) {
        return new FlatButton(text, FlatButton.Kind.DANGER);
    }

    public static FlatButton ghost(String text) {
        return new FlatButton(text, FlatButton.Kind.GHOST);
    }

    // ———————————————————— 输入 ————————————————————

    public static JTextField field(String placeholder, int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(Theme.plain(13));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        return field;
    }

    public static JPasswordField passwordField(String placeholder, int columns) {
        JPasswordField field = new JPasswordField(columns);
        field.setFont(Theme.plain(13));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");
        return field;
    }

    public static JTextArea textArea(int rows) {
        JTextArea area = new JTextArea(rows, 20);
        area.setFont(Theme.plain(13));
        area.setForeground(Theme.TEXT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
        return area;
    }

    public static JTextArea readOnlyArea(int rows) {
        JTextArea area = textArea(rows);
        area.setEditable(false);
        area.setBackground(new Color(0xFBFCFE));
        return area;
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(Theme.SURFACE);
        pane.getVerticalScrollBar().setUnitIncrement(18);
        pane.getHorizontalScrollBar().setUnitIncrement(18);
        pane.setOpaque(false);
        return pane;
    }

    public static <T> JComboBox<T> combo(List<T> items) {
        DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
        items.forEach(model::addElement);
        JComboBox<T> combo = new JComboBox<>(model);
        combo.setFont(Theme.plain(13));
        combo.setBackground(Theme.SURFACE);
        return combo;
    }

    public static JSpinner spinner(double min, double max, double step, double value) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        spinner.setFont(Theme.plain(13));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.##");
        editor.getTextField().setFont(Theme.plain(13));
        spinner.setEditor(editor);
        return spinner;
    }

    public static JCheckBox checkBox(String text) {
        JCheckBox box = new JCheckBox(text);
        box.setFont(Theme.plain(13));
        box.setForeground(Theme.TEXT);
        box.setOpaque(false);
        box.setFocusPainted(false);
        return box;
    }

    // ———————————————————— 表格 ————————————————————

    public static <T> JTable table(SimpleTableModel<T> model) {
        JTable table = new JTable(model);
        decorate(table);
        for (int i = 0; i < model.getColumnCount() && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(model.columns().get(i).width());
        }
        return table;
    }

    /** 用于可编辑表格（如评分细则）的构造：外观与只读表格保持一致。 */
    public static JTable editableTable(javax.swing.table.TableModel model) {
        JTable table = new JTable(model);
        decorate(table);
        return table;
    }

    /** 统一表格外观：行高、斑马纹、表头与选中态。 */
    public static void decorate(JTable table) {
        table.setRowHeight(42);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setBackground(Theme.SURFACE);
        table.setSelectionBackground(Theme.PRIMARY_SOFT);
        table.setSelectionForeground(Theme.TEXT);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setDefaultRenderer(Object.class, new CellRenderer());

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new HeaderRenderer());
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createEmptyBorder());
    }

    /** 表格外包一层无边框滚动面板。 */
    public static JScrollPane tableScroll(JTable table) {
        JScrollPane pane = scroll(table);
        pane.getViewport().setBackground(Theme.SURFACE);
        return pane;
    }

    /** 正文样式单元格渲染器：支持斑马纹、选中态与胶囊值。 */
    private static final class CellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Color background = isSelected ? Theme.PRIMARY_SOFT : (row % 2 == 1 ? ROW_STRIPE : Theme.SURFACE);
            if (value instanceof Badge badge) {
                return badgeCell(badge, background);
            }
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setFont(Theme.plain(13));
            setForeground(Theme.TEXT);
            setBackground(background);
            setBorder(new EmptyBorder(0, CELL_PADDING_LEFT, 0, 10));
            setVerticalAlignment(SwingConstants.CENTER);
            setText(value == null ? "" : value.toString());
            setToolTipText(value == null ? null : value.toString());
            return this;
        }

        private Component badgeCell(Badge badge, Color background) {
            JPanel holder = new JPanel(new BorderLayout());
            holder.setBackground(background);
            holder.setBorder(new EmptyBorder(6, CELL_PADDING_LEFT - 2, 6, 8));
            JPanel line = new JPanel();
            line.setOpaque(false);
            line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
            line.add(new Badge(badge.getText(), badge.getForeground(), badge.background()));
            line.add(Box.createHorizontalGlue());
            holder.add(line, BorderLayout.CENTER);
            return holder;
        }
    }

    /** 表头渲染器：小号灰字 + 底部细线，视觉上更轻。 */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(Theme.bold(12));
            setForeground(Theme.MUTED);
            setBackground(Theme.SURFACE);
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    new EmptyBorder(0, CELL_PADDING_LEFT, 0, 10)));
            setHorizontalAlignment(LEFT);
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    /** 统一空状态提示，避免页面出现大片空白。 */
    public static JPanel emptyState(String title, String hint) {
        JPanel wrapper = transparent(new GridBagLayout());
        StackPanel box = column(8);
        JLabel icon = new JLabel(Icons.list(36, new Color(0xC9D2E0)));
        JLabel titleLabel = h2(title);
        JLabel hintLabel = muted(hint);
        box.add(icon);
        box.add(titleLabel);
        box.add(hintLabel);
        // 空状态整体居中（StackPanel 默认左对齐，这里显式覆盖）
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.add(box);
        return wrapper;
    }

    /** 说明性提示条（浅色卡片 + 语义色文字）。 */
    public static JPanel notice(String text, Color accent, Color background) {
        Card card = new Card(new BorderLayout());
        card.setBackground(background);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel label = new JLabel(text);
        label.setFont(Theme.plain(12));
        label.setForeground(accent);
        card.add(label, BorderLayout.CENTER);
        return card;
    }
}
