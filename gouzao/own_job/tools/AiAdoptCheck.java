import com.peerreview.AppContext;
import com.peerreview.ai.AiAdoption;
import com.peerreview.common.AppLogger;
import com.peerreview.common.RegexTextMatcher;
import com.peerreview.config.AppConfig;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.Review;
import com.peerreview.domain.RubricItem;
import com.peerreview.domain.TaskRow;
import com.peerreview.domain.User;
import com.peerreview.ui.Theme;
import com.peerreview.ui.page.ReviewDialog;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 「采纳 AI 草稿」链路的验收脚本：离屏构造打分窗口 → 注入采纳结果 → 读回控件核对。
 *
 * <p>这条链路横跨 AI 模块与 Swing 控件，JUnit 测不到；
 * 但它是整个功能的关键——如果分数只是"看起来被采纳"而没落到 JSpinner 上，
 * 学生点提交时提交的仍是旧值，属于静默错误。所以单独用脚本验一次。</p>
 */
public final class AiAdoptCheck {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(Theme::install);
        AppConfig config = AppConfig.load();
        AppLogger.get().init(Paths.get("logs", "app.log"));
        AppContext context = AppContext.connectEmbedded(config);

        User student = context.auth().login("2023001", "123456");
        List<TaskRow> tasks = context.reviews().myTasks(student.getId(), null, RegexTextMatcher.matchAll());
        TaskRow task = tasks.stream().filter(row -> !row.isCompleted()).findFirst().orElse(tasks.get(0));
        Assignment assignment = context.reviews().assignmentOf(task.assignmentId());

        Constructor<ReviewDialog> constructor = ReviewDialog.class.getDeclaredConstructor(
                Window.class, AppContext.class, User.class, TaskRow.class, Assignment.class, Optional.class);
        constructor.setAccessible(true);
        ReviewDialog dialog = constructor.newInstance(null, context, student, task, assignment,
                Optional.<Review>empty());

        // 故意让最后一个维度给一个越界值，验证「填不进去的值会被裁到区间边界」
        Map<Long, Double> scores = new LinkedHashMap<>();
        List<RubricItem> rubric = assignment.getRubricItems();
        for (int index = 0; index < rubric.size(); index++) {
            RubricItem item = rubric.get(index);
            double value = index == rubric.size() - 1
                    ? item.getMaxScore() + 100
                    : item.getMinScore() + item.scoreSpan() * 0.75;
            scores.put(item.getId(), Math.round(value * 2) / 2.0);
        }
        String comment = "AI 草稿：分层清晰，建议补充边界条件的测试用例。";
        AiAdoption adoption = new AiAdoption(scores, comment);

        Method apply = ReviewDialog.class.getDeclaredMethod("applyAdoption", AiAdoption.class);
        apply.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                apply.invoke(dialog, adoption);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });

        Map<Long, JSpinner> spinners = (Map<Long, JSpinner>) field(dialog, "spinners");
        JTextArea commentArea = (JTextArea) field(dialog, "commentArea");
        JLabel totalLabel = (JLabel) field(dialog, "totalLabel");

        System.out.println("评语回填一致   : " + comment.equals(commentArea.getText()));
        System.out.println("总分显示       : " + totalLabel.getText());
        boolean allInRange = true;
        for (RubricItem item : rubric) {
            JSpinner spinner = spinners.get(item.getId());
            double value = spinner == null ? Double.NaN : ((Number) spinner.getValue()).doubleValue();
            boolean inRange = value >= item.getMinScore() && value <= item.getMaxScore();
            allInRange &= inRange;
            System.out.printf("  %-8s 期望=%-6s 实际=%-6s %s%n",
                    item.getName(),
                    RubricItem.trim(scores.get(item.getId())),
                    RubricItem.trim(value),
                    inRange ? "OK" : "越界！");
        }
        System.out.println("全部落在区间内 : " + allInRange);
        System.out.println("控件数量       : " + spinners.size() + " / 细则 " + rubric.size());
        System.exit(0);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
