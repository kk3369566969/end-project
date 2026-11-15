package com.peerreview.domain;

/** 系统角色：教师负责课程/作业/互评分配与统计，学生负责提交作业与完成互评。 */
public enum Role {

    TEACHER("教师"),
    STUDENT("学生");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 与数据库存储值一致的稳定编码。 */
    public String code() {
        return name();
    }

    public static Role fromCode(String code) {
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知角色：" + code);
    }
}
