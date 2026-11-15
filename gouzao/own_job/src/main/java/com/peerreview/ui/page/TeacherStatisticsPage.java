package com.peerreview.ui.page;

import com.peerreview.AppContext;
import com.peerreview.common.Dates;
import com.peerreview.common.RegexTextMatcher;
import com.peerreview.common.Text;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.AssignmentSummary;
import com.peerreview.domain.Course;
import com.peerreview.domain.ReviewRow;
import com.peerreview.domain.SubmissionStats;
import com.peerreview.domain.User;
import com.peerreview.ui.Dialogs;
import com.peerreview.ui.Icons;
import com.peerreview.ui.Page;
import com.peerreview.ui.Theme;
import com.peerreview.ui.Ui;
import com.peerreview.ui.component.Card;
import com.peerreview.ui.component.FlatButton;
import com.peerreview.ui.component.KpiCard;
import com.peerreview.ui.model.Choice;
import com.peerreview.ui.model.SimpleTableModel;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 教师端「统计与导出」页：按作业汇总平均/最高/最低分，并可按评语关键词检索。
 */
public final class TeacherStatisticsPage implements Page {

    private final AppContext context;
    private final User teacher;

    private final JPanel view = new JPanel(new BorderLayout(0, 14));
    private final JComboBox<Choice<Course>> courseCombo = Ui.combo(List.of());
    private final JComboBox<Choice<Assignment>> assignmentCombo = Ui.combo(List.of());
    private final JTextField reviewKeyword = Ui.field("评语正则，如 建议", 16);

    private final KpiCard submissionKpi = KpiCard.neutral("提交份数");
    private final KpiCard reviewKpi = KpiCard.primary("互评条数");
    private final KpiCard averageKpi = KpiCard.primary("平均分");
    private final KpiCard maxKpi = KpiCard.success("最高分");
    private final KpiCard minKpi = KpiCard.neutral("最低分");

    private final SimpleTableModel<SubmissionStats> statsModel;
    private final JTable statsTable;
    private final SimpleTableModel<ReviewRow> reviewModel;
    private final JTable reviewTable;
    private final JLabel detailTitle = Ui.h2("评审明细");
    private final JLabel detailMeta = Ui.muted("选择一名学生查看其收到的全部评语");
    private final JPanel statsContainer = new JPanel(new BorderLayout());

    private Assignment currentAssignment;
    private SubmissionStats selectedStats;
    private boolean loadingFilters;

    public TeacherStatisticsPage(AppContext context, User teacher) {
        this.context = context;
        this.teacher = teacher;

        statsModel = new SimpleTableModel<>(List.of(
                new SimpleTableModel.Column<>("学生", 150, SubmissionStats::studentName),
                new SimpleTableModel.Column<>("评审份数", 100, s -> s.counted() + " 份"),
                new SimpleTableModel.Column<>("平均分", 100, SubmissionStats::averageLabel),
                new SimpleTableModel.Column<>("最高分", 100, SubmissionStats::maxLabel),
                new SimpleTableModel.Column<>("最低分", 100, SubmissionStats::minLabel),
                new SimpleTableModel.Column<>("分数离散度", 120, s -> s.counted() <= 1 ? "—"
                        : com.peerreview.domain.RubricItem.trim(s.max() - s.min()))));
        statsTable = Ui.table(statsModel);

        reviewModel = new SimpleTableModel<>(List.of(
                new SimpleTableModel.Column<>("评审人", 120, ReviewRow::reviewerName),
                new SimpleTableModel.Column<>("总分", 80, ReviewRow::scoreLabel),
                new SimpleTableModel.Column<>("评语", 520, ReviewRow::comment)));
        reviewTable = Ui.table(reviewModel);

        view.setOpaque(false);
        view.add(buildFilterCard(), BorderLayout.NORTH);
        view.add(buildBody(), BorderLayout.CENTER);

        wireEvents();
    }

    private JComponent buildFilterCard() {
        Card card = Ui.card(new BorderLayout());
        JPanel row = Ui.row(12);
        row.add(fieldBlock("课程", courseCombo, 175));
        row.add(fieldBlock("作业", assignmentCombo, 240));
        row.add(fieldBlock("评语检索（正则）", reviewKeyword, 190));

        FlatButton export = Ui.primary("导出 CSV").compact();
        export.addActionListener(event -> exportCsv());
        JPanel buttons = Ui.column(6);
        buttons.add(Ui.boldLabel(" "));
        buttons.add(export);

        row.add(buttons);
        row.add(Ui.stretch());
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

    private JComponent buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);

        JPanel kpiRow = new JPanel(new GridLayout(1, 5, 14, 0));
        kpiRow.setOpaque(false);
        kpiRow.setPreferredSize(new Dimension(0, 102));
        kpiRow.add(submissionKpi);
        kpiRow.add(reviewKpi);
        kpiRow.add(averageKpi);
        kpiRow.add(maxKpi);
        kpiRow.add(minKpi);

        Card statsCard = Ui.card(new BorderLayout(0, 12));
        JPanel statsHeader = Ui.row(10);
        statsHeader.add(Ui.h2("成绩汇总"));
        statsHeader.add(Ui.muted("按平均分排序，未收到互评的提交排在最后"));
        statsHeader.add(Ui.stretch());
        statsCard.add(statsHeader, BorderLayout.NORTH);
        statsContainer.setOpaque(false);
        statsContainer.add(Ui.tableScroll(statsTable), BorderLayout.CENTER);
        statsCard.add(statsContainer, BorderLayout.CENTER);

        Card detailCard = Ui.card(new BorderLayout(0, 10));
        detailCard.setPreferredSize(new Dimension(0, 236));
        JPanel detailHeader = Ui.row(10);
        detailHeader.add(detailTitle);
        detailHeader.add(detailMeta);
        detailHeader.add(Ui.stretch());
        detailCard.add(detailHeader, BorderLayout.NORTH);
        detailCard.add(Ui.tableScroll(reviewTable), BorderLayout.CENTER);

        body.add(kpiRow, BorderLayout.NORTH);
        body.add(statsCard, BorderLayout.CENTER);
        body.add(detailCard, BorderLayout.SOUTH);
        return body;
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
        reviewKeyword.addActionListener(event -> reloadReviews());
        statsTable.getSelectionModel().addListSelectionListener(this::onRowSelected);
    }

    @Override
    public void onShow() {
        reloadCourseChoices();
        reload();
    }

    // ———————————————————— 筛选联动 ————————————————————

    private void reloadCourseChoices() {
        loadingFilters = true;
        try {
            doReloadCourseChoices();
        } finally {
            loadingFilters = false;
        }
    }

    private void doReloadCourseChoices() {
        Choice<Course> keep = selectedCourse();
        Long keepId = keep == null || keep.isAll() ? null : keep.value().getId();
        List<Choice<Course>> choices = new ArrayList<>();
        choices.add(Choice.all("全部课程"));
        for (Course course : context.courses().listAll()) {
            choices.add(Choice.of(course.getName() + " · " + course.getSemester(), course));
        }
        courseCombo.removeAllItems();
        choices.forEach(courseCombo::addItem);
        for (int i = 0; i < courseCombo.getItemCount(); i++) {
            Choice<Course> choice = courseCombo.getItemAt(i);
            if (keepId == null ? choice.isAll() : (!choice.isAll() && choice.value().getId() == keepId)) {
                courseCombo.setSelectedIndex(i);
                break;
            }
        }
        reloadAssignmentChoices();
    }

    private void reloadAssignmentChoices() {
        boolean previous = loadingFilters;
        loadingFilters = true;
        try {
            doReloadAssignmentChoices();
        } finally {
            loadingFilters = previous;
        }
    }

    private void doReloadAssignmentChoices() {
        Choice<Course> course = selectedCourse();
        List<Assignment> assignments = course == null || course.isAll()
                ? context.assignments().listAll()
                : context.assignments().listByCourse(course.value().getId());
        Choice<Assignment> keep = selectedAssignment();
        Long keepId = keep == null || keep.isAll() ? null : keep.value().getId();

        List<Choice<Assignment>> choices = new ArrayList<>();
        for (Assignment assignment : assignments) {
            choices.add(Choice.of(assignment.getTitle(), assignment));
        }
        assignmentCombo.removeAllItems();
        choices.forEach(assignmentCombo::addItem);
        if (choices.isEmpty()) {
            return;
        }
        int target = 0;
        if (keepId != null) {
            for (int i = 0; i < choices.size(); i++) {
                if (choices.get(i).value().getId() == keepId) {
                    target = i;
                    break;
                }
            }
        }
        assignmentCombo.setSelectedIndex(target);
    }

    private Choice<Course> selectedCourse() {
        return (Choice<Course>) courseCombo.getSelectedItem();
    }

    private Choice<Assignment> selectedAssignment() {
        return (Choice<Assignment>) assignmentCombo.getSelectedItem();
    }

    // ———————————————————— 数据加载 ————————————————————

    private void reload() {
        Choice<Assignment> choice = selectedAssignment();
        if (choice == null) {
            currentAssignment = null;
            statsModel.setRows(List.of());
            applySummary(AssignmentSummary.empty());
            statsContainer.removeAll();
            statsContainer.add(Ui.emptyState("暂无可统计的作业", "请先在「课程与作业」中创建并发布作业"),
                    BorderLayout.CENTER);
            statsContainer.revalidate();
            statsContainer.repaint();
            return;
        }
        currentAssignment = choice.value();
        try {
            AssignmentSummary summary = context.statistics().summaryFor(currentAssignment.getId());
            applySummary(summary);
            List<SubmissionStats> stats = context.statistics().statsFor(currentAssignment.getId());
            statsModel.setRows(stats);
            statsContainer.removeAll();
            if (stats.isEmpty()) {
                statsContainer.add(Ui.emptyState("该作业还没有提交", "学生提交后即可在此统计互评结果"),
                        BorderLayout.CENTER);
                detailModelClear();
            } else {
                statsContainer.add(Ui.tableScroll(statsTable), BorderLayout.CENTER);
                statsTable.setRowSelectionInterval(0, 0);
            }
            statsContainer.revalidate();
            statsContainer.repaint();
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
        }
    }

    private void applySummary(AssignmentSummary summary) {
        submissionKpi.set(String.valueOf(summary.submissions()), summary.submissionLabel());
        reviewKpi.set(String.valueOf(summary.reviewCount()), summary.completionLabel());
        averageKpi.set(summary.averageLabel(), "按每份提交的平均分统计");
        maxKpi.set(summary.maxLabel(), "最高平均分");
        minKpi.set(summary.minLabel(), "最低平均分");
    }

    private void onRowSelected(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        int row = statsTable.getSelectedRow();
        selectedStats = row < 0 ? null : statsModel.rowAt(statsTable.convertRowIndexToModel(row));
        reloadReviews();
    }

    private void reloadReviews() {
        if (selectedStats == null) {
            detailModelClear();
            return;
        }
        try {
            RegexTextMatcher matcher = RegexTextMatcher.of(reviewKeyword.getText());
            List<ReviewRow> reviews = context.reviews().reviewsOfSubmission(selectedStats.submissionId()).stream()
                    .filter(review -> matcher.matches(review.comment()))
                    .toList();
            reviewModel.setRows(reviews);
            detailTitle.setText(selectedStats.studentName() + " 的评审明细");
            detailMeta.setText("共 " + reviews.size() + " 条"
                    + (matcher.isActive() ? "（已按正则过滤）" : "")
                    + " · 平均 " + selectedStats.averageLabel());
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
        }
    }

    private void detailModelClear() {
        reviewModel.setRows(List.of());
        detailTitle.setText("评审明细");
        detailMeta.setText("选择一名学生查看其收到的全部评语");
    }

    // ———————————————————— 导出 ————————————————————

    private void exportCsv() {
        if (currentAssignment == null) {
            Dialogs.info(view, "请先选择要导出的作业");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出成绩汇总 CSV");
        chooser.setSelectedFile(new File(defaultFileName()));
        int choice = chooser.showSaveDialog(view);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path target = chooser.getSelectedFile().toPath();
            Path written = context.statistics().exportCsv(currentAssignment, target);
            Dialogs.success(view, "已导出 " + written.toAbsolutePath() + "\n可直接用 Excel 打开并粘贴到成绩表。");
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
        }
    }

    private String defaultFileName() {
        String title = Text.trimToEmpty(currentAssignment.getTitle()).replaceAll("[\\\\/:*?\"<>|]", "_");
        return "成绩汇总-" + title + "-" + Dates.fileStamp(LocalDateTime.now()) + ".csv";
    }

    // ———————————————————— Page ————————————————————

    @Override
    public String navLabel() {
        return "统计与导出";
    }

    @Override
    public Icon navIcon() {
        return Icons.chart(17, Theme.NAV_ICON);
    }

    @Override
    public String title() {
        return "互评统计";
    }

    @Override
    public String subtitle() {
        return "自动加权计算综合得分，汇总平均/最高/最低分并导出 CSV";
    }

    @Override
    public JComponent view() {
        return view;
    }
}
