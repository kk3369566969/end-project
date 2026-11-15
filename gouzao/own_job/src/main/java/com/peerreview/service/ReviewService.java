package com.peerreview.service;

import com.peerreview.common.AppLogger;
import com.peerreview.common.RegexTextMatcher;
import com.peerreview.common.Text;
import com.peerreview.common.ValidationException;
import com.peerreview.dao.AssignmentDao;
import com.peerreview.dao.ReviewDao;
import com.peerreview.dao.ReviewTaskDao;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.Review;
import com.peerreview.domain.ReviewRow;
import com.peerreview.domain.ReviewScore;
import com.peerreview.domain.ReviewTask;
import com.peerreview.domain.RubricItem;
import com.peerreview.domain.TaskRow;
import com.peerreview.domain.TaskStatus;
import com.peerreview.domain.User;
import com.peerreview.domain.scoring.ScoreCalculators;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 互评打分服务：校验 → 计算加权总分 → 落库 → 推进任务状态，一次调用完成。
 */
public final class ReviewService {

    /** 评语过短通常意味着「空泛评语」，因此设置下限。 */
    public static final int MIN_COMMENT_LENGTH = 10;
    public static final int MAX_COMMENT_LENGTH = 2000;

    private final AssignmentDao assignmentDao;
    private final ReviewTaskDao reviewTaskDao;
    private final ReviewDao reviewDao;

    public ReviewService(AssignmentDao assignmentDao,
                         ReviewTaskDao reviewTaskDao,
                         ReviewDao reviewDao) {
        this.assignmentDao = assignmentDao;
        this.reviewTaskDao = reviewTaskDao;
        this.reviewDao = reviewDao;
    }

    /** 学生的待评/已评任务列表，支持按状态与正则关键词过滤。 */
    public List<TaskRow> myTasks(long reviewerId, TaskStatus statusFilter, RegexTextMatcher matcher) {
        List<TaskRow> rows = reviewTaskDao.findRowsByReviewer(reviewerId);
        List<TaskRow> filtered = new ArrayList<>();
        for (TaskRow row : rows) {
            if (statusFilter != null && row.status() != statusFilter) {
                continue;
            }
            if (!matcher.matchesAny(row.assignmentTitle(), row.courseName(), row.content())) {
                continue;
            }
            filtered.add(row);
        }
        return filtered;
    }

    public Assignment assignmentOf(long assignmentId) {
        return assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new ValidationException("作业不存在或已被删除"));
    }

    public ReviewTask requireTask(long taskId) {
        return reviewTaskDao.findById(taskId)
                .orElseThrow(() -> new ValidationException("互评任务不存在"));
    }

    public Optional<Review> reviewOfTask(long taskId) {
        return reviewDao.findByTask(taskId);
    }

    public List<ReviewRow> reviewsOfSubmission(long submissionId) {
        return reviewDao.findRowsBySubmission(submissionId);
    }

    /** 界面实时预览用的总分计算（与最终落库使用同一策略，避免预览与结果不一致）。 */
    public double previewTotal(Assignment assignment, Map<Long, Double> scores) {
        return ScoreCalculators.of(assignment.getScoringMode())
                .calculate(assignment.getRubricItems(), scores);
    }

    /**
     * 提交互评。
     *
     * @param scores  维度 id → 得分
     * @param comment 人工评语（必填，且不能空泛）
     */
    public Review submit(User reviewer, long taskId, Map<Long, Double> scores, String comment) {
        ReviewTask task = requireTask(taskId);
        if (task.getReviewerId() != reviewer.getId()) {
            throw new ValidationException("无权提交该互评任务");
        }
        if (task.isCompleted()) {
            throw new ValidationException("该互评任务已完成，不能重复提交");
        }
        Assignment assignment = assignmentOf(task.getAssignmentId());
        List<RubricItem> items = assignment.getRubricItems();
        if (items.isEmpty()) {
            throw new ValidationException("该作业尚未配置评分细则");
        }

        Map<Long, Double> safeScores = validateScores(items, scores);
        String safeComment = Text.trimToEmpty(comment);
        if (safeComment.isEmpty()) {
            throw new ValidationException("请填写评语，说明打分依据");
        }
        if (safeComment.length() < MIN_COMMENT_LENGTH) {
            throw new ValidationException("评语过于简短，请至少填写 " + MIN_COMMENT_LENGTH + " 个字符");
        }
        if (safeComment.length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("评语不能超过 " + MAX_COMMENT_LENGTH + " 个字符");
        }

        double total = ScoreCalculators.of(assignment.getScoringMode()).calculate(items, safeScores);
        LocalDateTime now = LocalDateTime.now();

        Review review = new Review();
        review.setTaskId(taskId);
        review.setSubmissionId(task.getSubmissionId());
        review.setReviewerId(reviewer.getId());
        review.setTotalScore(total);
        review.setComment(safeComment);
        review.setCreatedAt(now);
        review.setScores(toScoreRecords(items, safeScores));
        review.setId(reviewDao.insert(review));

        reviewTaskDao.markCompleted(taskId, now);
        AppLogger.get().info("互评完成：taskId=" + taskId + "，评审人=" + reviewer.getUsername()
                + "，总分=" + RubricItem.trim(total));
        return review;
    }

    /** 校验每个维度都已打分且落在区间内，返回防御性拷贝。 */
    private Map<Long, Double> validateScores(List<RubricItem> items, Map<Long, Double> scores) {
        if (scores == null || scores.isEmpty()) {
            throw new ValidationException("请为每个评分维度打分");
        }
        Map<Long, Double> safe = new LinkedHashMap<>();
        for (RubricItem item : items) {
            Double score = scores.get(item.getId());
            if (score == null) {
                throw new ValidationException("请为「" + item.getName() + "」打分");
            }
            if (Double.isNaN(score) || Double.isInfinite(score)) {
                throw new ValidationException("维度「" + item.getName() + "」的得分非法");
            }
            if (score < item.getMinScore() - 1e-6 || score > item.getMaxScore() + 1e-6) {
                throw new ValidationException("维度「" + item.getName() + "」的得分应在 "
                        + item.rangeLabel() + " 之间");
            }
            safe.put(item.getId(), score);
        }
        return safe;
    }

    private List<ReviewScore> toScoreRecords(List<RubricItem> items, Map<Long, Double> scores) {
        List<ReviewScore> records = new ArrayList<>();
        for (RubricItem item : items) {
            records.add(new ReviewScore(item.getId(), item.getName(), scores.get(item.getId())));
        }
        return records;
    }

    /** 学生已完成互评的份数（用于首页卡片）。 */
    public int completedCount(long reviewerId) {
        return (int) reviewTaskDao.findRowsByReviewer(reviewerId).stream()
                .filter(TaskRow::isCompleted)
                .count();
    }
}
