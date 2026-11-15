package com.peerreview.domain;

import java.time.LocalDateTime;

/** 课程，例如「软件构造 / 2026 秋」。 */
public class Course {

    private long id;
    private String code;
    private String name;
    private String semester;
    private long teacherId;
    private LocalDateTime createdAt;

    public Course() {
    }

    public Course(String code, String name, String semester, long teacherId) {
        this.code = code;
        this.name = name;
        this.semester = semester;
        this.teacherId = teacherId;
        this.createdAt = LocalDateTime.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(long teacherId) {
        this.teacherId = teacherId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** 列表主标题：课程名。 */
    public String title() {
        return name;
    }

    /** 列表副标题：编码 · 学期。 */
    public String subtitle() {
        return code + " · " + semester;
    }

    @Override
    public String toString() {
        return name + "（" + semester + "）";
    }
}
