package com.peerreview.ui;

import com.peerreview.AppContext;
import com.peerreview.domain.User;
import com.peerreview.ui.component.FlatButton;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 登录窗口：左侧品牌区、右侧登录卡片。
 *
 * <p>界面只负责采集输入并把结果交给 {@code AuthService}；校验规则与失败提示都在服务层。</p>
 */
public final class LoginFrame extends JFrame {

    private final AppContext context;
    private final JTextField usernameField = Ui.field("请输入学号或工号", 18);
    private final JPasswordField passwordField = Ui.passwordField("请输入密码", 18);
    private final JLabel errorLabel = new JLabel(" ");
    private final FlatButton loginButton = Ui.primary("登 录");

    public LoginFrame(AppContext context) {
        super("同伴互评 · 登录");
        this.context = context;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 640);
        setMinimumSize(new Dimension(920, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.SURFACE);

        add(new BrandPanel(), BorderLayout.WEST);
        add(buildFormPanel(), BorderLayout.CENTER);

        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(event -> doLogin());
    }

    private JPanel buildFormPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.SURFACE);

        JPanel card = Ui.column(0);

        JLabel title = new JLabel("欢迎回来");
        title.setFont(Theme.bold(24));
        title.setForeground(Theme.TEXT);

        JLabel subtitle = Ui.muted("登录后即可提交作业、完成同伴互评");
        subtitle.setFont(Theme.plain(13));

        errorLabel.setFont(Theme.plain(12));
        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setPreferredSize(new Dimension(380, 18));

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(fieldLabel("账号"));
        card.add(Box.createVerticalStrut(8));
        stretch(usernameField);
        card.add(usernameField);
        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("密码"));
        card.add(Box.createVerticalStrut(8));
        stretch(passwordField);
        card.add(passwordField);
        card.add(Box.createVerticalStrut(10));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(6));
        stretch(loginButton);
        loginButton.setPreferredSize(new Dimension(380, 44));
        card.add(loginButton);

        card.add(Box.createVerticalStrut(18));
        card.add(registerRow());

        card.add(Box.createVerticalStrut(22));
        card.add(Ui.divider());
        card.add(Box.createVerticalStrut(16));

        JLabel hint = new JLabel("演示账号：teacher（教师） / 2023001（学生），初始口令 123456");
        hint.setFont(Theme.plain(12));
        hint.setForeground(Theme.FAINT);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hint);
        card.add(Box.createVerticalStrut(10));
        card.add(quickFillRow());

        wrapper.add(card);
        return wrapper;
    }

    private JPanel quickFillRow() {
        JPanel row = Ui.row(10);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(quickFill("填入教师账号", "teacher"));
        row.add(quickFill("填入学生账号", "2023001"));
        row.add(Ui.stretch());
        return row;
    }

    /** 「还没有账号？立即注册」——注册成功后直接进入主界面，不用再登录一次。 */
    private JPanel registerRow() {
        JPanel row = Ui.row(6);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(Ui.muted("还没有账号？"));

        JLabel link = new JLabel("立即注册");
        link.setFont(Theme.bold(12));
        link.setForeground(Theme.PRIMARY);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                openRegister();
            }
        });
        row.add(link);
        row.add(Ui.stretch());
        return row;
    }

    private void openRegister() {
        RegisterDialog.show(this, context).ifPresent(user -> {
            new MainFrame(context, user).setVisible(true);
            dispose();
        });
    }

    private JLabel quickFill(String text, String username) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.plain(12));
        label.setForeground(Theme.PRIMARY);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                usernameField.setText(username);
                passwordField.setText("123456");
                errorLabel.setText(" ");
            }
        });
        return label;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.bold(12));
        label.setForeground(Theme.MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void stretch(javax.swing.JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        component.setPreferredSize(new Dimension(380, 40));
    }

    private void doLogin() {
        errorLabel.setText(" ");
        loginButton.setEnabled(false);
        loginButton.setText("正在登录…");
        try {
            User user = context.auth().login(usernameField.getText(), new String(passwordField.getPassword()));
            new MainFrame(context, user).setVisible(true);
            dispose();
        } catch (RuntimeException ex) {
            errorLabel.setText(ex.getMessage() == null ? "登录失败，请重试" : ex.getMessage());
            loginButton.setEnabled(true);
            loginButton.setText("登 录");
        }
    }

    /** 左侧品牌区：渐变背景 + 产品定位，帮助使用者第一眼理解系统做什么。 */
    private static final class BrandPanel extends JPanel {

        private static final Color GRADIENT_END = new Color(0x6F5BF6);

        BrandPanel() {
            setPreferredSize(new Dimension(440, 0));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(56, 48, 40, 40));

            JLabel badge = new JLabel("评", SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 255, 255, 38));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setFont(new Font(Theme.bold(20).getName(), Font.BOLD, 20));
            badge.setForeground(Color.WHITE);
            badge.setMaximumSize(new Dimension(44, 44));
            badge.setPreferredSize(new Dimension(44, 44));
            badge.setBorder(BorderFactory.createEmptyBorder());

            JLabel title = new JLabel("同伴互评");
            title.setFont(new Font(Theme.bold(30).getName(), Font.BOLD, 30));
            title.setForeground(Color.WHITE);

            JLabel subtitle = new JLabel("AI 辅助课堂同伴互评工具");
            subtitle.setFont(Theme.plain(14));
            subtitle.setForeground(new Color(255, 255, 255, 205));

            add(badge);
            add(Box.createVerticalStrut(26));
            add(title);
            add(Box.createVerticalStrut(10));
            add(subtitle);
            add(Box.createVerticalStrut(44));

            add(feature("自定义评分细则", "多维度权重 + 分值区间，教师自由配置"));
            add(Box.createVerticalStrut(18));
            add(feature("随机分配与盲评", "环状随机分配，互评时隐藏作者身份"));
            add(Box.createVerticalStrut(18));
            add(feature("过程留痕可统计", "每一条打分与评语都可追溯、可导出"));

            add(Box.createVerticalGlue());

            JLabel footer = new JLabel("《软件构造》课程个人项目");
            footer.setFont(Theme.plain(12));
            footer.setForeground(new Color(255, 255, 255, 150));
            add(footer);
        }

        private JPanel feature(String title, String description) {
            JPanel panel = Ui.row(12);
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel dot = new JLabel(Icons.check(13, new Color(255, 255, 255, 190)));
            JPanel text = Ui.column(2);
            JLabel name = new JLabel(title);
            name.setFont(Theme.bold(13));
            name.setForeground(Color.WHITE);
            JLabel desc = new JLabel(description);
            desc.setFont(Theme.plain(12));
            desc.setForeground(new Color(255, 255, 255, 165));
            text.add(name);
            text.add(desc);
            panel.add(dot);
            panel.add(text);
            panel.add(Ui.stretch());
            return panel;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), getHeight(), GRADIENT_END));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillOval(getWidth() - 150, -80, 260, 260);
            g2.setColor(new Color(255, 255, 255, 16));
            g2.fillOval(-90, getHeight() - 190, 240, 240);
            g2.dispose();
        }
    }
}
