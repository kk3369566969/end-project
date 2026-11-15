package com.peerreview.domain;

import java.time.LocalDateTime;

/** 系统用户（教师或学生）。口令只以摘要形式保存。 */
public class User {

    private long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private Role role;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(String username, String displayName, Role role) {
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isTeacher() {
        return role == Role.TEACHER;
    }

    /** 头像/侧栏展示用的短名。 */
    public String shortName() {
        return displayName == null || displayName.isEmpty() ? username : displayName;
    }
}
