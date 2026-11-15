package com.peerreview.service;

import com.peerreview.common.AppLogger;
import com.peerreview.common.Text;
import com.peerreview.common.ValidationException;
import com.peerreview.dao.AssignmentDao;
import com.peerreview.dao.CourseDao;
import com.peerreview.dao.ReviewDao;
import com.peerreview.dao.ReviewTaskDao;
import com.peerreview.dao.SubmissionDao;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.AssignmentStatus;
import com.peerreview.domain.Course;
import com.peerreview.domain.RubricItem;
import com.peerreview.domain.RubricValidator;
import com.peerreview.domain.User;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 作业与评分细则的业务规则入口。
 *
 * <p>集中了三条关键规则：① 细则权重合计必须为 100%；② 分值区间合法；
 * ③ 作业状态只能沿既有流程流转（草稿 → 开放提交 → 互评中 → 已结束）。</p>
 */
public final class AssignmentService {

    /** 作业状态流转表（表驱动，新增状态只需改这一处）。 */
    private static final Map<AssignmentStatus, Set<AssignmentStatus>> TRANSITIONS = new EnumMap<>(AssignmentStatus.class);

    static {
        TRANSITIONS.put(AssignmentStatus.DRAFT, Set.of(AssignmentStatus.PUBLISHED));
        TRANSITIONS.put(AssignmentStatus.PUBLISHED, Set.of(AssignmentStatus.DRAFT, AssignmentStatus.CLOSED));
        TRANSITIONS.put(AssignmentStatus.ALLOCATED, Set.of(AssignmentStatus.CLOSED));
        TRANSITIONS.put(AssignmentStatus.CLOSED, Set.of());
    }

    private final AssignmentDao assignmentDao;
    private final CourseDao courseDao;
    private final SubmissionDao submissionDao;
    private final ReviewTaskDao reviewTaskDao;
    private final ReviewDao reviewDao;

    public AssignmentService(AssignmentDao assignmentDao,
                             CourseDao courseDao,
                             SubmissionDao submissionDao,
                             ReviewTaskDao reviewTaskDao,
                             ReviewDao reviewDao) {
        this.assignmentDao = assignmentDao;
        this.courseDao = courseDao;
        this.submissionDao = submissionDao;
        this.reviewTaskDao = reviewTaskDao;
        this.reviewDao = reviewDao;
    }

    public List<Assignment> listByCourse(long courseId) {
        return assignmentDao.findByCourse(courseId);
    }

    public List<Assignment> listAll() {
        return assignmentDao.findAll();
    }

    public Assignment require(long assignmentId) {
        return assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new ValidationException("作业不存在或已被删除"));
    }

    public List<RubricItem> rubricItems(long assignmentId) {
        return assignmentDao.findRubricItems(assignmentId);
    }

    /** 新建作业（默认草稿状态）。 */
    public Assignment create(User teacher, long courseId, Assignment draft) {
        requireTeacher(teacher);
        Course course = courseDao.findById(courseId)
                .orElseThrow(() -> new ValidationException("课程不存在"));
        if (course.getTeacherId() != teacher.getId()) {
            throw new ValidationException("只能给自己开设的课程布置作业");
        }
        validate(draft);
        draft.setCourseId(courseId);
        draft.setStatus(AssignmentStatus.DRAFT);
        draft.setCreatedAt(LocalDateTime.now());
        long id = assignmentDao.insert(draft);
        draft.setId(id);
        AppLogger.get().info("创建作业：" + draft.getTitle() + "（" + draft.getRubricItems().size() + " 条细则）");
        return draft;
    }

    public void update(User teacher, Assignment assignment) {
        requireTeacher(teacher);
        Assignment existing = require(assignment.getId());
        if (existing.getCourseId() != assignment.getCourseId()) {
            throw new ValidationException("不能把作业移动到其他课程");
        }
        validate(assignment);
        assignmentDao.update(assignment);
        AppLogger.get().info("修改作业：" + assignment.getTitle());
    }

    /** 状态流转，非法流转（如草稿直接关闭）会被拒绝。 */
    public void changeStatus(User teacher, long assignmentId, AssignmentStatus target) {
        requireTeacher(teacher);
        Assignment assignment = require(assignmentId);
        AssignmentStatus current = assignment.getStatus();
        if (current == target) {
            return;
        }
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new ValidationException("当前状态「" + current.label() + "」不能变更为「" + target.label() + "」");
        }
        assignmentDao.updateStatus(assignmentId, target);
        AppLogger.get().info("作业状态变更：" + assignment.getTitle() + " " + current.label() + " → " + target.label());
    }

    /** 删除作业：按「评分类 → 任务 → 提交 → 作业」顺序清理，避免留下孤儿数据。 */
    public void deleteCascade(long assignmentId) {
        reviewDao.deleteByAssignment(assignmentId);
        reviewTaskDao.deleteByAssignment(assignmentId);
        submissionDao.deleteByAssignment(assignmentId);
        assignmentDao.delete(assignmentId);
        AppLogger.get().info("删除作业 id=" + assignmentId + " 及其关联数据");
    }

    public void delete(User teacher, long assignmentId) {
        requireTeacher(teacher);
        Assignment assignment = require(assignmentId);
        deleteCascade(assignmentId);
        AppLogger.get().info("教师删除作业：" + assignment.getTitle());
    }

    /** 校验作业字段与评分细则，供服务层与界面层共用。 */
    public void validate(Assignment assignment) {
        assignment.setTitle(Text.requireText(assignment.getTitle(), "作业名称", 128));
        assignment.setDescription(Text.requireText(assignment.getDescription(), "作业要求", 2000));
        if (assignment.getReviewersPerSubmission() < 1 || assignment.getReviewersPerSubmission() > 10) {
            throw new ValidationException("每份作业的评审人数应在 1 ~ 10 之间");
        }
        validateRubric(assignment.getRubricItems());
    }

    /** 细则校验委托给领域校验器，保证界面、服务与测试共用同一套规则。 */
    public void validateRubric(List<RubricItem> items) {
        RubricValidator.validate(items);
    }

    private void requireTeacher(User user) {
        if (!user.isTeacher()) {
            throw new ValidationException("只有教师可以维护作业");
        }
    }
}
