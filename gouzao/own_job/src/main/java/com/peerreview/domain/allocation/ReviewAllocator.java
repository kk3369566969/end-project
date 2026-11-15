package com.peerreview.domain.allocation;

import com.peerreview.domain.ReviewTask;
import com.peerreview.domain.Submission;

import java.util.List;
import java.util.Random;

/**
 * 互评任务分配策略。
 *
 * <p>分配算法可能随课堂规模调整（随机轮转 / 指定分组 / 避免同班回避等），
 * 因此抽取为接口，{@code AllocationService} 只负责事务与前置校验。</p>
 */
public interface ReviewAllocator {

    /**
     * 为一次作业的全部提交生成互评任务。
     *
     * @param assignmentId          作业 id
     * @param submissions           该作业的全部提交（至少 2 份）
     * @param reviewersPerSubmission 每份提交需要被几人评审
     * @param random                随机源；测试时注入固定种子以便复现
     * @return 尚未落库的任务列表
     */
    List<ReviewTask> allocate(long assignmentId,
                              List<Submission> submissions,
                              int reviewersPerSubmission,
                              Random random);
}
