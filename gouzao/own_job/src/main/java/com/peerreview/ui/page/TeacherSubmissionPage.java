package com.peerreview.ui.page;

import com.peerreview.AppContext;
import com.peerreview.common.RegexTextMatcher;
import com.peerreview.common.Text;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.Course;
import com.peerreview.domain.SubmissionRow;
import com.peerreview.domain.User;
import com.peerreview.service.query.SubmissionFilter;
import com.peerreview.ui.Dialogs;
import com.peerreview.ui.Icons;
import com.peerreview.ui.Page;
import com.peerreview.ui.Theme;
import com.peerreview.ui.Ui;
import com.peerreview.ui.component.Card;
import com.peerreview.ui.component.FlatButton;
import com.peerreview.ui.model.Choice;
import com.peerreview.ui.model.SimpleTableModel;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * 教师端「提交管理」页：多条件 + 正则检索全部提交，并预览作业正文。
 */
public final class TeacherSubmissionPage implements Page {

    private final AppContext context;
    private final User teacher;

    private final JPanel view = new JPanel(new BorderLayout(0, 14));
    private final JComboBox<Choice<Course>> courseCombo = Ui.combo(List.of());
    private final JComboBox<Choice<Assignment>> assignmentCombo = Ui.combo(List.of());
    private final JTextField keywordField = Ui.field("正则，如 策略模式", 18);
    private final JLabel countLabel = Ui.muted("");

    private final SimpleTableModel<SubmissionRow> model;
    private final JTable table;
    private final JTextArea preview = Ui.readOnlyArea(8);
    private final JLabel previewTitle = Ui.h2("作业内容");
    private final JLabel previewMeta = Ui.muted("在左侧列表中选择一条提交查看正文");
    private final JPanel tableContainer = new JPanel(new BorderLayout());

    private boolean loadingFilters;

    public TeacherSubmissionPage(AppContext context, User teacher) {
        this.context = context;
        this.teacher = teacher;

        model = new SimpleTableModel<>(List.of(
                new SimpleTableModel.Column<>("学生", 110, SubmissionRow::studentName),
                new SimpleTableModel.Column<>("课程", 120, SubmissionRow::courseName),
                new SimpleTableModel.Column<>("作业", 210, SubmissionRow::assignmentTitle),
                new SimpleTableModel.Column<>("提交时间", 140,
                        row -> com.peerreview.common.Dates.format(row.submittedAt())),
                new SimpleTableModel.Column<>("字数", 70,
                        row -> String.valueOf(Text.countVisibleChars(row.content()))),
                new SimpleTableModel.Column<>("内容摘要", 320,
                        row -> Text.ellipsis(row.content(), 60))));
        table = Ui.table(model);

        view.setOpaque(false);
        view.add(buildFilterCard(), BorderLayout.NORTH);
        tableContainer.setOpaque(false);
        tableContainer.add(Ui.tableScroll(table), BorderLayout.CENTER);
        view.add(buildTableCard(), BorderLayout.CENTER);
        view.add(buildPreviewCard(), BorderLayout.SOUTH);

        wireEvents();
    }

    private JComponent buildFilterCard() {
        Card card = Ui.card(new BorderLayout());
        JPanel row = Ui.row(12);

        row.add(fieldBlock("课程", courseCombo, 170));
        row.add(fieldBlock("作业", assignmentCombo, 230));
        row.add(fieldBlock("内容关键词（正则）", keywordField, 230));

        FlatButton search = Ui.primary("搜索").compact().withIcon(Icons.search(15, java.awt.Color.WHITE));
        search.addActionListener(event -> reload());
        FlatButton reset = Ui.ghost("重置").compact();
        reset.addActionListener(event -> {
            keywordField.setText("");
            if (courseCombo.getItemCount() > 0) {
                courseCombo.setSelectedIndex(0);
            }
            reload();
        });

        JPanel buttons = Ui.column(6);
        buttons.add(Ui.boldLabel(" "));
        JPanel buttonRow = Ui.row(8);
        buttonRow.add(search);
        buttonRow.add(reset);
        buttons.add(buttonRow);

        row.add(buttons);
        row.add(Ui.stretch());
        row.add(countLabel);

        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JComponent fieldBlock(String label, JComponent field, int width) {
        JPanel panel = Ui.column(6);
        panel.add(Ui.boldLabel(label));
        field.setPreferredSize(new Dimension(width, 34));
        field.setMaximumSize(new Dimension(width, 34));
        panel.add(field);
        return panel;
    }

    private JComponent buildTableCard() {
        Card card = Ui.card(new BorderLayout(0, 12));
        JPanel header = Ui.row(10);
        header.add(Ui.h2("提交列表"));
        header.add(Ui.muted("支持按课程、作业筛选，并对作业正文做正则匹配"));
        header.add(Ui.stretch());
        card.add(header, BorderLayout.NORTH);
        card.add(tableContainer, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildPreviewCard() {
        Card card = Ui.card(new BorderLayout(0, 10));
        card.setPreferredSize(new Dimension(0, 230));

        JPanel header = Ui.row(10);
        header.add(previewTitle);
        header.add(previewMeta);
        header.add(Ui.stretch());

        JScrollPane scroll = Ui.scroll(preview);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        card.add(header, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void wireEvents() {
        courseCombo.addActionListener(event -> {
            if (loadingFilters) {
                return;
            }
            reloadAssignmentChoices();
            reload();
        });
        assignmentCombo.addActionListener(event -> {
            if (!loadingFilters) {
                reload();
            }
        });
        keywordField.addActionListener(event -> reload());
        table.getSelectionModel().addListSelectionListener(this::onRowSelected);
    }

    @Override
    public void onShow() {
        reloadCourseChoices();
        reload();
    }

    private void reloadCourseChoices() {
        loadingFilters = true;
        try {
            Choice<Course> keep = (Choice<Course>) courseCombo.getSelectedItem();
            Long keepId = keep == null || keep.isAll() ? null : keep.value().getId();
            List<Choice<Course>> choices = new ArrayList<>();
            choices.add(Choice.all("全部课程"));
            for (Course course : context.courses().listAll()) {
                choices.add(Choice.of(course.getName() + " · " + course.getSemester(), course));
            }
            courseCombo.removeAllItems();
            choices.forEach(courseCombo::addItem);
            selectCourse(keepId);
            reloadAssignmentChoices();
        } finally {
            loadingFilters = false;
        }
    }

    private void selectCourse(Long courseId) {
        for (int i = 0; i < courseCombo.getItemCount(); i++) {
            Choice<Course> choice = courseCombo.getItemAt(i);
            if (courseId == null ? choice.isAll()
                    : (!choice.isAll() && choice.value().getId() == courseId)) {
                courseCombo.setSelectedIndex(i);
                return;
            }
        }
        if (courseCombo.getItemCount() > 0) {
            courseCombo.setSelectedIndex(0);
        }
    }

    private void reloadAssignmentChoices() {
        boolean previous = loadingFilters;
        loadingFilters = true;
        try {
            Choice<Assignment> keep = (Choice<Assignment>) assignmentCombo.getSelectedItem();
            Long keepId = keep == null || keep.isAll() ? null : keep.value().getId();

            Choice<Course> courseChoice = (Choice<Course>) courseCombo.getSelectedItem();
            List<Assignment> assignments = courseChoice == null || courseChoice.isAll()
                    ? context.assignments().listAll()
                    : context.assignments().listByCourse(courseChoice.value().getId());

            List<Choice<Assignment>> choices = new ArrayList<>();
            choices.add(Choice.all("全部作业"));
            for (Assignment assignment : assignments) {
                choices.add(Choice.of(assignment.getTitle(), assignment));
            }
            assignmentCombo.removeAllItems();
            choices.forEach(assignmentCombo::addItem);

            boolean matched = false;
            for (int i = 0; i < assignmentCombo.getItemCount(); i++) {
                Choice<Assignment> choice = assignmentCombo.getItemAt(i);
                if (keepId != null && !choice.isAll() && choice.value().getId() == keepId) {
                    assignmentCombo.setSelectedIndex(i);
                    matched = true;
                    break;
                }
            }
            if (!matched && assignmentCombo.getItemCount() > 0) {
                assignmentCombo.setSelectedIndex(0);
            }
        } finally {
            loadingFilters = previous;
        }
    }

    private void reload() {
        try {
            Choice<Course> course = (Choice<Course>) courseCombo.getSelectedItem();
            Choice<Assignment> assignment = (Choice<Assignment>) assignmentCombo.getSelectedItem();

            SubmissionFilter filter = new SubmissionFilter()
                    .course(course == null || course.isAll() ? null : course.value().getId())
                    .assignment(assignment == null || assignment.isAll() ? null : assignment.value().getId())
                    .keyword(RegexTextMatcher.of(keywordField.getText()));

            List<SubmissionRow> rows = context.statistics().searchSubmissions(filter);
            model.setRows(rows);
            countLabel.setText("共 " + rows.size() + " 条提交");

            tableContainer.removeAll();
            if (rows.isEmpty()) {
                tableContainer.add(Ui.emptyState("没有匹配的提交", "换个课程/作业，或调整正则关键词"), BorderLayout.CENTER);
                clearPreview();
            } else {
                tableContainer.add(Ui.tableScroll(table), BorderLayout.CENTER);
                table.setRowSelectionInterval(0, 0);
            }
            tableContainer.revalidate();
            tableContainer.repaint();
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
        }
    }

    private void onRowSelected(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            clearPreview();
            return;
        }
        SubmissionRow selected = model.rowAt(table.convertRowIndexToModel(row));
        if (selected == null) {
            clearPreview();
            return;
        }
        previewTitle.setText(selected.studentName() + " 的提交");
        previewMeta.setText(selected.courseName() + " · " + selected.assignmentTitle()
                + " · " + com.peerreview.common.Dates.format(selected.submittedAt())
                + (selected.isEdited() ? " · 已于 " + com.peerreview.common.Dates.format(selected.updatedAt()) + " 修改" : ""));
        preview.setText(selected.content());
        preview.setCaretPosition(0);
    }

    private void clearPreview() {
        previewTitle.setText("作业内容");
        previewMeta.setText("在左侧列表中选择一条提交查看正文");
        preview.setText("");
    }

    // ———————————————————— Page ————————————————————

    @Override
    public String navLabel() {
        return "提交管理";
    }

    @Override
    public Icon navIcon() {
        return Icons.list(17, Theme.NAV_ICON);
    }

    @Override
    public String title() {
        return "作业提交";
    }

    @Override
    public String subtitle() {
        return "查看全部学生提交，支持正则检索与多条件过滤";
    }

    @Override
    public JComponent view() {
        return view;
    }
}
