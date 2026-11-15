package com.peerreview.domain;

import java.time.LocalDateTime;

/**
 * 学生对某个作业的一次提交。同一学生对同一作业只保留一份提交，
 * 因此 (assignmentId, studentId) 在数据库层是唯一约束。
 */
public class Submission {

    private long id;
    private long assignmentId;
    private long studentId;
    private String content;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    /** 非持久化字段：列表展示用。 */
    private String studentName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long studentId) {
        this.studentId = studentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    /** 提交后是否被修改过。 */
    public boolean isEdited() {
        return submittedAt != null && updatedAt != null && !updatedAt.equals(submittedAt);
    }
}
