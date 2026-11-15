package com.peerreview.domain.allocation;

import com.peerreview.common.ValidationException;
import com.peerreview.domain.ReviewTask;
import com.peerreview.domain.Submission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 随机分配逻辑白盒测试：边界、去重、公平性与可复现性。
 */
@DisplayName("互评任务随机分配")
class RandomRingAllocatorTest {

    private final ReviewAllocator allocator = new RandomRingAllocator();

    private List<Submission> submissions(int count) {
        List<Submission> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Submission submission = new Submission();
            submission.setId(100L + i);
            submission.setStudentId(i);
            submission.setAssignmentId(1);
            list.add(submission);
        }
        return list;
    }

    @Test
    @DisplayName("每份提交恰好被 N 人评审，且总任务数为 提交数 × N")
    void taskCountMatchesExpectation() {
        List<Submission> data = submissions(10);
        List<ReviewTask> tasks = allocator.allocate(1, data, 3, new Random(42));
        assertEquals(30, tasks.size());

        Map<Long, Integer> perSubmission = new HashMap<>();
        for (ReviewTask task : tasks) {
            perSubmission.merge(task.getSubmissionId(), 1, Integer::sum);
        }
        assertEquals(10, perSubmission.size());
        perSubmission.values().forEach(count -> assertEquals(3, count));
    }

    @Test
    @DisplayName("不会自己评自己，也不会让同一人重复评审同一份作业")
    void noSelfReviewAndNoDuplicates() {
        List<Submission> data = submissions(8);
        List<ReviewTask> tasks = allocator.allocate(1, data, 4, new Random(7));

        Map<Long, Long> authorOfSubmission = new HashMap<>();
        data.forEach(submission -> authorOfSubmission.put(submission.getId(), submission.getStudentId()));

        Set<String> pairs = new HashSet<>();
        for (ReviewTask task : tasks) {
            long authorId = authorOfSubmission.get(task.getSubmissionId());
            assertNotEquals(authorId, task.getReviewerId(), "出现了自评任务");
            assertTrue(pairs.add(task.getSubmissionId() + "-" + task.getReviewerId()), "出现重复分配");
        }
    }

    @Test
    @DisplayName("公平性：所有学生都提交时，每人评审份数相同")
    void workloadIsBalanced() {
        List<Submission> data = submissions(9);
        List<ReviewTask> tasks = allocator.allocate(1, data, 2, new Random(2026));

        Map<Long, Long> perReviewer = new HashMap<>();
        for (ReviewTask task : tasks) {
            perReviewer.merge(task.getReviewerId(), 1L, Long::sum);
        }
        assertEquals(9, perReviewer.size());
        assertEquals(2L, perReviewer.values().stream().distinct().findFirst().orElse(-1L));
    }

    @Test
    @DisplayName("随机源固定时结果可复现")
    void reproducibleWithSameSeed() {
        List<Submission> data = submissions(6);
        List<ReviewTask> first = allocator.allocate(1, data, 2, new Random(99));
        List<ReviewTask> second = allocator.allocate(1, data, 2, new Random(99));

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).getSubmissionId(), second.get(i).getSubmissionId());
            assertEquals(first.get(i).getReviewerId(), second.get(i).getReviewerId());
        }
    }

    @Test
    @DisplayName("边界：评审人数等于 提交数-1 时允许，超过则拒绝")
    void boundaryCheck() {
        List<Submission> data = submissions(3);
        assertEquals(6, allocator.allocate(1, data, 2, new Random(1)).size());
        assertThrows(ValidationException.class, () -> allocator.allocate(1, data, 3, new Random(1)));
        assertThrows(ValidationException.class, () -> allocator.allocate(1, submissions(1), 1, new Random(1)));
        assertThrows(ValidationException.class, () -> allocator.allocate(1, data, 0, new Random(1)));
    }
}
