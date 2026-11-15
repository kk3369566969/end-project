package com.peerreview.service;

import com.peerreview.AppContext;
import com.peerreview.common.Text;
import com.peerreview.common.ValidationException;
import com.peerreview.config.AppConfig;
import com.peerreview.db.ConnectionProvider;
import com.peerreview.db.DriverManagerConnectionProvider;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.AssignmentStatus;
import com.peerreview.domain.Course;
import com.peerreview.domain.ReviewRow;
import com.peerreview.domain.Role;
import com.peerreview.domain.RubricItem;
import com.peerreview.domain.SubmissionRow;
import com.peerreview.domain.SubmissionStats;
import com.peerreview.domain.TaskRow;
import com.peerreview.domain.User;
import com.peerreview.service.query.SubmissionFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端业务流程测试：使用内存数据库跑完整链路
 * 「教师建课布置作业 → 学生提交 → 分配互评 → 打分 → 统计 → 导出」。
 *
 * <p>这组测试同时承担集成验证职责：任何一层 SQL 与业务规则不匹配都会在这里暴露。</p>
 */
@DisplayName("完整互评业务流程")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowIntegrationTest {

    private static AppContext context;
    private static ConnectionProvider provider;
    private static User teacher;
    private static User studentA;
    private static long assignmentId;

    @BeforeAll
    static void setUp() {
        provider = new DriverManagerConnectionProvider(
                "jdbc:h2:mem:workflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa", "", "内存测试库");
        context = AppContext.connectWith(AppConfig.load(), provider);
        teacher = context.auth().login("teacher", "123456");
        studentA = context.auth().login("2023001", "123456");
    }

    @AfterAll
    static void tearDown() {
        context.close();
    }

    @Test
    @Order(1)
    @DisplayName("初始数据与登录：教师、学生账号可用，演示课程已就绪")
    void seedDataIsReady() {
        assertEquals("张明远", teacher.shortName());
        assertTrue(teacher.isTeacher());
        List<Course> courses = context.courses().listVisible(teacher);
        assertFalse(courses.isEmpty());
        assertThrows(ValidationException.class, () -> context.auth().login("teacher", "wrong-password"));
    }

    @Test
    @Order(2)
    @DisplayName("教师建课并布置作业，权重校验生效")
    void teacherCreatesAssignment() {
        Course course = context.courses().create(teacher, "CS401", "软件测试", "2026 秋");

        Assignment draft = new Assignment();
        draft.setTitle("实验二：单元测试设计");
        draft.setDescription("为一个已有模块补充单元测试，覆盖正常、边界与异常三类用例。");
        draft.setReviewersPerSubmission(2);
        draft.setAnonymous(true);
        draft.setRubricItems(List.of(
                new RubricItem("用例覆盖", 50, 0, 10),
                new RubricItem("断言质量", 50, 0, 10)));
        Assignment created = context.assignments().create(teacher, course.getId(), draft);
        assertNotNull(created.getId());
        assertEquals(AssignmentStatus.DRAFT, created.getStatus());

        Assignment duplicate = new Assignment();
        duplicate.setTitle("非法权重作业");
        duplicate.setDescription("用于验证权重合计校验。");
        duplicate.setReviewersPerSubmission(1);
        duplicate.setRubricItems(List.of(new RubricItem("覆盖", 80, 0, 10)));
        assertThrows(ValidationException.class,
                () -> context.assignments().create(teacher, course.getId(), duplicate));

        context.assignments().changeStatus(teacher, created.getId(), AssignmentStatus.PUBLISHED);
        assertEquals(AssignmentStatus.PUBLISHED, context.assignments().require(created.getId()).getStatus());
        // 非法状态流转：开放提交不能直接跳到「互评中」
        assertThrows(ValidationException.class,
                () -> context.assignments().changeStatus(teacher, created.getId(), AssignmentStatus.ALLOCATED));
    }

    @Test
    @Order(3)
    @DisplayName("学生提交：空内容与过短内容被拒绝，重复提交视为更新")
    void studentSubmitsAssignment() {
        // 选择已有学生提交的作业（演示数据），才能走通分配与互评
        assignmentId = context.assignments().listAll().stream()
                .filter(a -> a.getStatus() == AssignmentStatus.PUBLISHED)
                .filter(a -> context.submissions().countByAssignment(a.getId()) >= 2)
                .findFirst()
                .orElseThrow()
                .getId();

        assertThrows(ValidationException.class,
                () -> context.submissions().submit(studentA, assignmentId, "   "));
        assertThrows(ValidationException.class,
                () -> context.submissions().submit(studentA, assignmentId, "太短了"));

        String content = "本实验按模块拆分了解析器与执行器，并为每个模块补充了 12 个单元测试用例。";
        context.submissions().submit(studentA, assignmentId, content);
        String updated = content + "补充了边界用例。";
        context.submissions().submit(studentA, assignmentId, updated);

        assertEquals(updated, context.submissions().findMine(assignmentId, studentA.getId()).orElseThrow().getContent());
        assertThrows(ValidationException.class,
                () -> context.submissions().submit(teacher, assignmentId, content));
    }

    @Test
    @Order(4)
    @DisplayName("分配互评：任务数正确、不会自评、重复分配需显式确认")
    void allocateReviewTasks() {
        int submissions = context.submissions().countByAssignment(assignmentId);
        assertTrue(submissions >= 2);

        AllocationService.AllocationResult result =
                context.allocation().allocate(teacher, assignmentId, false);
        assertEquals(submissions * 2, result.taskCount());
        assertEquals(AssignmentStatus.ALLOCATED, context.assignments().require(assignmentId).getStatus());

        assertThrows(ValidationException.class,
                () -> context.allocation().allocate(teacher, assignmentId, false));

        List<TaskRow> tasks = context.reviews().myTasks(studentA.getId(), null,
                com.peerreview.common.RegexTextMatcher.matchAll());
        assertEquals(2, tasks.size());
        tasks.forEach(task -> assertFalse(task.content().isBlank()));
    }

    @Test
    @Order(5)
    @DisplayName("打分：评语过短被拒绝，合法提交后任务状态变为已完成并计入统计")
    void submitReviewAndStatistics() {
        List<TaskRow> tasks = context.reviews().myTasks(studentA.getId(), null,
                com.peerreview.common.RegexTextMatcher.matchAll());
        TaskRow task = tasks.get(0);
        Assignment assignment = context.reviews().assignmentOf(task.assignmentId());

        Map<Long, Double> scores = new HashMap<>();
        for (RubricItem item : assignment.getRubricItems()) {
            scores.put(item.getId(), (item.getMinScore() + item.getMaxScore()) / 2);
        }

        assertThrows(ValidationException.class,
                () -> context.reviews().submit(studentA, task.taskId(), scores, "还行"));
        assertThrows(ValidationException.class,
                () -> context.reviews().submit(studentA, task.taskId(), Map.of(), "缺少打分项，评语足够长"));
        assertThrows(ValidationException.class,
                () -> context.reviews().submit(studentA, task.taskId(), Map.of(
                        assignment.getRubricItems().get(0).getId(), 999.0,
                        assignment.getRubricItems().get(1).getId(), 5.0), "分数越界应当被拒绝"));

        context.reviews().submit(studentA, task.taskId(), scores,
                "整体结构清晰，模块职责划分合理，建议补充异常路径的测试用例。");

        List<TaskRow> refreshed = context.reviews().myTasks(studentA.getId(), null,
                com.peerreview.common.RegexTextMatcher.matchAll());
        long completed = refreshed.stream().filter(TaskRow::isCompleted).count();
        assertEquals(1, completed);

        List<SubmissionStats> stats = context.statistics().statsFor(assignmentId);
        assertFalse(stats.isEmpty());
        SubmissionStats reviewed = stats.stream().filter(s -> s.counted() > 0).findFirst().orElseThrow();
        assertEquals(1, reviewed.counted());
        assertTrue(reviewed.average() > 0);

        List<ReviewRow> reviews = context.reviews().reviewsOfSubmission(reviewed.submissionId());
        assertEquals(1, reviews.size());
        assertTrue(reviews.get(0).comment().contains("结构清晰"));
    }

    @Test
    @Order(6)
    @DisplayName("检索与导出：正则过滤生效，CSV 含表头与学生数据")
    void searchAndExport() throws Exception {
        List<SubmissionRow> all = context.statistics().searchSubmissions(new SubmissionFilter());
        assertFalse(all.isEmpty());

        List<SubmissionRow> filtered = context.statistics().searchSubmissions(
                new SubmissionFilter().keyword(com.peerreview.common.RegexTextMatcher.of("单元测试")));
        assertTrue(filtered.size() <= all.size());

        assertThrows(ValidationException.class,
                () -> com.peerreview.common.RegexTextMatcher.of("(unclosed"));

        Path target = Files.createTempFile("成绩汇总", ".csv");
        context.statistics().exportCsv(context.assignments().require(assignmentId), target);
        String csv = Files.readString(target);
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("平均分"));
        assertTrue(Text.isNotBlank(csv));
        Files.deleteIfExists(target);
    }

    @Test
    @Order(7)
    @DisplayName("删除作业会级联清理提交与任务，避免孤儿数据")
    void deleteCascade() {
        long submissionCount = context.statistics().searchSubmissions(new SubmissionFilter()
                .assignment(assignmentId)).size();
        assertTrue(submissionCount > 0);

        context.assignments().delete(teacher, assignmentId);

        assertEquals(0, context.statistics().searchSubmissions(
                new SubmissionFilter().assignment(assignmentId)).size());
        assertTrue(context.reviews().myTasks(studentA.getId(), null,
                com.peerreview.common.RegexTextMatcher.matchAll()).stream()
                .noneMatch(task -> task.assignmentId() == assignmentId));
    }

    @Test
    @Order(8)
    @DisplayName("自助注册：新账号落库后可直接登录，重复注册被唯一约束拦下")
    void registerNewAccount() {
        User created = context.auth().register(
                RegistrationRequest.student("2023901", "测试同学", "abc123", "abc123"));

        assertTrue(created.getId() > 0, "应回填自增主键");
        assertEquals(Role.STUDENT, created.getRole());

        User loggedIn = context.auth().login("2023901", "abc123");
        assertEquals("测试同学", loggedIn.getDisplayName());
        assertFalse(loggedIn.isTeacher());

        ValidationException duplicate = assertThrows(ValidationException.class,
                () -> context.auth().register(
                        RegistrationRequest.student("2023901", "重复注册", "abc123", "abc123")));
        assertTrue(duplicate.getMessage().contains("已被注册"), duplicate.getMessage());
    }
}
