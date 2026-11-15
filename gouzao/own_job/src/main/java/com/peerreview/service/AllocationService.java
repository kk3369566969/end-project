package com.peerreview.service;

import com.peerreview.common.AppLogger;
import com.peerreview.common.ValidationException;
import com.peerreview.dao.AssignmentDao;
import com.peerreview.dao.ReviewTaskDao;
import com.peerreview.dao.SubmissionDao;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.AssignmentStatus;
import com.peerreview.domain.ReviewTask;
import com.peerreview.domain.Submission;
import com.peerreview.domain.User;
import com.peerreview.domain.allocation.RandomRingAllocator;
import com.peerreview.domain.allocation.ReviewAllocator;

import java.util.List;
import java.util.Random;

/**
 * 互评任务分配服务：负责前置校验、幂等保护与状态推进，分配算法本身交给 {@link ReviewAllocator}。
 *
 * <p>「高内聚」体现为：本类只描述「什么时候可以分配、分配后状态怎么变」；
 * 「低耦合」体现为：换一种分配算法不需要改动本类。</p>
 */
public final class AllocationService {

    /** 分配结果，供界面展示。 */
    public record AllocationResult(int submissionCount, int taskCount, int reviewersPerSubmission, int skipped) {
    }

    private final AssignmentDao assignmentDao;
    private final SubmissionDao submissionDao;
    private final ReviewTaskDao reviewTaskDao;
    private final ReviewAllocator allocator;
    private final Random random;

    public AllocationService(AssignmentDao assignmentDao,
                             SubmissionDao submissionDao,
                             ReviewTaskDao reviewTaskDao,
                             ReviewAllocator allocator,
                             Random random) {
        this.assignmentDao = assignmentDao;
        this.submissionDao = submissionDao;
        this.reviewTaskDao = reviewTaskDao;
        this.allocator = allocator;
        this.random = random;
    }

    public static AllocationService withDefaultAllocator(AssignmentDao assignmentDao,
                                                         SubmissionDao submissionDao,
                                                         ReviewTaskDao reviewTaskDao) {
        return new AllocationService(assignmentDao, submissionDao, reviewTaskDao,
                new RandomRingAllocator(), new Random());
    }

    /** 该作业已生成的互评任务数，界面据此决定是否需要「重新分配」确认。 */
    public int taskCount(long assignmentId) {
        return reviewTaskDao.countByAssignment(assignmentId);
    }

    /**
     * 为作业生成互评任务。
     *
     * @param force 已有任务时是否重新分配（会清空旧任务，已完成的互评记录仍然保留在库中）
     */
    public AllocationResult allocate(User teacher, long assignmentId, boolean force) {
        if (!teacher.isTeacher()) {
            throw new ValidationException("只有教师可以发起互评分配");
        }
        Assignment assignment = assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new ValidationException("作业不存在或已被删除"));
        if (!assignment.getStatus().acceptsAllocation()) {
            throw new ValidationException("当前状态「" + assignment.getStatus().label()
                    + "」不能分配互评，请先发布作业");
        }
        List<Submission> submissions = submissionDao.listByAssignment(assignmentId);
        if (submissions.size() < 2) {
            throw new ValidationException("至少需要 2 份提交才能分配互评，当前只有 " + submissions.size() + " 份");
        }
        int existing = reviewTaskDao.countByAssignment(assignmentId);
        if (existing > 0 && !force) {
            throw new ValidationException("该作业已分配 " + existing + " 条互评任务，如需重新分配请先确认");
        }
        if (existing > 0) {
            reviewTaskDao.deleteByAssignment(assignmentId);
            AppLogger.get().info("重新分配互评：清空作业 " + assignment.getTitle() + " 的 " + existing + " 条旧任务");
        }

        List<ReviewTask> tasks = allocator.allocate(
                assignmentId, submissions, assignment.getReviewersPerSubmission(), random);

        int inserted = 0;
        int skipped = 0;
        for (ReviewTask task : tasks) {
            // 冲突检测：同一评审人重复评审同一份作业时跳过（数据库唯一约束再兜一层）
            if (reviewTaskDao.exists(task.getSubmissionId(), task.getReviewerId())) {
                skipped++;
                continue;
            }
            reviewTaskDao.insert(task);
            inserted++;
        }

        assignmentDao.updateStatus(assignmentId, AssignmentStatus.ALLOCATED);
        AppLogger.get().info("互评分配完成：作业=" + assignment.getTitle()
                + "，提交=" + submissions.size() + "，任务=" + inserted + "，跳过=" + skipped);
        return new AllocationResult(submissions.size(), inserted,
                assignment.getReviewersPerSubmission(), skipped);
    }
}
