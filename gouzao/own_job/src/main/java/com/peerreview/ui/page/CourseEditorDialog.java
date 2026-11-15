package com.peerreview.ui.page;

import com.peerreview.AppContext;
import com.peerreview.domain.Course;
import com.peerreview.domain.User;
import com.peerreview.ui.Dialogs;
import com.peerreview.ui.Theme;
import com.peerreview.ui.Ui;
import com.peerreview.ui.component.FlatButton;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.Optional;

/** 新建 / 编辑课程对话框。表单只采集输入，规则校验交给服务层。 */
public final class CourseEditorDialog extends JDialog {

    private final AppContext context;
    private final User teacher;
    private final Course editing;
    private final JTextField codeField;
    private final JTextField nameField;
    private final JTextField semesterField;
    private Course result;

    private CourseEditorDialog(Window owner, AppContext context, User teacher, Course course) {
        super(owner, course == null ? "新建课程" : "编辑课程", ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.teacher = teacher;
        this.editing = course;

        codeField = Ui.field("如 CS301", 18);
        nameField = Ui.field("如 软件构造", 18);
        semesterField = Ui.field("如 2026 秋", 18);
        if (course != null) {
            codeField.setText(course.getCode());
            nameField.setText(course.getName());
            semesterField.setText(course.getSemester());
        } else {
            semesterField.setText("2026 秋");
        }

        JPanel content = Ui.column(0);
        content.setBorder(new EmptyBorder(24, 26, 8, 26));
        content.add(Ui.h2("课程信息"));
        content.add(Ui.gap(0, 4));
        content.add(Ui.muted("课程代码用于区分同一学期的多门课程，创建后仍可修改。"));
        content.add(Ui.gap(0, 20));
        content.add(fieldLabel("课程代码"));
        content.add(codeField);
        content.add(Ui.gap(0, 14));
        content.add(fieldLabel("课程名称"));
        content.add(nameField);
        content.add(Ui.gap(0, 14));
        content.add(fieldLabel("学期"));
        content.add(semesterField);

        FlatButton cancel = Ui.secondary("取消");
        cancel.addActionListener(event -> dispose());
        FlatButton save = Ui.primary("保存");
        save.addActionListener(event -> save());

        JPanel footer = Ui.row(10);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(14, 26, 16, 26)));
        footer.add(Ui.stretch());
        footer.add(cancel);
        footer.add(save);

        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        setSize(new Dimension(520, 380));
        setLocationRelativeTo(owner);
    }

    public static Optional<Course> show(Component parent, AppContext context, User teacher, Course course) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        CourseEditorDialog dialog = new CourseEditorDialog(owner, context, teacher, course);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.result);
    }

    private JLabel fieldLabel(String text) {
        JLabel label = Ui.boldLabel(text);
        label.setBorder(new EmptyBorder(0, 0, 6, 0));
        return label;
    }

    private void save() {
        try {
            if (editing == null) {
                result = context.courses().create(teacher,
                        codeField.getText(), nameField.getText(), semesterField.getText());
            } else {
                context.courses().update(teacher, editing,
                        codeField.getText(), nameField.getText(), semesterField.getText());
                result = editing;
            }
            dispose();
        } catch (RuntimeException ex) {
            Dialogs.error(this, ex);
        }
    }
}
