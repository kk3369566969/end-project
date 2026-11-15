package com.peerreview.ui.page;

import com.peerreview.AppContext;
import com.peerreview.common.Dates;
import com.peerreview.common.Text;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.AssignmentStatus;
import com.peerreview.domain.RubricItem;
import com.peerreview.domain.Submission;
import com.peerreview.domain.User;
import com.peerreview.service.SubmissionService;
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
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生端「我的作业」页：左侧作业列表，右侧查看要求、评分细则并提交正文。
 */
public final class StudentAssignmentPage implements Page {

    private final AppContext context;
    private final User student;

    private final JPanel view = new JPanel(new BorderLayout(16, 0));
    private final DefaultListModel<Assignment> assignmentModel = new DefaultListModel<>();
    private final JList<Assignment> assignmentList = new JList<>(assignmentModel);

    private final JLabel titleLabel = Ui.h1("选择一个作业");
    private final JLabel metaLabel = Ui.muted("从左侧选择作业查看要求并提交");
    private final JPanel statusHolder = Ui.row(0);
    private final JPanel titleRow = Ui.row(10);

    private final JTextArea descriptionArea = Ui.readOnlyArea(5);
    private final SimpleTableModel<RubricItem> rubricModel;
    private final JTable rubricTable;
    private final JTextArea contentArea = Ui.textArea(10);
    private final JLabel counterLabel = Ui.muted("0 字");
    private final JLabel submitStateLabel = Ui.muted("");
    private final FlatButton submitButton = Ui.primary("提交作业");

    private final JPanel detailContainer = new JPanel(new BorderLayout());
    private final Card detailCard;

    private Assignment selected;
    private Submission mySubmission;

    public StudentAssignmentPage(AppContext context, User student) {
        this.context = context;
        this.student = student;

        rubricModel = new SimpleTableModel<>(List.of(
                new SimpleTableModel.Column<>("评分维度", 220, RubricItem::getName),
                new SimpleTableModel.Column<>("权重", 80, RubricItem::weightLabel),
                new SimpleTableModel.Column<>("分值区间", 140, RubricItem::rangeLabel)));
        rubricTable = Ui.table(rubricModel);

        detailCard = buildDetailCard();

        view.setOpaque(false);
        view.add(buildListCard(), BorderLayout.WEST);
        detailContainer.setOpaque(false);
        view.add(detailContainer, BorderLayout.CENTER);

        showEmptyDetail();
        wireEvents();
    }

    // ———————————————————— 左侧列表 ————————————————————

    private JComponent buildListCard() {
        Card card = Ui.card(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(300, 0));

        JPanel header = Ui.row(10);
        header.add(Ui.h2("作业列表"));
        header.add(Ui.stretch());

        assignmentList.setOpaque(false);
        assignmentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assignmentList.setFixedCellHeight(66);
        assignmentList.setCellRenderer(new AssignmentCell());

        JScrollPane scroll = Ui.scroll(assignmentList);
        scroll.getViewport().setOpaque(false);

        card.add(header, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ———————————————————— 右侧详情 ————————————————————

    private Card buildDetailCard() {
        Card card = Ui.card(new BorderLayout(0, 14));

        titleRow.add(titleLabel);
        titleRow.add(statusHolder);
        titleRow.add(Ui.stretch());
        JPanel header = Ui.column(6);
        header.add(titleRow);
        header.add(metaLabel);

        JPanel descriptionBlock = Ui.column(6);
        descriptionBlock.add(Ui.boldLabel("作业要求"));
        JScrollPane descriptionScroll = Ui.scroll(descriptionArea);
        descriptionScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        descriptionScroll.setPreferredSize(new Dimension(0, 120));
        descriptionBlock.add(descriptionScroll);

        JPanel rubricBlock = Ui.column(6);
        rubricBlock.add(Ui.boldLabel("评分细则"));
        JScrollPane rubricScroll = Ui.tableScroll(rubricTable);
        rubricScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        rubricScroll.setPreferredSize(new Dimension(0, 168));
        rubricBlock.add(rubricScroll);

        JPanel top = Ui.column(12);
        top.add(header);
        top.add(descriptionBlock);
        top.add(rubricBlock);

        JPanel contentBlock = Ui.column(6);
        JPanel contentHeader = Ui.row(10);
        contentHeader.add(Ui.boldLabel("我的提交"));
        contentHeader.add(Ui.stretch());
        contentHeader.add(counterLabel);
        contentBlock.add(contentHeader);
        JScrollPane contentScroll = Ui.scroll(contentArea);
        contentScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        contentBlock.add(contentScroll);

        JPanel footer = Ui.row(12);
        submitStateLabel.setFont(Theme.plain(12));
        footer.add(submitStateLabel);
        footer.add(Ui.stretch());
        footer.add(submitButton);

        card.add(top, BorderLayout.NORTH);
        card.add(contentBlock, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private void wireEvents() {
        assignmentList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelection();
            }
        });

        contentArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateCounter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateCounter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateCounter();
            }
        });

        submitButton.addActionListener(event -> submit());
        updateCounter();
    }

    // ———————————————————— 数据 ————————————————————

    @Override
    public void onShow() {
        Long keepId = selected == null ? null : selected.getId();
        List<Assignment> assignments = new ArrayList<>();
        try {
            for (Assignment assignment : context.assignments().listAll()) {
                if (assignment.getStatus() != AssignmentStatus.DRAFT) {
                    assignments.add(assignment);
                }
            }
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
            return;
        }
        assignmentModel.clear();
        assignments.forEach(assignmentModel::addElement);

        if (assignments.isEmpty()) {
            showEmptyDetail();
            return;
        }
        int target = 0;
        if (keepId != null) {
            for (int i = 0; i < assignments.size(); i++) {
                if (assignments.get(i).getId() == keepId) {
                    target = i;
                    break;
                }
            }
        }
        assignmentList.setSelectedIndex(target);
        assignmentList.ensureIndexIsVisible(target);
        loadSelection();
    }

    private void loadSelection() {
        Assignment picked = assignmentList.getSelectedValue();
        if (picked == null) {
            showEmptyDetail();
            return;
        }
        try {
            selected = context.assignments().require(picked.getId());
            mySubmission = context.submissions().findMine(selected.getId(), student.getId()).orElse(null);
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
            return;
        }
        detailContainer.removeAll();
        detailContainer.add(detailCard, BorderLayout.CENTER);
        detailContainer.revalidate();
        detailContainer.repaint();
        renderDetail();
    }

    private void showEmptyDetail() {
        detailContainer.removeAll();
        detailContainer.add(Ui.emptyState("暂无作业", "教师发布作业后会出现在左侧列表"), BorderLayout.CENTER);
        detailContainer.revalidate();
        detailContainer.repaint();
    }

    private void renderDetail() {
        if (selected == null) {
            return;
        }
        titleLabel.setText(selected.getTitle());
        statusHolder.removeAll();
        statusHolder.add(Badges.assignment(selected.getStatus()));
        statusHolder.revalidate();
        statusHolder.repaint();

        String meta = selected.getCourseName() + " · 截止 " + Dates.format(selected.getDeadline());
        metaLabel.setText(meta);
        metaLabel.setToolTipText(meta + " · 每份作业 " + selected.getReviewersPerSubmission() + " 人评审"
                + (selected.isAnonymous() ? " · 匿名互评" : " · 实名互评"));
        descriptionArea.setText(selected.getDescription());
        descriptionArea.setCaretPosition(0);
        rubricModel.setRows(selected.getRubricItems());

        boolean editable = selected.getStatus().acceptsSubmission() && !selected.isOverdue();
        contentArea.setEditable(editable);
        contentArea.setBackground(editable ? Color.WHITE : new Color(0xFBFCFE));
        contentArea.setText(mySubmission == null ? "" : mySubmission.getContent());
        contentArea.setCaretPosition(0);
        submitButton.setEnabled(editable);
        submitButton.setText(mySubmission == null ? "提交作业" : "更新提交");

        if (mySubmission == null) {
            submitStateLabel.setForeground(Theme.MUTED);
            submitStateLabel.setText(editable ? "尚未提交，请在下方填写作业内容" : "当前状态不可提交");
        } else {
            submitStateLabel.setForeground(Theme.SUCCESS);
            submitStateLabel.setText("已于 " + Dates.format(mySubmission.getSubmittedAt()) + " 提交"
                    + (mySubmission.isEdited() ? "，最近更新 " + Dates.format(mySubmission.getUpdatedAt()) : ""));
        }
        if (selected.getStatus() == AssignmentStatus.PUBLISHED && selected.isOverdue()) {
            submitStateLabel.setForeground(Theme.DANGER);
            submitStateLabel.setText("已超过截止时间，无法再提交或修改");
        }
        if (selected.getStatus() == AssignmentStatus.ALLOCATED) {
            submitStateLabel.setForeground(Theme.WARNING);
            submitStateLabel.setText("已进入互评阶段，提交内容已锁定"
                    + (mySubmission == null ? "（你没有提交本次作业）" : ""));
        }
        updateCounter();
    }

    private void updateCounter() {
        int chars = Text.countVisibleChars(contentArea.getText());
        counterLabel.setText(chars + " 字");
        counterLabel.setForeground(chars >= SubmissionService.MIN_CONTENT_LENGTH ? Theme.SUCCESS : Theme.MUTED);
    }

    private void submit() {
        if (selected == null) {
            return;
        }
        try {
            mySubmission = context.submissions().submit(student, selected.getId(), contentArea.getText());
            Dialogs.success(view, "提交成功！\n提交时间：" + Dates.format(mySubmission.getSubmittedAt()));
            renderDetail();
        } catch (RuntimeException ex) {
            Dialogs.error(view, ex);
        }
    }

    // ———————————————————— Page ————————————————————

    @Override
    public String navLabel() {
        return "我的作业";
    }

    @Override
    public Icon navIcon() {
        return Icons.upload(17, Theme.NAV_ICON);
    }

    @Override
    public String title() {
        return "我的作业";
    }

    @Override
    public String subtitle() {
        return "查看作业要求与评分细则，提交或更新作业正文";
    }

    @Override
    public JComponent view() {
        return view;
    }

    /** 作业列表项：标题 + 课程/状态，选中时高亮。 */
    private static final class AssignmentCell extends JPanel implements ListCellRenderer<Assignment> {

        private final JLabel title = new JLabel();
        private final JLabel meta = new JLabel();
        private boolean selected;

        AssignmentCell() {
            setLayout(new BorderLayout(8, 2));
            setBorder(new EmptyBorder(10, 14, 10, 12));
            setOpaque(false);
            title.setFont(Theme.bold(13));
            title.setForeground(Theme.TEXT);
            meta.setFont(Theme.plain(11));
            meta.setForeground(Theme.MUTED);

            JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
            text.setOpaque(false);
            text.add(title);
            text.add(meta);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Assignment> list, Assignment value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            selected = isSelected;
            title.setText(value == null ? "" : value.getTitle());
            meta.setText(value == null ? "" : value.getCourseName() + " · " + value.getStatus().label()
                    + " · " + Dates.format(value.getDeadline()));
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
                g2.fillRoundRect(0, getHeight() / 2 - 12, 3, 24, 3, 3);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
