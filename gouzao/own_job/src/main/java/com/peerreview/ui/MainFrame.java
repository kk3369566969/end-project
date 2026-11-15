package com.peerreview.ui;

import com.peerreview.AppContext;
import com.peerreview.domain.User;
import com.peerreview.ui.component.FlatButton;
import com.peerreview.ui.component.NavButton;
import com.peerreview.ui.page.StudentAssignmentPage;
import com.peerreview.ui.page.StudentReviewPage;
import com.peerreview.ui.page.TeacherCoursePage;
import com.peerreview.ui.page.TeacherStatisticsPage;
import com.peerreview.ui.page.TeacherSubmissionPage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * 主框架：左侧导航 + 顶部页头 + 中部卡片内容区。
 *
 * <p>导航项由当前角色可见的 {@link Page} 列表自动生成；新增页面只需在 {@link #pagesFor} 里登记。</p>
 */
public final class MainFrame extends JFrame {

    private static final int SIDEBAR_WIDTH = 232;

    private final AppContext context;
    private final User user;
    private final List<Page> pages = new ArrayList<>();
    private final List<NavButton> navButtons = new ArrayList<>();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final JLabel pageTitle = new JLabel();
    private final JLabel pageSubtitle = new JLabel();
    private Page current;

    public MainFrame(AppContext context, User user) {
        super("同伴互评 · " + user.shortName());
        this.context = context;
        this.user = user;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1360, 860);
        setMinimumSize(new Dimension(1180, 740));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG);

        pages.addAll(pagesFor());
        content.setBackground(Theme.BG);

        add(buildSidebar(), BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);

        if (!pages.isEmpty()) {
            show(pages.get(0));
        }
    }

    private List<Page> pagesFor() {
        if (user.isTeacher()) {
            return List.of(
                    new TeacherCoursePage(context, user),
                    new TeacherSubmissionPage(context, user),
                    new TeacherStatisticsPage(context, user));
        }
        return List.of(
                new StudentAssignmentPage(context, user),
                new StudentReviewPage(context, user));
    }

    // ———————————————————— 侧栏 ————————————————————

    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SURFACE);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        sidebar.add(buildBrand());
        sidebar.add(buildNav());
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(buildUserCard());
        return sidebar;
    }

    private JComponent buildBrand() {
        JPanel brand = Ui.row(12);
        brand.setBorder(new EmptyBorder(24, 20, 22, 20));
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));

        JLabel logo = new JLabel("评", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logo.setFont(Theme.bold(17));
        logo.setForeground(Color.WHITE);
        logo.setPreferredSize(new Dimension(36, 36));
        logo.setMaximumSize(new Dimension(36, 36));

        JPanel text = Ui.column(2);
        JLabel name = new JLabel("同伴互评");
        name.setFont(Theme.bold(15));
        name.setForeground(Theme.TEXT);
        JLabel sub = new JLabel("Peer Review");
        sub.setFont(Theme.plain(11));
        sub.setForeground(Theme.FAINT);
        text.add(name);
        text.add(sub);

        brand.add(logo);
        brand.add(text);
        brand.add(Ui.stretch());
        return brand;
    }

    private JComponent buildNav() {
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setOpaque(false);
        nav.setBorder(new EmptyBorder(4, 12, 4, 12));

        for (Page page : pages) {
            NavButton button = new NavButton(page.navLabel(), page.navIcon());
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            button.setPreferredSize(new Dimension(SIDEBAR_WIDTH - 24, 44));
            button.addActionListener(event -> show(page));
            nav.add(button);
            nav.add(Box.createVerticalStrut(4));
            navButtons.add(button);
        }
        return nav;
    }

    private JComponent buildUserCard() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(14, 18, 18, 18));

        JPanel info = Ui.row(10);
        JLabel avatar = new JLabel(user.shortName().substring(0, 1), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PRIMARY_SOFT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setFont(Theme.bold(14));
        avatar.setForeground(Theme.PRIMARY);
        avatar.setPreferredSize(new Dimension(34, 34));
        avatar.setMaximumSize(new Dimension(34, 34));

        JPanel text = Ui.column(2);
        JLabel name = new JLabel(user.shortName());
        name.setFont(Theme.bold(13));
        name.setForeground(Theme.TEXT);
        JLabel role = new JLabel(user.getRole().label() + " · " + user.getUsername());
        role.setFont(Theme.plain(11));
        role.setForeground(Theme.MUTED);
        text.add(name);
        text.add(role);

        info.add(avatar);
        info.add(text);
        info.add(Ui.stretch());

        FlatButton logout = Ui.ghost("退出登录").withIcon(Icons.logout(16, Theme.MUTED));
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        logout.addActionListener(event -> logout());

        wrapper.add(info, BorderLayout.NORTH);
        wrapper.add(logout, BorderLayout.SOUTH);
        return wrapper;
    }

    // ———————————————————— 内容区 ————————————————————

    private JComponent buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);
        main.add(buildHeader(), BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);

        for (Page page : pages) {
            JPanel holder = new JPanel(new BorderLayout());
            holder.setBackground(Theme.BG);
            holder.setBorder(new EmptyBorder(4, 26, 24, 26));
            holder.add(page.view(), BorderLayout.CENTER);
            content.add(holder, page.navLabel());
        }
        return main;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(16, 26, 16, 26)));

        pageTitle.setFont(new Font(Theme.bold(20).getName(), Font.BOLD, 20));
        pageTitle.setForeground(Theme.TEXT);
        pageSubtitle.setFont(Theme.plain(12));
        pageSubtitle.setForeground(Theme.MUTED);

        JPanel text = Ui.column(4);
        text.add(pageTitle);
        text.add(pageSubtitle);

        FlatButton refresh = Ui.secondary("刷新").withIcon(Icons.refresh(16, Theme.MUTED)).compact();
        refresh.addActionListener(event -> {
            if (current != null) {
                current.onShow();
            }
        });

        JLabel connection = Ui.hint(user.isTeacher() ? "教师视图" : "学生视图");
        JPanel right = Ui.row(12);
        right.add(connection);
        right.add(refresh);

        header.add(text, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void show(Page page) {
        current = page;
        pageTitle.setText(page.title());
        pageSubtitle.setText(page.subtitle());
        cardLayout.show(content, page.navLabel());
        for (int i = 0; i < pages.size(); i++) {
            navButtons.get(i).setSelected(pages.get(i) == page);
        }
        page.onShow();
    }

    private void logout() {
        if (!Dialogs.confirm(this, "退出登录", "确定要退出当前账号吗？", "退出")) {
            return;
        }
        dispose();
        new LoginFrame(context).setVisible(true);
    }
}
