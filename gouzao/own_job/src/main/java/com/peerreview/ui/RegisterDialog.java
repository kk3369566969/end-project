package com.peerreview.ui;

import com.peerreview.AppContext;
import com.peerreview.domain.Role;
import com.peerreview.domain.User;
import com.peerreview.service.RegistrationRequest;
import com.peerreview.ui.component.FlatButton;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.Optional;

/**
 * 注册窗口：采集账号信息，校验与落库全部交给 {@code AuthService}。
 *
 * <p>界面只做两件"界面该做的事"：教师角色才启用邀请码输入、把服务层的失败提示显示在表单下方。
 * 规则本身（长度、格式、重复、邀请码）不在这里判断，避免同一规则在两处各写一遍。</p>
 */
public final class RegisterDialog extends JDialog {

    private static final int FIELD_WIDTH = 460;

    private final AppContext context;

    private final JRadioButton studentRadio = new JRadioButton("学生", true);
    private final JRadioButton teacherRadio = new JRadioButton("教师");
    private final JTextField usernameField = Ui.field("学号或工号，如 2023007", 18);
    private final JTextField nameField = Ui.field("真实姓名，用于作业署名", 18);
    private final JPasswordField passwordField = Ui.passwordField("至少 6 位", 18);
    private final JPasswordField confirmField = Ui.passwordField("再次输入密码", 18);
    private final JTextField inviteField = Ui.field("教师注册邀请码", 18);
    private final JLabel inviteHint = Ui.hint("学生注册无需邀请码");
    private final JLabel errorLabel = new JLabel(" ");
    private final FlatButton submitButton = Ui.primary("注册并登录");

    private User result;

    private RegisterDialog(Window owner, AppContext context) {
        super(owner, "注册账号", ModalityType.APPLICATION_MODAL);
        this.context = context;

        JPanel content = Ui.column(0);
        content.setBorder(new EmptyBorder(26, 30, 12, 30));
        content.add(Ui.h2("创建账号"));
        content.add(Ui.gap(0, 4));
        content.add(Ui.muted("注册后可提交作业、完成同伴互评任务。"));
        content.add(Ui.gap(0, 22));

        content.add(fieldLabel("角色"));
        content.add(roleRow());
        content.add(Ui.gap(0, 14));

        content.add(fieldLabel("学号 / 工号"));
        stretch(usernameField);
        content.add(usernameField);
        content.add(Ui.gap(0, 4));
        content.add(Ui.hint("登录时使用的账号，4~20 位字母、数字或下划线"));
        content.add(Ui.gap(0, 12));

        content.add(fieldLabel("姓名"));
        stretch(nameField);
        content.add(nameField);
        content.add(Ui.gap(0, 12));

        content.add(fieldLabel("密码"));
        stretch(passwordField);
        content.add(passwordField);
        content.add(Ui.gap(0, 12));

        content.add(fieldLabel("确认密码"));
        stretch(confirmField);
        content.add(confirmField);
        content.add(Ui.gap(0, 12));

        content.add(fieldLabel("教师邀请码"));
        stretch(inviteField);
        content.add(inviteField);
        content.add(Ui.gap(0, 4));
        content.add(inviteHint);
        content.add(Ui.gap(0, 14));

        errorLabel.setFont(Theme.plain(12));
        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setPreferredSize(new Dimension(FIELD_WIDTH, 18));
        content.add(errorLabel);

        // 没有弹性占位时，BoxLayout 会把多余高度均摊给每个子组件，
        // 表现为标签与输入框之间莫名出现大段空白；放一个 glue 让余量集中到底部。
        content.add(Box.createVerticalGlue());

        FlatButton cancel = Ui.secondary("返回登录");
        cancel.addActionListener(event -> dispose());
        submitButton.addActionListener(event -> submit());

        JPanel footer = Ui.row(10);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(14, 30, 16, 30)));
        footer.add(Ui.stretch());
        footer.add(cancel);
        footer.add(submitButton);

        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(submitButton);

        studentRadio.addActionListener(event -> syncRole());
        teacherRadio.addActionListener(event -> syncRole());
        syncRole();

        setSize(new Dimension(520, 672));
        setLocationRelativeTo(owner);
    }

    /** 打开注册窗口；返回已创建的账号，取消则返回空。 */
    public static Optional<User> show(Component parent, AppContext context) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        RegisterDialog dialog = new RegisterDialog(owner, context);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.result);
    }

    private JComponent roleRow() {
        ButtonGroup group = new ButtonGroup();
        group.add(studentRadio);
        group.add(teacherRadio);

        studentRadio.setFont(Theme.plain(13));
        studentRadio.setForeground(Theme.TEXT);
        studentRadio.setOpaque(false);
        studentRadio.setFocusPainted(false);

        teacherRadio.setFont(Theme.plain(13));
        teacherRadio.setForeground(Theme.TEXT);
        teacherRadio.setOpaque(false);
        teacherRadio.setFocusPainted(false);

        JPanel row = Ui.row(20);
        row.add(studentRadio);
        row.add(teacherRadio);
        row.add(Ui.stretch());
        return row;
    }

    /** 角色切换只影响邀请码是否可用，不在界面上做权限判断。 */
    private void syncRole() {
        boolean teacher = teacherRadio.isSelected();
        inviteField.setEnabled(teacher);
        inviteField.setText(teacher ? inviteField.getText() : "");
        inviteHint.setText(teacher
                ? "教师注册需邀请码（默认 TEACH2026，可在配置文件修改）"
                : "学生注册无需邀请码");
        inviteField.setBackground(teacher ? Theme.SURFACE : Theme.BG);
    }

    private JLabel fieldLabel(String text) {
        JLabel label = Ui.boldLabel(text);
        label.setBorder(new EmptyBorder(0, 0, 6, 0));
        return label;
    }

    private void stretch(JComponent component) {
        component.setPreferredSize(new Dimension(FIELD_WIDTH, 40));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    }

    private void submit() {
        errorLabel.setText(" ");
        submitButton.setEnabled(false);
        try {
            Role role = teacherRadio.isSelected() ? Role.TEACHER : Role.STUDENT;
            RegistrationRequest request = new RegistrationRequest(
                    usernameField.getText(),
                    nameField.getText(),
                    new String(passwordField.getPassword()),
                    new String(confirmField.getPassword()),
                    role,
                    inviteField.getText());
            result = context.auth().register(request);
            dispose();
        } catch (RuntimeException ex) {
            errorLabel.setText(ex.getMessage() == null ? "注册失败，请重试" : ex.getMessage());
            submitButton.setEnabled(true);
        }
    }
}
