package com.peerreview.ui.page;

import com.peerreview.AppContext;
import com.peerreview.ai.AiAdoption;
import com.peerreview.ai.SuggestionRequest;
import com.peerreview.common.Text;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.Review;
import com.peerreview.domain.ReviewScore;
import com.peerreview.domain.RubricItem;
import com.peerreview.domain.ScoringMode;
import com.peerreview.domain.TaskRow;
import com.peerreview.domain.User;
import com.peerreview.ui.Dialogs;
import com.peerreview.ui.Icons;
import com.peerreview.ui.Theme;
import com.peerreview.ui.Ui;
import com.peerreview.ui.component.Badge;
import com.peerreview.ui.component.FlatButton;
import com.peerreview.ui.component.StackPanel;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 互评打分对话框：左侧展示（匿名的）作业正文，右侧逐维度打分并实时显示加权总分。
 *
 * <p>总分预览与最终落库使用同一套 {@code ScoreCalculator}，保证「看到的就是提交的」。</p>
 *
 * <p>布局要点：整个窗口用 {@link BorderLayout} 切成「页头 / 主体 / 页脚」，
 * 主体再切「正文（自适应宽） + 打分列（固定宽）」。打分列内部同样用 BorderLayout
 * 而不是 BoxLayout——BoxLayout 会把多余高度摊给每个子组件，导致输入框被拉高变形。</p>
 */
public final class ReviewDialog extends JDialog {

    /** 右侧打分列固定宽度，保证不同作业之间视觉一致。 */
    private static final int SCORE_COLUMN_WIDTH = 430;
    /** 单行评分控件的统一高度。 */
    private static final int FIELD_HEIGHT = 32;

    private final AppContext context;
    private final User reviewer;
    private final TaskRow task;
    private final Assignment assignment;
    private final Optional<Review> existingReview;

    private final Map<Long, JSpinner> spinners = new LinkedHashMap<>();
    private final JLabel totalLabel = new JLabel("0");
    private final JTextArea commentArea = Ui.textArea(5);
    private boolean submitted;

    private ReviewDialog(Window owner, AppContext context, User reviewer, TaskRow task,
                         Assignment assignment, Optional<Review> existingReview) {
        super(owner, "互评打分 · " + assignment.getTitle(), ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.reviewer = reviewer;
        this.task = task;
        this.assignment = assignment;
        this.existingReview = existingReview;

        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        setSize(new Dimension(1000, 820));
        setLocationRelativeTo(owner);

        refreshTotal();
    }

    /** 打开打分窗口；返回 true 表示本次会话内成功提交了互评。 */
    public static boolean show(Component parent, AppContext context, User reviewer,
                               TaskRow task, Assignment assignment, Optional<Review> existingReview) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        ReviewDialog dialog = new ReviewDialog(owner, context, reviewer, task, assignment, existingReview);
        dialog.setVisible(true);
        return dialog.submitted;
    }

    // ———————————————————— 结构 ————————————————————

    private JPanel buildHeader() {
        StackPanel header = Ui.column(6);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(18, 24, 16, 24)));

        StackPanel row = Ui.row(10);
        row.add(Ui.h1(assignment.getTitle()));
        row.add(new Badge(assignment.isAnonymous() ? "盲评" : "实名",
                assignment.isAnonymous() ? Theme.PRIMARY : Theme.MUTED,
                assignment.isAnonymous() ? Theme.PRIMARY_SOFT : Theme.NEUTRAL_SOFT));
        row.add(Ui.stretch());

        String author = assignment.isAnonymous() ? "匿名同学" : task.authorLabel();
        header.add(row);
        header.add(Ui.muted(task.courseName() + " · " + author
                + " · 计分方式：" + assignment.getScoringMode().label()));
        return header;
    }

    private JComponent buildBody() {
        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setBackground(Theme.SURFACE);
        body.setBorder(new EmptyBorder(16, 24, 14, 24));
        body.add(buildContentPanel(), BorderLayout.CENTER);
        body.add(buildScoringPanel(), BorderLayout.EAST);
        return body;
    }

    /** 左侧：标题固定在顶部，正文占据剩余高度并自行滚动。 */
    private JComponent buildContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        StackPanel header = Ui.row(8);
        header.add(Ui.boldLabel("作业正文"));
        header.add(Ui.stretch());
        header.add(Ui.hint(Text.countVisibleChars(task.content()) + " 字"));

        JTextArea content = Ui.readOnlyArea(12);
        content.setText(task.content());
        content.setCaretPosition(0);
        JScrollPane scroll = Ui.scroll(content);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /** 右侧：上半是逐维度打分（高度随细则条数而定），下半评语框吃掉剩余高度。 */
    private JComponent buildScoringPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(SCORE_COLUMN_WIDTH, 0));

        panel.add(buildScoreFields(), BorderLayout.NORTH);
        panel.add(buildCommentBox(), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildScoreFields() {
        StackPanel fields = Ui.column(8);
        fields.add(Ui.boldLabel("逐维度打分"));

        List<RubricItem> items = assignment.getRubricItems();
        for (int index = 0; index < items.size(); index++) {
            if (index > 0) {
                fields.add(Ui.divider());
            }
            fields.add(buildRubricRow(items.get(index)));
        }
        return fields;
    }

    private JComponent buildCommentBox() {
        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setOpaque(false);

        StackPanel text = Ui.column(4);
        text.add(Ui.boldLabel("人工评语"));
        text.add(Ui.hint(existingReview.isPresent()
                ? "历史评语（只读）"
                : "请说明打分依据，不少于 10 个字符"));

        StackPanel header = Ui.row(8);
        header.add(text);
        header.add(Ui.stretch());
        if (aiAvailable()) {
            header.add(buildAiButton());
        }

        JScrollPane commentScroll = Ui.scroll(commentArea);
        commentScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        box.add(header, BorderLayout.NORTH);
        box.add(commentScroll, BorderLayout.CENTER);
        return box;
    }

    /** 只有「还能打分」且「AI 已配置」时才出现入口，避免给用户一个点了就报错的按钮。 */
    private boolean aiAvailable() {
        return existingReview.isEmpty() && context.ai().isAvailable();
    }

    private FlatButton buildAiButton() {
        FlatButton button = Ui.secondary("AI 参考意见").compact()
                .withIcon(Icons.star(14, Theme.PRIMARY));
        button.setToolTipText("让 " + context.ai().displayName() + " 先起草一份建议，供你参考");
        button.addActionListener(event -> openAiAssistant());
        return button;
    }

    /** 单行评分：维度名 / 分值区间 / 输入框 / 权重，四列固定宽度，避免挤压与错位。 */
    private JComponent buildRubricRow(RubricItem item) {
        StackPanel row = Ui.row(10);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));

        JLabel name = new JLabel(item.getName());
        name.setFont(Theme.bold(13));
        name.setForeground(Theme.TEXT);
        name.setPreferredSize(new Dimension(116, FIELD_HEIGHT));

        JLabel range = Ui.hint(item.rangeLabel());
        range.setPreferredSize(new Dimension(96, FIELD_HEIGHT));

        JLabel weight = Ui.muted(item.weightLabel());
        weight.setPreferredSize(new Dimension(70, FIELD_HEIGHT));

        row.add(name);
        row.add(range);
        row.add(buildScoreInput(item));
        row.add(weight);
        row.add(Ui.stretch());
        return row;
    }

    /** 可编辑时给 JSpinner，历史评语则退化为一个只读分数标签。 */
    private JComponent buildScoreInput(RubricItem item) {
        Dimension size = new Dimension(78, FIELD_HEIGHT);
        if (existingReview.isPresent()) {
            JLabel score = new JLabel(existingScore(item));
            score.setFont(Theme.bold(15));
            score.setForeground(Theme.PRIMARY);
            score.setPreferredSize(size);
            return score;
        }
        JSpinner spinner = Ui.spinner(item.getMinScore(), item.getMaxScore(), 0.5, existingOrDefault(item));
        spinner.setPreferredSize(size);
        spinner.setMinimumSize(size);
        spinner.setMaximumSize(size);
        spinner.addChangeListener(event -> refreshTotal());
        spinners.put(item.getId(), spinner);
        return spinner;
    }

    private String existingScore(RubricItem item) {
        return existingReview.map(review -> review.getScores().stream()
                        .filter(score -> score.getRubricItemId() == item.getId())
                        .map(ReviewScore::getScore)
                        .map(RubricItem::trim)
                        .findFirst()
                        .orElse("—"))
                .orElse("—");
    }

    private double existingOrDefault(RubricItem item) {
        return existingReview
                .flatMap(review -> review.getScores().stream()
                        .filter(score -> score.getRubricItemId() == item.getId())
                        .map(ReviewScore::getScore)
                        .findFirst())
                .orElse((item.getMinScore() + item.getMaxScore()) / 2);
    }

    /** 直接加权模式下总分不是百分制，按「各维度最高分 × 权重」换算出实际满分。 */
    private double totalPossible() {
        return assignment.getRubricItems().stream()
                .mapToDouble(item -> item.getMaxScore() * item.getWeight() / 100.0)
                .sum();
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.SURFACE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(12, 24, 16, 24)));

        StackPanel totalBox = Ui.row(8);
        totalLabel.setFont(new Font(Theme.bold(22).getName(), Font.BOLD, 22));
        totalLabel.setForeground(Theme.PRIMARY);
        totalBox.add(Ui.muted("加权总分"));
        totalBox.add(totalLabel);
        totalBox.add(Ui.muted(assignment.getScoringMode() == ScoringMode.NORMALIZED
                ? "/ 100"
                : "分（满分 " + RubricItem.trim(totalPossible()) + "）"));

        StackPanel buttons = Ui.row(10);
        if (existingReview.isPresent()) {
            commentArea.setEditable(false);
            commentArea.setBackground(new java.awt.Color(0xFBFCFE));
            commentArea.setText(existingReview.get().getComment());
            FlatButton close = Ui.secondary("关闭");
            close.addActionListener(event -> dispose());
            buttons.add(close);
        } else {
            FlatButton cancel = Ui.secondary("取消");
            cancel.addActionListener(event -> dispose());
            FlatButton submit = Ui.primary("提交互评");
            submit.addActionListener(event -> submit());
            buttons.add(cancel);
            buttons.add(submit);
            getRootPane().setDefaultButton(submit);
        }
        footer.add(totalBox, BorderLayout.WEST);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    // ———————————————————— 交互 ————————————————————

    private void refreshTotal() {
        if (existingReview.isPresent()) {
            totalLabel.setText(RubricItem.trim(existingReview.get().getTotalScore()));
            return;
        }
        double total = context.reviews().previewTotal(assignment, collectScores());
        totalLabel.setText(RubricItem.trim(total));
    }

    private Map<Long, Double> collectScores() {
        Map<Long, Double> scores = new LinkedHashMap<>();
        spinners.forEach((itemId, spinner) ->
                scores.put(itemId, ((Number) spinner.getValue()).doubleValue()));
        return scores;
    }

    // ———————————————————— AI 辅助 ————————————————————

    private void openAiAssistant() {
        SuggestionRequest request = new SuggestionRequest(
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getRubricItems(),
                task.content());
        AiAssistDialog.show(this, context.ai(), request).ifPresent(this::applyAdoption);
    }

    /** 采纳 AI 草稿：只负责填控件，不负责提交——学生仍要自己核对再点「提交互评」。 */
    private void applyAdoption(AiAdoption adoption) {
        if (adoption.hasComment()) {
            commentArea.setText(adoption.comment());
            commentArea.setCaretPosition(0);
        }
        adoption.scores().forEach(this::applyScore);
        refreshTotal();
    }

    private void applyScore(long rubricItemId, double score) {
        JSpinner spinner = spinners.get(rubricItemId);
        if (spinner == null) {
            return;
        }
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
        double min = ((Number) model.getMinimum()).doubleValue();
        double max = ((Number) model.getMaximum()).doubleValue();
        spinner.setValue(Math.max(min, Math.min(max, score)));
    }

    private void submit() {
        try {
            context.reviews().submit(reviewer, task.taskId(), collectScores(), commentArea.getText());
            submitted = true;
            Dialogs.success(this, "互评已提交，感谢你的认真评审！");
            dispose();
        } catch (RuntimeException ex) {
            Dialogs.error(this, ex);
        }
    }

    /** 供调用方在关闭后判断是否需要刷新列表。 */
    public boolean submitted() {
        return submitted;
    }
}
