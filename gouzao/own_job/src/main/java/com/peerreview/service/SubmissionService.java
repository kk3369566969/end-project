package com.peerreview.service;

import com.peerreview.common.AppLogger;
import com.peerreview.common.Text;
import com.peerreview.common.ValidationException;
import com.peerreview.dao.AssignmentDao;
import com.peerreview.dao.SubmissionDao;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.Submission;
import com.peerreview.domain.SubmissionRow;
import com.peerreview.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 作业提交服务。
 *
 * <p>防御性编程要点：空提交、超长提交、过期提交、越权提交都会被拦截并给出明确提示；
 * 同一学生对同一作业只保留一条记录，重复提交视为修改。</p>
 */
public final class SubmissionService {

    public static final int MIN_CONTENT_LENGTH = 20;
    public static final int MAX_CONTENT_LENGTH = 20000;

    private final AssignmentDao assignmentDao;
    private final SubmissionDao submissionDao;

    public SubmissionService(AssignmentDao assignmentDao, SubmissionDao submissionDao) {
        this.assignmentDao = assignmentDao;
        this.submissionDao = submissionDao;
    }

    /**
     * 提交或更新作业内容。
     *
     * @return 保存后的提交记录
     */
    public Submission submit(User student, long assignmentId, String content) {
        if (student.isTeacher()) {
            throw new ValidationException("教师账号不能提交作业");
        }
        Assignment assignment = assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new ValidationException("作业不存在或已被删除"));
        if (!assignment.getStatus().acceptsSubmission()) {
            throw new ValidationException("当前作业状态为「" + assignment.getStatus().label() + "」，不接受提交");
        }
        if (assignment.isOverdue()) {
            throw new ValidationException("作业已于 " + com.peerreview.common.Dates.format(assignment.getDeadline())
                    + " 截止，无法再提交");
        }
        String safeContent = Text.trimToEmpty(content);
        int length = safeContent.length();
        if (length == 0) {
            throw new ValidationException("提交内容不能为空");
        }
        if (length < MIN_CONTENT_LENGTH) {
            throw new ValidationException("提交内容过短，请至少填写 " + MIN_CONTENT_LENGTH + " 个字符");
        }
        if (length > MAX_CONTENT_LENGTH) {
            throw new ValidationException("提交内容不能超过 " + MAX_CONTENT_LENGTH + " 个字符");
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<Submission> existing = submissionDao.findByAssignmentAndStudent(assignmentId, student.getId());
        if (existing.isPresent()) {
            Submission submission = existing.get();
            submission.setContent(safeContent);
            submission.setUpdatedAt(now);
            submissionDao.update(submission);
            AppLogger.get().info("学生 " + student.getUsername() + " 更新了作业提交：assignmentId=" + assignmentId);
            return submission;
        }
        Submission submission = new Submission();
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(student.getId());
        submission.setContent(safeContent);
        submission.setSubmittedAt(now);
        submission.setUpdatedAt(now);
        submission.setId(submissionDao.insert(submission));
        AppLogger.get().info("学生 " + student.getUsername() + " 提交作业：assignmentId=" + assignmentId);
        return submission;
    }

    public Optional<Submission> findMine(long assignmentId, long studentId) {
        return submissionDao.findByAssignmentAndStudent(assignmentId, studentId);
    }

    public List<SubmissionRow> listRows(long assignmentId) {
        return submissionDao.findRowsByAssignment(assignmentId);
    }

    public List<SubmissionRow> listAllRows() {
        return submissionDao.findAllRows();
    }

    public int countByAssignment(long assignmentId) {
        return submissionDao.countByAssignment(assignmentId);
    }

    /** 展示用字数（不含空白字符）。 */
    public int visibleLength(String content) {
        return Text.countVisibleChars(content);
    }
}
