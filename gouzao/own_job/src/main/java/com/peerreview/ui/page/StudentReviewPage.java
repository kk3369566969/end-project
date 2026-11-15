package com.peerreview.ui.page;

import com.peerreview.AppContext;
import com.peerreview.common.Dates;
import com.peerreview.common.RegexTextMatcher;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.Review;
import com.peerreview.domain.TaskRow;
import com.peerreview.domain.TaskStatus;
import com.peerreview.domain.User;
import com.peerreview.service.query.TaskFilter;
import com.peerreview.ui.Badges;
import com.peerreview.ui.Dialogs;
import com.peerreview.ui.Icons;
import com.peerreview.ui.Page;
import com.peerreview.ui.Theme;
import com.peerreview.ui.Ui;
import com.peerreview.ui.component.Card;
import com.peerreview.ui.component.FlatButton;
import com.peerreview.ui.model.Choice;
import com.peerreview.ui.model.SimpleTableModel;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

/**
 * 学生端「互评任务」页：列出待评/已评任务，进入打分窗口完成互评。
 */
public final class StudentReviewPage implements Page {

    private static final Choice<TaskStatus> ALL = Choice.all("全部状态");
    private static final Choice<TaskStatus> PENDING = Choice.of("待互评", TaskStatus.PENDING);
    private static final Choice<TaskStatus> COMPLETED = Choice.of("已完成", TaskStatus.COMPLETED);

    private final AppContext context;
    private final User student;

    private final JPanel view = new JPanel(new BorderLayout(0, 14));
    private final JComboBox<Choice<TaskStatus>> statusCombo = Ui.combo(List.of(ALL, PENDING, COMPLETED));
    private final JTextField keywordField = Ui.field("正则，如 模块化", 20);
    private final JLabel countLabel = Ui.muted("");

    private final SimpleTableModel<TaskRow> model;
    private final JTable table;
    private final JPanel tableContainer = new JPanel(new BorderLayout());
    private final FlatButton openButton = Ui.primary("开始评审").compact();

    public StudentReviewPage(AppContext context, User student) {
        this.context = context;
        this.student = student;

        model = new SimpleTableModel<>(List.of(
                new SimpleTableModel.Column<>("课程", 130, TaskRow::courseName),
                new SimpleTableModel.Column<>("作业", 230, TaskRow::assignmentTitle),
                new SimpleTableModel.Column<>("被评同学", 100, TaskRow::authorLabel),
                new SimpleTableModel.Column<>("状态", 104, row -> Badges.task(row.status())),
                new SimpleTableModel.Column<>("我的评分", 90, TaskRow::scoreLabel),
                new SimpleTableModel.Column<>("完成时间", 140,
                        row -> Dates.format(row.completedAt()))));
        table = Ui.table(model);

        view.setOpaque(false);
        view.add(buildFilterCard(), BorderLayout.NORTH);
        view.add(buildTableCard(), BorderLayout.CENTER);

        wireEvents();
    }

    private JComponent buildFilterCard() {
        Card card = Ui.card(new BorderLayout());
        JPanel row = Ui.row(12);

        JPanel statusBlock = Ui.column(6);
        statusBlock.add(Ui.boldLabel("任务状态"));
        statusCombo.setPreferredSize(new Dimension(140, 34));
        statusCombo.setMaximumSize(new Dimension(140, 34));
        statusBlock.add(statusCombo);

        JPanel keywordBlock = Ui.column(6);
        keywordBlock.add(Ui.boldLabel("内容关键词（正则）"));
        keywordField.setPreferredSize(new Dimension(240, 34));
        keywordField.setMaximumSize(new Dimension(240, 34));
        keywordBlock.add(keywordField);

        FlatButton search = Ui.primary("筛选").compact().withIcon(Icons.search(15, java.awt.Color.WHITE));
        search.addActionListener(event -> reload());
        FlatButton reset = Ui.ghost("重置").compact();
        reset.addActionListener(event -> {
            statusCombo.setSelectedIndex(0);
            keywordField.setText("");
            reload();
        });

        JPanel buttonBlock = Ui.column(6);
        buttonBlock.add(Ui.boldLabel(" "));
        JPanel buttons = Ui.row(8);
        buttons.add(search);
        buttons.add(reset);
        buttonBlock.add(buttons);

        row.add(statusBlock);
        row.add(keywordBlock);
        row.add(buttonBlock);
        row.add(Ui.stretch());
        row.add(countLabel);

        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildTableCard() {
        Card card = Ui.card(new BorderLayout(0, 12));

        JPanel header = Ui.row(10);
        header.add(Ui.h2("我的互评任务"));
        header.add(Ui.muted("双击任务或点击右侧按钮进入打分"));
        header.add(Ui.stretch());
        header.add(openButton);

        tableContainer.setOpaque(false);
        tableContainer.add(Ui.tableScroll(table), BorderLayout.CENTER);

        card.add(header, BorderLayout.NORTH);
        card.add(tableContainer, BorderLayout.CENTER);
        return card;
    }

    private void wireEvents() {
        statusCombo.addActionListener(event -> reload());
        keywordField.addActionListener(event -> reload());
        openButton.addActionListener(event -> openSelected());

        table.getSelectionModel().addListSelectionListener(this::onRowSelected);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    openSelected();
                }
            }
        });
    }

    private void onRowSelected(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        TaskRow selected = selectedTask();
        openButton.setEnabled(selected != null);
        openButton.setText(selected != null && selected.isCompleted() ? "查看评审" : "开始评审");
    }

    private TaskRow selectedTask() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.rowAt(table.convertRowIndexToModel(row));
    }

    @Override
    public void onShow() {
        reload();
    }

    private void reload() {
        try {
            Choice<TaskStatus> statusChoice = (Choice<TaskStatus>) statusCombo.getSelectedItem();
            TaskFilter filter = new TaskFilter()
                    .status(statusChoice == null ? null : statusChoice.value())
                    .keyword(RegexTextMatcher.of(keywordField.getText()));

            List<TaskRow> rows = context.reviews()
                    .myTasks(student.getId(), null, RegexTextMatcher.matchAll())
                    .stream()
                    .filter(filter::accepts)
                    .toList();

            model.setRows(rows);
            long pending = rows.stream().filter(row -> !row.isCompleted()).count();
            countLabel.setText("共 " + rows.size() + " 条任务 · 待互评 " + pending + " 条");

            tableContainer.removeAll();
            if (rows.isEmpty()) {
                tableContainer.add(Ui.emptyState("暂无互评任务",
                        "教师完成分配后，这里会出现需要你评审的作业"), BorderLayout.CENTER);
                openButton.setEnabled(false);
                openButton.setText("开始评审");
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

    private void openSelected() {
        TaskRow task = selectedTask();
        if (task == null) {
            Dialogs.info(view, "请先选择一条互评任务");
            return;
        }
        try {
            Assignment assignment = context.reviews().assignmentOf(task.assignmentId());
            Optional<Review> existing = context.reviews().reviewOfTask(task.taskId());
            if (task.isCompleted() && existing.isEmpty()) {
                Dialogs.info(view, "该任务已完成，但未找到对应评分记录");
                return;
            }
            ReviewDialog.show(view, context, student, task, assignment, existing);
            reload();
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
        }
    }

    // ———————————————————— Page ————————————————————

    @Override
    public String navLabel() {
        return "互评任务";
    }

    @Override
    public Icon navIcon() {
        return Icons.star(17, Theme.NAV_ICON);
    }

    @Override
    public String title() {
        return "我的互评任务";
    }

    @Override
    public String subtitle() {
        return "盲评模式下按细则逐项打分，评语与分数都会记入统计";
    }

    @Override
    public JComponent view() {
        return view;
    }
}
