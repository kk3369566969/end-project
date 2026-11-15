package com.peerreview.ui.page;

import com.peerreview.ai.AiAdoption;
import com.peerreview.ai.ReviewSuggestion;
import com.peerreview.ai.ReviewSuggestionProvider;
import com.peerreview.ai.SuggestionRequest;
import com.peerreview.domain.RubricItem;
import com.peerreview.ui.Theme;
import com.peerreview.ui.Ui;
import com.peerreview.ui.component.Badge;
import com.peerreview.ui.component.FlatButton;
import com.peerreview.ui.component.StackPanel;
import com.peerreview.ui.model.SimpleTableModel;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;

/**
 * AI 参考意见窗口：把「请求 → 等待 → 结果 / 失败」三态做成一个明确的流程。
 *
 * <p>三条设计决定：</p>
 * <ol>
 *   <li><b>不阻塞界面线程</b>：网络请求放在 {@link SwingWorker}，等待期间显示进度条，
 *       点击关闭会立刻置位 {@code closed}，迟到的结果不再回写已被销毁的界面。</li>
 *   <li><b>采纳是显式动作</b>：AI 从不直接改打分窗口的分数。学生点「采纳」后，
 *       内容才被回填到输入框，而且回填后的值仍可手动修改、仍需再次点「提交互评」才落库。</li>
 *   <li><b>总评可编辑</b>：展示的是草稿而不是结论，学生可以直接在这里润色再采纳。</li>
 * </ol>
 */
public final class AiAssistDialog extends JDialog {

    private static final int DIALOG_WIDTH = 780;
    private static final int DIALOG_HEIGHT = 648;
    private static final int HINT_TABLE_HEIGHT = 172;

    private static final String CARD_LOADING = "loading";
    private static final String CARD_RESULT = "result";
    private static final String CARD_ERROR = "error";

    private static final String HINT_CARD_TABLE = "table";
    private static final String HINT_CARD_EMPTY = "empty";

    private final ReviewSuggestionProvider provider;
    private final SuggestionRequest request;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final StackPanel actions = Ui.row(8);
    private final JLabel metaLabel = Ui.hint(" ");

    private final SimpleTableModel<ReviewSuggestion.DimensionHint> hintModel;
    private final JTable hintTable;
    private final JScrollPane hintScroll;
    private final CardLayout hintCards = new CardLayout();
    private final JPanel hintHolder = Ui.transparent(hintCards);
    private final JLabel hintCountLabel = Ui.hint(" ");
    private final JTextArea commentArea = Ui.textArea(6);
    private final JLabel errorLabel = new JLabel();

    private ReviewSuggestion latest;
    private AiAdoption adoption;
    private boolean closed;

    private AiAssistDialog(Window owner, ReviewSuggestionProvider provider, SuggestionRequest request) {
        super(owner, "AI 参考意见 · " + request.assignmentTitle(), ModalityType.APPLICATION_MODAL);
        this.provider = provider;
        this.request = request;

        this.hintModel = new SimpleTableModel<>(List.of(
                new SimpleTableModel.Column<>("评分维度", 190, ReviewSuggestion.DimensionHint::dimension),
                new SimpleTableModel.Column<>("建议分", 90,
                        hint -> RubricItem.trim(hint.score())),
                new SimpleTableModel.Column<>("判断依据", 400, ReviewSuggestion.DimensionHint::reason)));
        this.hintTable = Ui.table(hintModel);
        this.hintScroll = Ui.tableScroll(hintTable);

        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCards(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        setSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
        setLocationRelativeTo(owner);

        generate();
    }

    /** 打开窗口并等待用户决定；返回空表示用户放弃采纳。 */
    public static Optional<AiAdoption> show(Component parent, ReviewSuggestionProvider provider,
                                            SuggestionRequest request) {
        // parent 本身可能就是打分窗口（JDialog 也是 Window），此时直接拿它当 owner，
        // 否则 getWindowAncestor 会返回 null，弹窗就只能居中于屏幕。
        Window owner = parent instanceof Window window
                ? window
                : SwingUtilities.getWindowAncestor(parent);
        AiAssistDialog dialog = new AiAssistDialog(owner, provider, request);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.adoption);
    }

    // ———————————————————— 结构 ————————————————————

    private JComponent buildHeader() {
        StackPanel header = Ui.column(5);
        header.setOpaque(true);
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(18, 24, 14, 24)));

        StackPanel row = Ui.row(10);
        row.add(Ui.h1("AI 参考意见"));
        row.add(new Badge("草稿", Theme.PRIMARY, Theme.PRIMARY_SOFT));
        row.add(Ui.stretch());
        row.add(Ui.hint(provider.displayName()));

        header.add(row);
        header.add(Ui.muted("模型只负责阅读正文与比对细则，分数与评语的采纳与否由你决定。"));
        return header;
    }

    private JComponent buildCards() {
        cards.setBackground(Theme.SURFACE);
        cards.setBorder(new EmptyBorder(16, 24, 16, 24));
        cards.add(buildLoadingCard(), CARD_LOADING);
        cards.add(buildResultCard(), CARD_RESULT);
        cards.add(buildErrorCard(), CARD_ERROR);
        return cards;
    }

    private JComponent buildLoadingCard() {
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setBorderPainted(false);
        bar.setForeground(Theme.PRIMARY);
        bar.setPreferredSize(new Dimension(260, 6));

        JPanel panel = Ui.transparent(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 16, 0);
        panel.add(Ui.h2("正在生成参考意见…"), constraints);

        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 16, 0);
        panel.add(bar, constraints);

        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(Ui.muted("正在阅读正文并比对 " + request.rubricItems().size() + " 条评分细则，通常需要 2~10 秒"),
                constraints);
        return panel;
    }

    private JComponent buildResultCard() {
        JPanel panel = Ui.transparent(new BorderLayout(0, 14));
        panel.add(buildHintSection(), BorderLayout.NORTH);
        panel.add(buildCommentSection(), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildHintSection() {
        StackPanel header = Ui.row(8);
        header.add(Ui.boldLabel("逐维度建议"));
        header.add(Ui.stretch());
        header.add(hintCountLabel);

        hintScroll.setPreferredSize(new Dimension(0, HINT_TABLE_HEIGHT));
        hintScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, HINT_TABLE_HEIGHT));

        // 用 CardLayout 而不是把两个组件叠在同一个 BorderLayout 区域：
        // BorderLayout 每个区域只认最后添加的那个，会直接把表格挤成 0 高。
        hintHolder.setPreferredSize(new Dimension(0, HINT_TABLE_HEIGHT));
        hintHolder.add(hintScroll, HINT_CARD_TABLE);
        hintHolder.add(emptyHintPanel(), HINT_CARD_EMPTY);

        StackPanel section = Ui.column(8);
        section.add(header);
        section.add(hintHolder);
        return section;
    }

    /** 没有逐维度建议时的占位：与表格同高，避免上下两块布局跳动。 */
    private JComponent emptyHintPanel() {
        JPanel panel = Ui.transparent(new GridBagLayout());
        panel.add(Ui.hint("本次未返回逐维度建议，仅生成了总评"));
        return panel;
    }

    private JComponent buildCommentSection() {
        StackPanel header = Ui.row(8);
        header.add(Ui.boldLabel("总评草稿"));
        header.add(Ui.stretch());
        header.add(Ui.hint("可直接修改后再采纳"));

        JScrollPane scroll = Ui.scroll(commentArea);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JPanel section = Ui.transparent(new BorderLayout(0, 8));
        section.add(header, BorderLayout.NORTH);
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    private JComponent buildErrorCard() {
        errorLabel.setFont(Theme.plain(13));
        errorLabel.setForeground(Theme.DANGER);

        JPanel panel = Ui.transparent(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 12, 0);
        panel.add(Ui.h2("这次没能生成"), constraints);

        constraints.gridy = 1;
        panel.add(errorLabel, constraints);

        constraints.gridy = 2;
        constraints.insets = new Insets(16, 0, 0, 0);
        panel.add(Ui.muted("可以点右下角「重试」，或检查网络连接与 ai.api.key 配置"), constraints);
        return panel;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.SURFACE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(12, 24, 16, 24)));
        footer.add(metaLabel, BorderLayout.WEST);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    // ———————————————————— 状态流转 ————————————————————

    private void generate() {
        cardLayout.show(cards, CARD_LOADING);
        metaLabel.setText("正在调用 " + provider.displayName() + "…");
        replaceActions(List.of(cancelButton()));

        new SwingWorker<ReviewSuggestion, Void>() {
            @Override
            protected ReviewSuggestion doInBackground() {
                return provider.suggest(request);
            }

            @Override
            protected void done() {
                // 用户可能已经关掉窗口：迟到的结果不再触碰组件
                if (closed) {
                    return;
                }
                try {
                    showResult(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showError(cause.getMessage() == null ? "未知错误，请稍后重试" : cause.getMessage());
                }
            }
        }.execute();
    }

    private void showResult(ReviewSuggestion suggestion) {
        this.latest = suggestion;
        hintModel.setRows(suggestion.hints());
        commentArea.setText(suggestion.comment());
        commentArea.setCaretPosition(0);

        boolean hasHints = !suggestion.hints().isEmpty();
        hintCards.show(hintHolder, hasHints ? HINT_CARD_TABLE : HINT_CARD_EMPTY);
        hintCountLabel.setText(hasHints ? "共 " + suggestion.hints().size() + " 条" : " ");

        metaLabel.setText("模型 " + suggestion.model()
                + " · 耗时 " + suggestion.elapsedLabel()
                + " · 约 " + suggestion.totalTokens() + " tokens");
        cardLayout.show(cards, CARD_RESULT);
        installAdoptActions(suggestion);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        metaLabel.setText("生成失败");
        cardLayout.show(cards, CARD_ERROR);

        FlatButton cancel = cancelButton();
        FlatButton retry = Ui.primary("重试");
        retry.addActionListener(event -> generate());
        replaceActions(List.of(cancel, retry));
        getRootPane().setDefaultButton(retry);
    }

    private void installAdoptActions(ReviewSuggestion suggestion) {
        List<JComponent> buttons = new ArrayList<>();
        FlatButton regenerate = Ui.ghost("重新生成");
        regenerate.addActionListener(event -> generate());
        buttons.add(regenerate);
        buttons.add(cancelButton());

        boolean hasHints = !suggestion.hints().isEmpty();
        if (hasHints) {
            FlatButton commentOnly = Ui.secondary("仅采纳评语");
            commentOnly.addActionListener(event -> adopt(Map.of()));
            buttons.add(commentOnly);
        }

        FlatButton adoptAll = Ui.primary(hasHints ? "采纳建议分与评语" : "采纳评语");
        adoptAll.addActionListener(event -> adopt(collectedScores()));
        buttons.add(adoptAll);

        replaceActions(buttons);
        getRootPane().setDefaultButton(adoptAll);
    }

    private FlatButton cancelButton() {
        FlatButton cancel = Ui.secondary("取消");
        cancel.addActionListener(event -> dispose());
        return cancel;
    }

    private void replaceActions(List<JComponent> buttons) {
        actions.removeAll();
        buttons.forEach(actions::add);
        actions.revalidate();
        actions.repaint();
    }

    // ———————————————————— 采纳 ————————————————————

    private void adopt(Map<Long, Double> scores) {
        this.adoption = new AiAdoption(scores, commentArea.getText());
        dispose();
    }

    /** 把「维度名」翻译回「维度 ID」，只采纳能对上细则的条目。 */
    private Map<Long, Double> collectedScores() {
        Map<Long, Double> scores = new LinkedHashMap<>();
        if (latest == null) {
            return scores;
        }
        for (ReviewSuggestion.DimensionHint hint : latest.hints()) {
            rubricIdOf(hint.dimension()).ifPresent(id -> scores.put(id, hint.score()));
        }
        return scores;
    }

    private OptionalLong rubricIdOf(String dimension) {
        String target = dimension == null ? "" : dimension.trim();
        for (RubricItem item : request.rubricItems()) {
            if (item.getName().trim().equals(target)) {
                return OptionalLong.of(item.getId());
            }
        }
        return OptionalLong.empty();
    }

    @Override
    public void dispose() {
        closed = true;
        super.dispose();
    }
}
