package com.peerreview.domain.allocation;

import com.peerreview.common.ValidationException;
import com.peerreview.domain.ReviewTask;
import com.peerreview.domain.Submission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 随机轮转分配器：先把提交顺序随机打乱形成一个「环」，再让每个评审人顺延领取后面的作业。
 *
 * <p>相比「对每份提交独立随机抽 N 个人」，环状顺延有两个好处：</p>
 * <ol>
 *   <li>天然避免自己评自己，且同一份作业不会被同一人重复领到；</li>
 *   <li>当每人都提交时，每人评审的份数严格等于 N，避免有人轻松有人爆量的不公平。</li>
 * </ol>
 */
public final class RandomRingAllocator implements ReviewAllocator {

    @Override
    public List<ReviewTask> allocate(long assignmentId,
                                     List<Submission> submissions,
                                     int reviewersPerSubmission,
                                     Random random) {
        if (submissions == null || submissions.size() < 2) {
            throw new ValidationException("至少需要 2 份提交才能发起互评分配");
        }
        if (reviewersPerSubmission < 1) {
            throw new ValidationException("每份作业的评审人数至少为 1");
        }
        int total = submissions.size();
        if (reviewersPerSubmission > total - 1) {
            throw new ValidationException(
                    "评审人数不能超过提交总数减 1（当前提交 " + total + " 份，最多 " + (total - 1) + " 人评审）");
        }

        List<Submission> ring = new ArrayList<>(submissions);
        Collections.shuffle(ring, random);

        List<ReviewTask> tasks = new ArrayList<>(total * reviewersPerSubmission);
        for (Submission submission : ring) {
            int authorPosition = indexOfAuthor(ring, submission.getStudentId());
            for (int step = 1; step <= reviewersPerSubmission; step++) {
                int reviewerPosition = (authorPosition + step) % total;
                long reviewerId = ring.get(reviewerPosition).getStudentId();
                tasks.add(new ReviewTask(assignmentId, submission.getId(), reviewerId));
            }
        }
        return tasks;
    }

    private int indexOfAuthor(List<Submission> ring, long studentId) {
        for (int i = 0; i < ring.size(); i++) {
            if (ring.get(i).getStudentId() == studentId) {
                return i;
            }
        }
        throw new ValidationException("提交数据异常：未在提交列表中找到作者");
    }
}
