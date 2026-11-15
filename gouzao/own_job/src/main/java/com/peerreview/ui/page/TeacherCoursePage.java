package com.peerreview.ui.page;

import com.peerreview.AppContext;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.AssignmentStatus;
import com.peerreview.domain.Course;
import com.peerreview.domain.RubricItem;
import com.peerreview.domain.User;
import com.peerreview.service.AllocationService;
import com.peerreview.ui.Badges;
import com.peerreview.ui.Dialogs;
import com.peerreview.ui.Icons;
import com.peerreview.ui.Page;
import com.peerreview.ui.Theme;
import com.peerreview.ui.Ui;
import com.peerreview.ui.component.Card;
import com.peerreview.ui.component.FlatButton;
import com.peerreview.ui.model.SimpleTableModel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Optional;

/**
 * 教师端「课程与作业」页：左侧课程列表，右侧作业列表与评分细则。
 *
 * <p>页面不直接操作 DAO，所有写操作都通过服务层完成；成功后重新拉取数据，
 * 保证界面状态与数据库一致（而不是在界面里手工拼状态）。</p>
 */
public final class TeacherCoursePage implements Page {

    private final AppContext context;
    private final User teacher;

    private final JPanel view = new JPanel(new BorderLayout(16, 0));
    private final DefaultListModel<Course> courseModel = new DefaultListModel<>();
    private final JList<Course> courseList = new JList<>(courseModel);

    private final SimpleTableModel<Assignment> assignmentModel;
    private final JTable assignmentTable;
    private final SimpleTableModel<RubricItem> rubricModel;
    private final JTable rubricTable;
    private final JPanel assignmentContainer = new JPanel(new BorderLayout());

    private final JLabel courseTitle = Ui.h1("我的课程");
    private final JLabel courseMeta = Ui.muted("请选择左侧课程查看作业");
    private final JLabel rubricSummary = Ui.muted("选择一次作业查看细则");

    private final FlatButton editCourseButton = Ui.secondary("编辑").compact();
    private final FlatButton deleteCourseButton = Ui.danger("删除").compact();
    private final FlatButton createAssignmentButton = Ui.primary("新建作业").compact();
    private final FlatButton editAssignmentButton = Ui.secondary("编辑").compact();
    private final FlatButton statusButton = Ui.secondary("发布作业").compact();
    private final FlatButton allocateButton = Ui.secondary("分配互评").compact();
    private final FlatButton deleteAssignmentButton = Ui.danger("删除").compact();

    private Course selectedCourse;
    private Assignment selectedAssignment;

    public TeacherCoursePage(AppContext context, User teacher) {
        this.context = context;
        this.teacher = teacher;

        assignmentModel = new SimpleTableModel<>(List.of(
                new SimpleTableModel.Column<>("作业名称", 250, Assignment::getTitle),
                new SimpleTableModel.Column<>("状态", 104, a -> Badges.assignment(a.getStatus())),
                new SimpleTableModel.Column<>("每份评审人数", 110, a -> a.getReviewersPerSubmission() + " 人"),
                new SimpleTableModel.Column<>("截止时间", 150, a -> com.peerreview.common.Dates.format(a.getDeadline())),
                new SimpleTableModel.Column<>("互评方式", 88, a -> a.isAnonymous() ? "匿名" : "实名"),
                new SimpleTableModel.Column<>("提交数", 84, a -> context.submissions().countByAssignment(a.getId()) + " 份")));
        assignmentTable = Ui.table(assignmentModel);

        rubricModel = new SimpleTableModel<>(List.of(
                new SimpleTableModel.Column<>("维度名称", 220, RubricItem::getName),
                new SimpleTableModel.Column<>("权重", 90, RubricItem::weightLabel),
                new SimpleTableModel.Column<>("分值区间", 140, RubricItem::rangeLabel)));
        rubricTable = Ui.table(rubricModel);

        view.setOpaque(false);
        view.add(buildCourseCard(), BorderLayout.WEST);
        view.add(buildAssignmentCard(), BorderLayout.CENTER);

        wireEvents();
    }

    // ———————————————————— 左侧课程 ————————————————————

    private JComponent buildCourseCard() {
        Card card = Ui.card(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(292, 0));

        FlatButton create = Ui.secondary("").withIcon(Icons.plus(15, Theme.TEXT)).compact();
        create.setToolTipText("新建课程");
        create.addActionListener(event -> createCourse());

        JPanel header = Ui.row(8);
        header.add(Ui.h2("我的课程"));
        header.add(Ui.stretch());
        header.add(create);

        courseList.setOpaque(false);
        courseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseList.setFixedCellHeight(58);
        courseList.setCellRenderer(new CourseCell());
        courseList.setBorder(new EmptyBorder(0, 0, 0, 0));

        JScrollPane scroll = Ui.scroll(courseList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        editCourseButton.addActionListener(event -> editCourse());
        deleteCourseButton.addActionListener(event -> deleteCourse());
        JPanel footer = Ui.row(8);
        footer.add(editCourseButton);
        footer.add(deleteCourseButton);
        footer.add(Ui.stretch());

        card.add(header, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    // ———————————————————— 右侧作业 ————————————————————

    private JComponent buildAssignmentCard() {
        Card card = Ui.card(new BorderLayout(0, 14));

        JPanel titleArea = Ui.column(4);
        titleArea.add(courseTitle);
        titleArea.add(courseMeta);

        JPanel toolbar = Ui.row(8);
        toolbar.add(createAssignmentButton);
        toolbar.add(editAssignmentButton);
        toolbar.add(statusButton);
        toolbar.add(allocateButton);
        toolbar.add(deleteAssignmentButton);
        toolbar.add(Ui.stretch());

        JPanel header = Ui.column(12);
        header.add(titleArea);
        header.add(toolbar);

        assignmentContainer.setOpaque(false);
        assignmentContainer.add(Ui.tableScroll(assignmentTable), BorderLayout.CENTER);
        assignmentContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 表格区高度随数据量自适应（上限 340），细则紧跟其后，避免少量数据时出现大片空白
        JPanel body = Ui.column(14);
        body.add(assignmentContainer);
        JComponent rubricPanel = buildRubricPanel();
        rubricPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 236));
        body.add(rubricPanel);
        body.add(javax.swing.Box.createVerticalGlue());

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildRubricPanel() {
        JPanel panel = Ui.column(8);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(14, 0, 0, 0)));

        JPanel header = Ui.row(10);
        header.add(Ui.h2("评分细则"));
        header.add(rubricSummary);
        header.add(Ui.stretch());

        JScrollPane scroll = Ui.tableScroll(rubricTable);
        // 表头 40 + 三行细则，保证常用维度数下无需滚动
        scroll.setPreferredSize(new Dimension(0, 186));
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        panel.add(header);
        panel.add(scroll);
        return panel;
    }

    // ———————————————————— 事件 ————————————————————

    private void wireEvents() {
        courseList.addListSelectionListener(this::onCourseSelected);
        assignmentTable.getSelectionModel().addListSelectionListener(this::onAssignmentSelected);

        createAssignmentButton.addActionListener(event -> createAssignment());
        editAssignmentButton.addActionListener(event -> editAssignment());
        statusButton.addActionListener(event -> toggleStatus());
        allocateButton.addActionListener(event -> allocate());
        deleteAssignmentButton.addActionListener(event -> deleteAssignment());

        updateCourseButtons();
        updateAssignmentButtons();
    }

    private void onCourseSelected(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        selectedCourse = courseList.getSelectedValue();
        updateCourseButtons();
        reloadAssignments();
    }

    private void onAssignmentSelected(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        int row = assignmentTable.getSelectedRow();
        selectedAssignment = row < 0 ? null : assignmentModel.rowAt(assignmentTable.convertRowIndexToModel(row));
        reloadRubric();
        updateAssignmentButtons();
    }

    // ———————————————————— 数据加载 ————————————————————

    @Override
    public void onShow() {
        reloadCourses();
    }

    private void reloadCourses() {
        Long keepId = selectedCourse == null ? null : selectedCourse.getId();
        List<Course> courses;
        try {
            courses = context.courses().listVisible(teacher);
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
            return;
        }
        courseModel.clear();
        courses.forEach(courseModel::addElement);

        if (courses.isEmpty()) {
            selectedCourse = null;
            courseList.clearSelection();
            courseTitle.setText("还没有课程");
            courseMeta.setText("点击右上角「＋」创建第一门课程");
            assignmentModel.setRows(List.of());
            assignmentContainer.removeAll();
            assignmentContainer.add(Ui.emptyState("暂无作业", "创建课程后即可布置作业"), BorderLayout.CENTER);
            resizeAssignmentArea(0);
            assignmentContainer.revalidate();
            assignmentContainer.repaint();
            rubricModel.setRows(List.of());
            rubricSummary.setText("选择一次作业查看细则");
            updateCourseButtons();
            updateAssignmentButtons();
            return;
        }

        int index = 0;
        for (int i = 0; i < courses.size(); i++) {
            if (keepId != null && courses.get(i).getId() == keepId) {
                index = i;
                break;
            }
        }
        courseList.setSelectedIndex(index);
        courseList.ensureIndexIsVisible(index);
        selectedCourse = courses.get(index);
        updateCourseButtons();
        reloadAssignments();
    }

    /** 表格区高度跟随行数变化：数据少时不留白，数据多时内部滚动。 */
    /** 作业表格按行数自适应高度：行少时不留空白，行多时最多 6 行后内部滚动。 */
    private void resizeAssignmentArea(int rowCount) {
        int contentHeight = 46 + rowCount * 42;
        int height = rowCount == 0 ? 240 : Math.min(340, contentHeight);
        assignmentContainer.setPreferredSize(new Dimension(0, height));
        assignmentContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        assignmentContainer.setMinimumSize(new Dimension(0, height));
    }

    private void reloadAssignments() {
        selectedAssignment = null;
        rubricModel.setRows(List.of());
        rubricSummary.setText("选择一次作业查看细则");

        if (selectedCourse == null) {
            assignmentModel.setRows(List.of());
            updateAssignmentButtons();
            return;
        }
        courseTitle.setText(selectedCourse.getName());
        courseMeta.setText(selectedCourse.subtitle() + " · 共 " + courseModel.size() + " 门课程");

        List<Assignment> assignments;
        try {
            assignments = context.assignments().listByCourse(selectedCourse.getId());
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
            return;
        }
        assignmentModel.setRows(assignments);

        assignmentContainer.removeAll();
        if (assignments.isEmpty()) {
            assignmentContainer.add(Ui.emptyState("该课程还没有作业", "点击「新建作业」布置第一次作业"),
                    BorderLayout.CENTER);
        } else {
            assignmentContainer.add(Ui.tableScroll(assignmentTable), BorderLayout.CENTER);
        }
        resizeAssignmentArea(assignments.size());
        assignmentContainer.revalidate();
        assignmentContainer.repaint();
        if (!assignments.isEmpty()) {
            // 默认选中第一条，让右侧评分细则立刻可见，避免大面积空白
            assignmentTable.setRowSelectionInterval(0, 0);
        }
        updateAssignmentButtons();
    }

    private void reloadRubric() {
        if (selectedAssignment == null) {
            rubricModel.setRows(List.of());
            rubricSummary.setText("选择一次作业查看细则");
            return;
        }
        List<RubricItem> items = selectedAssignment.getRubricItems();
        if (items.isEmpty()) {
            try {
                items = context.assignments().rubricItems(selectedAssignment.getId());
            } catch (RuntimeException ex) {
                Dialogs.error(view, ex);
                return;
            }
        }
        rubricModel.setRows(items);
        rubricSummary.setText("共 " + items.size() + " 个维度 · 权重合计 "
                + items.stream().mapToInt(RubricItem::getWeight).sum() + "%");
    }

    // ———————————————————— 操作 ————————————————————

    private void createCourse() {
        CourseEditorDialog.show(view, context, teacher, null)
                .ifPresent(course -> {
                    selectedCourse = course;
                    reloadCourses();
                });
    }

    private void editCourse() {
        if (selectedCourse == null) {
            return;
        }
        CourseEditorDialog.show(view, context, teacher, selectedCourse)
                .ifPresent(course -> reloadCourses());
    }

    private void deleteCourse() {
        if (selectedCourse == null) {
            return;
        }
        boolean confirmed = Dialogs.confirm(view, "删除课程",
                "删除课程「" + selectedCourse.getName() + "」会同时删除其下所有作业、提交与互评记录，\n"
                        + "该操作不可撤销，确定继续吗？", "删除课程");
        if (!confirmed) {
            return;
        }
        try {
            context.courses().delete(teacher, selectedCourse.getId());
            selectedCourse = null;
            reloadCourses();
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
        }
    }

    private void createAssignment() {
        if (selectedCourse == null) {
            Dialogs.info(view, "请先选择或创建一门课程");
            return;
        }
        Optional<Assignment> created = AssignmentEditorDialog.show(
                view, context, teacher, selectedCourse.getId(), null);
        if (created.isPresent()) {
            reloadAssignments();
            Dialogs.success(view, "作业「" + created.get().getTitle() + "」已创建，状态为草稿。\n"
                    + "发布后学生即可提交作业。");
        }
    }

    private void editAssignment() {
        if (selectedAssignment == null) {
            return;
        }
        AssignmentEditorDialog.show(view, context, teacher,
                        selectedAssignment.getCourseId(), selectedAssignment)
                .ifPresent(updated -> reloadAssignments());
    }

    private void toggleStatus() {
        if (selectedAssignment == null) {
            return;
        }
        AssignmentStatus status = selectedAssignment.getStatus();
        try {
            if (status == AssignmentStatus.DRAFT) {
                context.assignments().changeStatus(teacher, selectedAssignment.getId(), AssignmentStatus.PUBLISHED);
                Dialogs.success(view, "作业已发布，学生现在可以提交作业。");
            } else if (status == AssignmentStatus.PUBLISHED || status == AssignmentStatus.ALLOCATED) {
                boolean confirmed = Dialogs.confirm(view, "结束作业",
                        "结束后学生将无法继续提交或修改作业，确定结束吗？", "结束作业");
                if (!confirmed) {
                    return;
                }
                context.assignments().changeStatus(teacher, selectedAssignment.getId(), AssignmentStatus.CLOSED);
            } else {
                Dialogs.info(view, "该作业已结束，如需重新开放请在编辑中调整状态流程");
            }
            reloadAssignments();
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
        }
    }

    private void allocate() {
        if (selectedAssignment == null) {
            return;
        }
        long assignmentId = selectedAssignment.getId();
        int reviewers = selectedAssignment.getReviewersPerSubmission();
        int submissions = context.submissions().countByAssignment(assignmentId);
        if (submissions < 2) {
            Dialogs.info(view, "至少需要 2 份提交才能分配互评，当前只有 " + submissions + " 份。");
            return;
        }

        int existingTasks = context.allocation().taskCount(assignmentId);
        boolean force = false;
        if (existingTasks > 0) {
            force = Dialogs.confirm(view, "重新分配互评",
                    "该作业已存在 " + existingTasks + " 条互评任务，重新分配会清空这些任务\n"
                            + "（已提交的互评记录仍保留在统计中），确定继续吗？", "重新分配");
            if (!force) {
                return;
            }
        } else {
            boolean confirmed = Dialogs.confirm(view, "分配互评",
                    "将把 " + submissions + " 份提交随机分配给同学评审，每份作业由 " + reviewers + " 人评审。\n"
                            + "分配后作业状态将变为「互评中」，学生不能再修改提交，确定继续吗？", "开始分配");
            if (!confirmed) {
                return;
            }
        }

        try {
            AllocationService.AllocationResult result =
                    context.allocation().allocate(teacher, assignmentId, force);
            Dialogs.success(view, "互评任务分配完成：\n"
                    + "提交 " + result.submissionCount() + " 份，生成 " + result.taskCount()
                    + " 条任务（每份 " + result.reviewersPerSubmission() + " 人评审）"
                    + (result.skipped() > 0 ? "，跳过冲突 " + result.skipped() + " 条" : ""));
            reloadAssignments();
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
        }
    }

    private void deleteAssignment() {
        if (selectedAssignment == null) {
            return;
        }
        boolean confirmed = Dialogs.confirm(view, "删除作业",
                "删除作业「" + selectedAssignment.getTitle() + "」会同时清除其提交与互评记录，确定继续吗？", "删除作业");
        if (!confirmed) {
            return;
        }
        try {
            context.assignments().delete(teacher, selectedAssignment.getId());
            reloadAssignments();
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
        }
    }

    // ———————————————————— 按钮状态 ————————————————————

    private void updateCourseButtons() {
        boolean has = selectedCourse != null;
        editCourseButton.setEnabled(has);
        deleteCourseButton.setEnabled(has);
        createAssignmentButton.setEnabled(has);
    }

    private void updateAssignmentButtons() {
        boolean has = selectedAssignment != null;
        editAssignmentButton.setEnabled(has);
        allocateButton.setEnabled(has);
        deleteAssignmentButton.setEnabled(has);
        statusButton.setEnabled(has);
        if (!has) {
            statusButton.setText("发布作业");
            return;
        }
        statusButton.setText(switch (selectedAssignment.getStatus()) {
            case DRAFT -> "发布作业";
            case PUBLISHED, ALLOCATED -> "结束作业";
            case CLOSED -> "已结束";
        });
        statusButton.setEnabled(selectedAssignment.getStatus() != AssignmentStatus.CLOSED);
    }

    // ———————————————————— Page ————————————————————

    @Override
    public String navLabel() {
        return "课程与作业";
    }

    @Override
    public Icon navIcon() {
        return Icons.book(17, Theme.NAV_ICON);
    }

    @Override
    public String title() {
        return "课程与作业管理";
    }

    @Override
    public String subtitle() {
        return "维护课程与作业，配置多维度带权重的评分细则";
    }

    @Override
    public JComponent view() {
        return view;
    }

    /** 单个课程列表项：标题 + 编码学期，选中时高亮圆角底。 */
    private static final class CourseCell extends JPanel implements ListCellRenderer<Course> {

        private final JLabel title = new JLabel();
        private final JLabel meta = new JLabel();
        private boolean selected;

        CourseCell() {
            setLayout(new java.awt.GridLayout(2, 1, 0, 2));
            setBorder(new EmptyBorder(9, 14, 9, 12));
            setOpaque(false);
            title.setFont(Theme.bold(13));
            title.setForeground(Theme.TEXT);
            meta.setFont(Theme.plain(11));
            meta.setForeground(Theme.MUTED);
            add(title);
            add(meta);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Course> list, Course value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            title.setText(value == null ? "" : value.title());
            meta.setText(value == null ? "" : value.subtitle());
            this.selected = isSelected;
            title.setForeground(Theme.TEXT);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (selected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PRIMARY_SOFT);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(Theme.PRIMARY);
                g2.fillRoundRect(0, getHeight() / 2 - 10, 3, 20, 3, 3);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
