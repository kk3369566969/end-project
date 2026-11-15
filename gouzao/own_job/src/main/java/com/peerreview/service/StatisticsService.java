package com.peerreview.service;

import com.peerreview.common.AppLogger;
import com.peerreview.common.CsvWriter;
import com.peerreview.common.Dates;
import com.peerreview.common.DataAccessException;
import com.peerreview.common.RegexTextMatcher;
import com.peerreview.dao.ReviewDao;
import com.peerreview.dao.SubmissionDao;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.AssignmentSummary;
import com.peerreview.domain.ReviewRow;
import com.peerreview.domain.SubmissionRow;
import com.peerreview.domain.SubmissionStats;
import com.peerreview.service.query.SubmissionFilter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计与导出服务：把「一份作业收到的多条互评」聚合成教师可用的成绩视图。
 */
public final class StatisticsService {

    private final SubmissionDao submissionDao;
    private final ReviewDao reviewDao;

    public StatisticsService(SubmissionDao submissionDao, ReviewDao reviewDao) {
        this.submissionDao = submissionDao;
        this.reviewDao = reviewDao;
    }

    /**
     * 逐份提交统计：评审份数、平均分、最高分、最低分。
     *
     * <p>按平均分从高到低排序，未收到互评的提交排在最后（便于教师催评）。</p>
     */
    public List<SubmissionStats> statsFor(long assignmentId) {
        List<SubmissionRow> submissions = submissionDao.findRowsByAssignment(assignmentId);
        Map<Long, List<ReviewRow>> reviewsBySubmission = groupBySubmission(reviewDao.findRowsByAssignment(assignmentId));

        List<SubmissionStats> stats = new ArrayList<>();
        for (SubmissionRow submission : submissions) {
            List<ReviewRow> reviews = reviewsBySubmission.getOrDefault(submission.submissionId(), List.of());
            stats.add(aggregate(submission, reviews));
        }
        stats.sort(Comparator.comparingInt(SubmissionStats::counted).reversed()
                .thenComparing(Comparator.comparingDouble(SubmissionStats::average).reversed())
                .thenComparing(SubmissionStats::studentName));
        return stats;
    }

    /** 作业级概览：提交数、互评条数、平均/最高/最低（以「每份提交的平均分」为单位）。 */
    public AssignmentSummary summaryFor(long assignmentId) {
        List<SubmissionStats> stats = statsFor(assignmentId);
        List<SubmissionStats> reviewed = stats.stream().filter(s -> s.counted() > 0).toList();
        int reviewCount = reviewed.stream().mapToInt(SubmissionStats::counted).sum();
        if (reviewed.isEmpty()) {
            return new AssignmentSummary(stats.size(), 0, 0, 0, 0);
        }
        double average = reviewed.stream().mapToDouble(SubmissionStats::average).average().orElse(0);
        double max = reviewed.stream().mapToDouble(SubmissionStats::average).max().orElse(0);
        double min = reviewed.stream().mapToDouble(SubmissionStats::average).min().orElse(0);
        return new AssignmentSummary(stats.size(), reviewCount, average, max, min);
    }

    /** 教师端按课程/作业/正则过滤提交列表。 */
    public List<SubmissionRow> searchSubmissions(SubmissionFilter filter) {
        List<SubmissionRow> all = submissionDao.findAllRows();
        if (filter == null || filter.isEmpty()) {
            return all;
        }
        return all.stream().filter(filter::accepts).toList();
    }

    /** 正则检索某次作业下的评语（用于教师端「按评语关键词检索」）。 */
    public List<ReviewRow> searchReviews(long assignmentId, RegexTextMatcher matcher) {
        List<ReviewRow> rows = reviewDao.findRowsByAssignment(assignmentId);
        if (matcher == null || !matcher.isActive()) {
            return rows;
        }
        return rows.stream().filter(row -> matcher.matches(row.comment())).toList();
    }

    /**
     * 导出成绩汇总 CSV。
     *
     * @return 实际写入的文件路径（便于界面提示）
     */
    public Path exportCsv(Assignment assignment, Path target) {
        List<SubmissionStats> stats = statsFor(assignment.getId());
        Map<Long, SubmissionRow> submissions = new LinkedHashMap<>();
        for (SubmissionRow row : submissionDao.findRowsByAssignment(assignment.getId())) {
            submissions.put(row.submissionId(), row);
        }
        List<String> headers = List.of("课程", "作业", "学生", "提交时间", "最近更新",
                "评审份数", "平均分", "最高分", "最低分");
        List<List<String>> rows = new ArrayList<>();
        for (SubmissionStats stat : stats) {
            SubmissionRow submission = submissions.get(stat.submissionId());
            rows.add(List.of(
                    submission == null ? "" : submission.courseName(),
                    assignment.getTitle(),
                    stat.studentName(),
                    submission == null ? "" : Dates.format(submission.submittedAt()),
                    submission == null ? "" : Dates.format(submission.updatedAt()),
                    String.valueOf(stat.counted()),
                    stat.counted() == 0 ? "" : stat.averageLabel(),
                    stat.counted() == 0 ? "" : stat.maxLabel(),
                    stat.counted() == 0 ? "" : stat.minLabel()));
        }
        try {
            CsvWriter.write(target, headers, rows);
        } catch (IOException ex) {
            throw new DataAccessException("导出失败：" + ex.getMessage(), ex);
        }
        AppLogger.get().info("导出成绩 CSV：" + target.toAbsolutePath() + "（" + rows.size() + " 行）");
        return target;
    }

    private Map<Long, List<ReviewRow>> groupBySubmission(List<ReviewRow> reviews) {
        Map<Long, List<ReviewRow>> grouped = new LinkedHashMap<>();
        for (ReviewRow review : reviews) {
            grouped.computeIfAbsent(review.submissionId(), key -> new ArrayList<>()).add(review);
        }
        return grouped;
    }

    private SubmissionStats aggregate(SubmissionRow submission, List<ReviewRow> reviews) {
        if (reviews.isEmpty()) {
            return SubmissionStats.empty(submission.submissionId(), submission.studentName());
        }
        double sum = 0;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for (ReviewRow review : reviews) {
            sum += review.totalScore();
            max = Math.max(max, review.totalScore());
            min = Math.min(min, review.totalScore());
        }
        return new SubmissionStats(submission.submissionId(), submission.studentName(),
                reviews.size(), sum / reviews.size(), max, min);
    }
}
