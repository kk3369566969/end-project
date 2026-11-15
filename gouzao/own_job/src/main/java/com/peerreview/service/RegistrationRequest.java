package com.peerreview.service;

import com.peerreview.domain.Role;

/**
 * 注册请求：界面采集到的原始输入。
 *
 * <p>用 record 承载而不是一串参数，一是避免「四个 String 传错顺序」这类低级错误，
 * 二是让校验逻辑的入参保持自解释。</p>
 */
public record RegistrationRequest(String username,
                                  String displayName,
                                  String password,
                                  String confirmPassword,
                                  Role role,
                                  String inviteCode) {

    /** 学生自助注册：不需要邀请码。 */
    public static RegistrationRequest student(String username, String displayName,
                                              String password, String confirmPassword) {
        return new RegistrationRequest(username, displayName, password, confirmPassword, Role.STUDENT, "");
    }

    /** 教师注册：必须携带邀请码。 */
    public static RegistrationRequest teacher(String username, String displayName,
                                              String password, String confirmPassword, String inviteCode) {
        return new RegistrationRequest(username, displayName, password, confirmPassword, Role.TEACHER, inviteCode);
    }

    /** 角色缺省为学生，避免界面漏传时静默创建出教师账号。 */
    public Role roleOrStudent() {
        return role == null ? Role.STUDENT : role;
    }
}
