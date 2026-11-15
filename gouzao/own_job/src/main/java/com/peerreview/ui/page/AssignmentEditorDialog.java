package com.peerreview.ui.page;

import com.peerreview.AppContext;
import com.peerreview.common.Dates;
import com.peerreview.common.Text;
import com.peerreview.common.ValidationException;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.AssignmentStatus;
import com.peerreview.domain.RubricItem;
import com.peerreview.domain.ScoringMode;
import com.peerreview.domain.User;
import com.peerreview.ui.Dialogs;
import com.peerreview.ui.Theme;
import com.peerreview.ui.Ui;
import com.peerreview.ui.component.FlatButton;
import com.peerreview.ui.model.Choice;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 新建 / 编辑作业对话框，包含完整的评分细则编辑器。
 *
 * <p>细则表支持动态增删维度，并实时显示权重合计；权重不等于 100% 时保存会被服务层拒绝
 * （界面这里先给出红字提示，避免用户点保存后才发现）。</p>
 */
public final class AssignmentEditorDialog extends JDialog {

    private static final String[] RUBRIC_HEADERS = {"维度名称", "权重(%)", "最低分", "最高分"};

    private final AppContext context;
    private final User teacher;
    private final long courseId;
    private final Assignment editing;

    private final JTextField titleField = Ui.field("如 实验一：模块化设计与实现", 24);
    private final JTextField deadlineField = Ui.field("yyyy-MM-dd HH:mm，留空表示不限制", 24);
    private final JSpinner reviewersSpinner = Ui.spinner(1, 10, 1, 2);
    private final JComboBox<Choice<ScoringMode>> modeCombo = Ui.combo(
            Arrays.stream(ScoringMode.values()).map(mode -> Choice.of(mode.label(), mode)).toList());
    private final JCheckBox anonymousBox = Ui.checkBox("开启匿名互评（互评界面隐藏作者姓名）");
    private final JTextArea descriptionArea = Ui.textArea(6);
    private final DefaultTableModel rubricModel;
    private final JLabel weightLabel = new JLabel();
    private final JTable rubricTable;

    private Assignment result;

    private AssignmentEditorDialog(Window owner, AppContext context, User teacher, long courseId, Assignment assignment) {
        super(owner, assignment == null ? "新建作业" : "编辑作业", ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.teacher = teacher;
        this.courseId = courseId;
        this.editing = assignment;

        rubricModel = new DefaultTableModel(RUBRIC_HEADERS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        rubricTable = Ui.editableTable(rubricModel);

        if (assignment == null) {
            addDefaultRubricRows();
            anonymousBox.setSelected(true);
        } else {
            titleField.setText(assignment.getTitle());
            deadlineField.setText(assignment.getDeadline() == null ? "" : Dates.format(assignment.getDeadline()));
            reviewersSpinner.setValue(assignment.getReviewersPerSubmission());
            selectMode(assignment.getScoringMode());
            anonymousBox.setSelected(assignment.isAnonymous());
            descriptionArea.setText(assignment.getDescription());
            for (RubricItem item : assignment.getRubricItems()) {
                rubricModel.addRow(new Object[]{item.getName(), item.getWeight(),
                        RubricItem.trim(item.getMinScore()), RubricItem.trim(item.getMaxScore())});
            }
        }

        rubricModel.addTableModelListener(event -> refreshWeightLabel());
        refreshWeightLabel();

        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        setSize(new Dimension(840, 820));
        setLocationRelativeTo(owner);
    }

    public static Optional<Assignment> show(Component parent, AppContext context, User teacher,
                                            long courseId, Assignment assignment) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        AssignmentEditorDialog dialog = new AssignmentEditorDialog(owner, context, teacher, courseId, assignment);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.result);
    }

    private void addDefaultRubricRows() {
        rubricModel.addRow(new Object[]{"功能完整性", 40, 0, 10});
        rubricModel.addRow(new Object[]{"代码规范性", 30, 0, 10});
        rubricModel.addRow(new Object[]{"设计与创新", 30, 0, 10});
    }

    // ———————————————————— 结构 ————————————————————

    private JComponent buildHeader() {
        JPanel header = Ui.column(4);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(20, 26, 16, 26)));
        header.add(Ui.h1(editing == null ? "新建作业" : "编辑作业"));
        header.add(Ui.muted("评分细则的权重合计必须为 100%，学生将按这些维度逐项打分"));
        return header;
    }

    private JComponent buildBody() {
        JPanel form = Ui.column(0);
        form.setBorder(new EmptyBorder(18, 26, 18, 26));

        form.add(fieldLabel("作业名称"));
        form.add(fill(titleField));
        form.add(Ui.gap(0, 14));

        form.add(fieldLabel("截止时间"));
        form.add(fill(deadlineField));
        form.add(Ui.gap(0, 14));

        JPanel options = Ui.row(18);
        options.add(fillLabeled("每份作业评审人数", reviewersSpinner));
        options.add(fillLabeled("计分方式", modeCombo));
        options.add(Ui.stretch());
        form.add(options);
        form.add(Ui.gap(0, 12));
        form.add(anonymousBox);
        form.add(Ui.gap(0, 14));

        form.add(fieldLabel("作业要求"));
        JScrollPane descriptionScroll = Ui.scroll(descriptionArea);
        descriptionScroll.setPreferredSize(new Dimension(0, 98));
        descriptionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        descriptionScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        descriptionScroll.getViewport().setBackground(Color.WHITE);
        form.add(descriptionScroll);
        form.add(Ui.gap(0, 20));

        form.add(buildRubricToolbar());
        form.add(Ui.gap(0, 8));

        JScrollPane rubricScroll = Ui.tableScroll(rubricTable);
        rubricScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        rubricScroll.setPreferredSize(new Dimension(0, 190));
        rubricScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        form.add(rubricScroll);
        form.add(Ui.gap(0, 8));
        form.add(Ui.hint("提示：分值区间可不同（如 0~10 与 0~5），系统会按所选计分方式折算总分"));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.SURFACE);
        wrapper.add(form, BorderLayout.NORTH);

        JScrollPane outer = Ui.scroll(wrapper);
        outer.getViewport().setBackground(Theme.SURFACE);
        return outer;
    }

    private JComponent buildRubricToolbar() {
        JPanel bar = Ui.row(10);
        bar.add(Ui.h2("评分细则"));

        weightLabel.setFont(Theme.bold(12));
        bar.add(weightLabel);
        bar.add(Ui.stretch());

        FlatButton addRow = Ui.secondary("添加维度").compact();
        addRow.addActionListener(event -> {
            rubricModel.addRow(new Object[]{"新维度", 0, 0, 10});
            int last = rubricModel.getRowCount() - 1;
            rubricTable.setRowSelectionInterval(last, last);
        });

        FlatButton removeRow = Ui.ghost("删除选中").compact();
        removeRow.addActionListener(event -> {
            int selected = rubricTable.getSelectedRow();
            if (selected < 0) {
                Dialogs.info(this, "请先在表格中选中要删除的维度");
                return;
            }
            rubricModel.removeRow(selected);
        });

        FlatButton balance = Ui.ghost("均分权重").compact();
        balance.addActionListener(event -> distributeWeights());

        bar.add(addRow);
        bar.add(removeRow);
        bar.add(balance);
        return bar;
    }

    private JComponent buildFooter() {
        FlatButton cancel = Ui.secondary("取消");
        cancel.addActionListener(event -> dispose());
        FlatButton save = Ui.primary(editing == null ? "创建作业" : "保存修改");
        save.addActionListener(event -> save());

        JPanel footer = Ui.row(10);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(14, 26, 16, 26)));
        footer.add(Ui.stretch());
        footer.add(cancel);
        footer.add(save);
        getRootPane().setDefaultButton(save);
        return footer;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = Ui.boldLabel(text);
        label.setBorder(new EmptyBorder(0, 0, 6, 0));
        return label;
    }

    private JComponent fill(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    private JComponent fillLabeled(String label, JComponent field) {
        JPanel panel = Ui.column(6);
        JLabel caption = Ui.boldLabel(label);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setPreferredSize(new Dimension(150, 34));
        field.setMaximumSize(new Dimension(180, 34));
        panel.add(caption);
        panel.add(field);
        return panel;
    }

    // ———————————————————— 交互 ————————————————————

    /** 把 100% 尽量平均地分给所有维度，余数给前几行。 */
    private void distributeWeights() {
        int rows = rubricModel.getRowCount();
        if (rows == 0) {
            return;
        }
        int base = 100 / rows;
        int remainder = 100 % rows;
        for (int i = 0; i < rows; i++) {
            rubricModel.setValueAt(base + (i < remainder ? 1 : 0), i, 1);
        }
    }

    private void refreshWeightLabel() {
        int total = 0;
        boolean parsable = true;
        for (int i = 0; i < rubricModel.getRowCount(); i++) {
            try {
                total += Integer.parseInt(String.valueOf(rubricModel.getValueAt(i, 1)).trim());
            } catch (NumberFormatException ex) {
                parsable = false;
            }
        }
        if (!parsable) {
            weightLabel.setText("权重合计：存在非法数字");
            weightLabel.setForeground(Theme.DANGER);
            return;
        }
        weightLabel.setText("权重合计：" + total + "%");
        weightLabel.setForeground(total == 100 ? Theme.SUCCESS : Theme.DANGER);
    }

    private void save() {
        try {
            Assignment draft = collect();
            if (editing == null) {
                result = context.assignments().create(teacher, courseId, draft);
            } else {
                draft.setId(editing.getId());
                draft.setCourseId(editing.getCourseId());
                draft.setStatus(editing.getStatus());
                draft.setCreatedAt(editing.getCreatedAt());
                context.assignments().update(teacher, draft);
                result = draft;
            }
            dispose();
        } catch (RuntimeException ex) {
            Dialogs.error(this, ex);
        }
    }

    private Assignment collect() {
        Assignment draft = new Assignment();
        draft.setTitle(titleField.getText());
        draft.setDescription(descriptionArea.getText());
        draft.setDeadline(Dates.parseOptional(deadlineField.getText(), "截止时间"));
        draft.setAnonymous(anonymousBox.isSelected());
        draft.setReviewersPerSubmission((Integer) reviewersSpinner.getValue());
        draft.setScoringMode(selectedMode());
        if (editing != null) {
            draft.setStatus(editing.getStatus());
        } else {
            draft.setStatus(AssignmentStatus.DRAFT);
        }
        draft.setRubricItems(collectRubric());
        return draft;
    }

    private void selectMode(ScoringMode mode) {
        for (int i = 0; i < modeCombo.getItemCount(); i++) {
            if (modeCombo.getItemAt(i).value() == mode) {
                modeCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private ScoringMode selectedMode() {
        Choice<ScoringMode> choice = modeCombo.getItemAt(modeCombo.getSelectedIndex());
        return choice == null ? ScoringMode.NORMALIZED : choice.value();
    }

    private List<RubricItem> collectRubric() {        if (rubricModel.getRowCount() == 0) {
            throw new ValidationException("请至少添加一条评分细则");
        }
        List<RubricItem> items = new ArrayList<>();
        for (int i = 0; i < rubricModel.getRowCount(); i++) {
            String name = Text.trimToEmpty(String.valueOf(rubricModel.getValueAt(i, 0)));
            if (name.isEmpty()) {
                throw new ValidationException("第 " + (i + 1) + " 行细则缺少维度名称");
            }
            items.add(new RubricItem(
                    name,
                    parseInt(rubricModel.getValueAt(i, 1), "维度「" + name + "」的权重"),
                    parseDouble(rubricModel.getValueAt(i, 2), "维度「" + name + "」的最低分"),
                    parseDouble(rubricModel.getValueAt(i, 3), "维度「" + name + "」的最高分")));
        }
        return items;
    }

    private int parseInt(Object value, String field) {
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException(field + "必须是整数");
        }
    }

    private double parseDouble(Object value, String field) {
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException(field + "必须是数字");
        }
    }
}
