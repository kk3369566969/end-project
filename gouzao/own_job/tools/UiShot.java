import com.peerreview.AppContext;
import com.peerreview.ai.ReviewSuggestion;
import com.peerreview.ai.ReviewSuggestionProvider;
import com.peerreview.ai.SuggestionRequest;
import com.peerreview.common.AppLogger;
import com.peerreview.common.RegexTextMatcher;
import com.peerreview.config.AppConfig;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.Course;
import com.peerreview.domain.Review;
import com.peerreview.domain.RubricItem;
import com.peerreview.domain.TaskRow;
import com.peerreview.domain.User;
import com.peerreview.ui.LoginFrame;
import com.peerreview.ui.MainFrame;
import com.peerreview.ui.RegisterDialog;
import com.peerreview.ui.Theme;
import com.peerreview.ui.page.AiAssistDialog;
import com.peerreview.ui.page.AssignmentEditorDialog;
import com.peerreview.ui.page.ReviewDialog;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 离屏截图工具：把关键界面与弹窗渲染成 PNG，便于检查视觉效果（仅开发期使用）。 */
public class UiShot {

    private static AppContext context;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> Theme.install());
        AppConfig config = AppConfig.load();
        AppLogger.get().init(Paths.get("logs", "app.log"));
        context = AppContext.connectEmbedded(config);
        Files.createDirectories(Paths.get("shots"));

        LoginFrame login = new LoginFrame(context);
        show(login, 1000, 640);
        shot(login, "shots/01-login.png");
        login.dispose();

        User teacher = context.auth().login("teacher", "123456");
        Assignment demo = allocateDemoTasks(teacher);
        submitDemoReviews(demo);
        Course course = context.courses().listVisible(teacher).get(0);

        MainFrame teacherFrame = new MainFrame(context, teacher);
        show(teacherFrame, 1360, 860);
        shot(teacherFrame, "shots/02-teacher-course.png");
        clickNav(teacherFrame, "提交管理");
        shot(teacherFrame, "shots/03-teacher-submission.png");
        clickNav(teacherFrame, "统计与导出");
        shot(teacherFrame, "shots/04-teacher-stats.png");
        teacherFrame.dispose();

        captureDialog(newAssignmentDialog(teacher, course.getId()), "shots/07-assignment-editor.png", 840, 820);

        User student = context.auth().login("2023001", "123456");
        MainFrame studentFrame = new MainFrame(context, student);
        show(studentFrame, 1360, 860);
        shot(studentFrame, "shots/05-student-assignment.png");
        clickNav(studentFrame, "互评任务");
        shot(studentFrame, "shots/06-student-review.png");
        studentFrame.dispose();

        captureDialog(registerDialog(), "shots/10-register.png", 520, 672);
        captureDialog(reviewDialog(student), "shots/08-review-dialog.png", 940, 840);
        captureDialog(aiDialog(student), "shots/09-ai-assist.png", 780, 648);

        System.out.println("done");
        System.exit(0);
    }

    private static Assignment allocateDemoTasks(User teacher) {
        try {
            Assignment target = context.assignments().listAll().stream()
                    .filter(a -> context.submissions().countByAssignment(a.getId()) >= 2)
                    .findFirst().orElseThrow();
            context.allocation().allocate(teacher, target.getId(), true);
            return target;
        } catch (RuntimeException ex) {
            System.out.println("跳过分配：" + ex.getMessage());
            return null;
        }
    }

    /** 让部分学生先完成互评，统计页与成绩汇总才有真实数据可展示。 */
    private static void submitDemoReviews(Assignment target) {
        if (target == null) {
            return;
        }
        List<RubricItem> items = context.reviews().assignmentOf(target.getId()).getRubricItems();
        String[][] demo = {{"2023002", "8.5"}, {"2023003", "7"}, {"2023004", "9"}};
        for (String[] entry : demo) {
            User reviewer = context.auth().login(entry[0], "123456");
            List<TaskRow> tasks = context.reviews()
                    .myTasks(reviewer.getId(), null, RegexTextMatcher.matchAll());
            tasks = tasks.stream().filter(t -> t.assignmentId() == target.getId()).toList();
            if (tasks.isEmpty() || tasks.get(0).isCompleted()) {
                continue;
            }
            double score = Double.parseDouble(entry[1]);
            Map<Long, Double> scores = new LinkedHashMap<>();
            for (RubricItem item : items) {
                scores.put(item.getId(), Math.min(item.getMaxScore(), score));
            }
            context.reviews().submit(reviewer, tasks.get(0).taskId(), scores,
                    "模块划分清晰，说明书中对设计取舍也有交代；建议补充边界条件的测试用例，"
                            + "并把异常处理统一收敛到服务层，减少重复的 try-catch。");
        }
    }

    private static RegisterDialog registerDialog() throws Exception {
        Constructor<RegisterDialog> constructor =
                RegisterDialog.class.getDeclaredConstructor(Window.class, AppContext.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, context);
    }

    private static AssignmentEditorDialog newAssignmentDialog(User teacher, long courseId) throws Exception {
        Constructor<AssignmentEditorDialog> constructor = AssignmentEditorDialog.class.getDeclaredConstructor(
                Window.class, AppContext.class, User.class, long.class, Assignment.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, context, teacher, courseId, null);
    }

    private static ReviewDialog reviewDialog(User student) throws Exception {
        TaskRow task = context.reviews().myTasks(student.getId(), null,
                com.peerreview.common.RegexTextMatcher.matchAll()).get(0);
        Assignment assignment = context.reviews().assignmentOf(task.assignmentId());
        Constructor<ReviewDialog> constructor = ReviewDialog.class.getDeclaredConstructor(
                Window.class, AppContext.class, User.class, TaskRow.class, Assignment.class, Optional.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, context, student, task, assignment, Optional.<Review>empty());
    }

    /**
     * 用固定数据的假提供者渲染 AI 窗口：截图不应该消耗真实额度，
     * 也不应该因为网络抖动导致产物时好时坏。
     */
    private static AiAssistDialog aiDialog(User student) throws Exception {
        TaskRow task = context.reviews().myTasks(student.getId(), null, RegexTextMatcher.matchAll()).get(0);
        Assignment assignment = context.reviews().assignmentOf(task.assignmentId());
        SuggestionRequest request = new SuggestionRequest(assignment.getTitle(),
                assignment.getDescription(), assignment.getRubricItems(), task.content());
        Constructor<AiAssistDialog> constructor = AiAssistDialog.class.getDeclaredConstructor(
                Window.class, ReviewSuggestionProvider.class, SuggestionRequest.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, demoProvider(assignment.getRubricItems()), request);
    }

    private static ReviewSuggestionProvider demoProvider(List<RubricItem> rubric) {
        String[] reasons = {
                "按层分包，业务层不感知 SQL，职责边界清晰",
                "计分规则抽象为接口并按模式分发，便于扩展",
                "三级异常体系并在界面统一兜底，未向用户抛堆栈"};
        double[] raw = {8.5, 9, 4};
        List<ReviewSuggestion.DimensionHint> hints = new ArrayList<>();
        for (int index = 0; index < rubric.size(); index++) {
            RubricItem item = rubric.get(index);
            double score = index < raw.length ? Math.min(item.getMaxScore(), raw[index])
                    : (item.getMinScore() + item.getMaxScore()) / 2;
            hints.add(new ReviewSuggestion.DimensionHint(item.getName(), score,
                    reasons[index % reasons.length]));
        }
        ReviewSuggestion suggestion = new ReviewSuggestion(hints,
                "作业把计分规则抽象为 ScoreCalculator 接口，并用注册表按 ScoringMode 分发，"
                        + "DAO 层以接口收敛连接管理与异常转换，分层边界清晰。异常处理建立三级体系，"
                        + "界面层统一捕获后以对话框提示，没有把堆栈抛给用户。改进建议：为数据库连接失败补充"
                        + "重试或降级策略，日志级别改为可配置，并在正文中说明边界条件的测试覆盖情况。"
                        + "整体结构合理，补齐异常与日志细节后完成度更高。",
                "deepseek-chat", 2180, 874);

        return new ReviewSuggestionProvider() {
            @Override
            public String displayName() {
                return "DeepSeek · deepseek-chat";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public String unavailableReason() {
                return "";
            }

            @Override
            public ReviewSuggestion suggest(SuggestionRequest request) {
                return suggestion;
            }
        };
    }

    private static void captureDialog(JDialog dialog, String path, int width, int height) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            dialog.setUndecorated(true);
            dialog.setModal(false);
            dialog.setSize(width, height);
            dialog.setLocation(-4000, -3000);
            dialog.setVisible(true);
            dialog.validate();
        });
        Thread.sleep(600);
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage image = new BufferedImage(dialog.getWidth(), dialog.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            dialog.printAll(g);
            g.dispose();
            try {
                ImageIO.write(image, "png", new File(path));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            dialog.dispose();
        });
        System.out.println("captured " + path);
    }

    private static void show(JFrame frame, int width, int height) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.setUndecorated(true);
            frame.setSize(width, height);
            frame.setLocation(-4000, -3000);
            frame.setVisible(true);
            frame.validate();
        });
        Thread.sleep(700);
    }

    private static void clickNav(JFrame frame, String label) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton button = findButton(frame.getContentPane(), label);
            if (button == null) {
                System.out.println("未找到导航按钮：" + label);
                return;
            }
            button.doClick();
            frame.validate();
        });
        Thread.sleep(700);
    }

    private static JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container container) {
                JButton found = findButton(container, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void shot(JFrame frame, String path) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.validate();
            BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            frame.printAll(g);
            g.dispose();
            try {
                ImageIO.write(image, "png", new File(path));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        System.out.println("captured " + path);
    }
}
