package com.peerreview.service;

import com.peerreview.common.AppLogger;
import com.peerreview.common.DataAccessException;
import com.peerreview.common.Passwords;
import com.peerreview.common.Text;
import com.peerreview.common.ValidationException;
import com.peerreview.dao.UserDao;
import com.peerreview.domain.Role;
import com.peerreview.domain.User;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * 登录、注册与身份校验。
 *
 * <p>登录刻意不抛「用户名不存在」这类可枚举信息，统一提示「用户名或密码错误」；
 * 注册则相反——必须明确指出是账号已占用还是密码不合规，否则用户无法完成操作。</p>
 */
public final class AuthService {

    /** 学号/工号：4~20 位字母、数字或下划线，既容纳纯数字学号，也容纳 T2026 这类工号。 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{4,20}");

    static final int MIN_PASSWORD_LENGTH = 6;
    static final int MAX_PASSWORD_LENGTH = 32;
    static final int MAX_DISPLAY_NAME_LENGTH = 20;

    private final UserDao userDao;
    private final String teacherInviteCode;

    public AuthService(UserDao userDao, String teacherInviteCode) {
        this.userDao = userDao;
        this.teacherInviteCode = Text.trimToEmpty(teacherInviteCode);
    }

    public User login(String username, String password) {
        String name = Text.requireText(username, "用户名", 64);
        String rawPassword = Text.trimToEmpty(password);
        if (rawPassword.isEmpty()) {
            throw new ValidationException("请输入密码");
        }
        User user = userDao.findByUsername(name)
                .orElseThrow(() -> new ValidationException("用户名或密码错误"));
        if (!Passwords.matches(rawPassword, user.getPasswordHash())) {
            throw new ValidationException("用户名或密码错误");
        }
        AppLogger.get().info("登录成功：" + user.getUsername() + "（" + user.getRole().label() + "）");
        return user;
    }

    /**
     * 注册新账号。
     *
     * <p>校验顺序：账号格式 → 姓名 → 密码（确认一致、长度、与账号不同）→ 教师邀请码 → 账号是否占用。
     * 把「格式类」错误放在「占用类」之前，用户先改自己的输入，不必反复试用户名。</p>
     *
     * @return 已落库、带自增主键的用户
     * @throws ValidationException 任一规则不满足，message 可直接展示
     */
    public User register(RegistrationRequest request) {
        Role role = request.roleOrStudent();
        String username = validateUsername(request.username());
        String displayName = Text.requireText(request.displayName(), "姓名", MAX_DISPLAY_NAME_LENGTH);
        String password = validatePassword(request.password(), request.confirmPassword(), username);
        verifyTeacherInvite(role, request.inviteCode());

        if (userDao.existsByUsername(username)) {
            throw new ValidationException("账号「" + username + "」已被注册，请换一个");
        }

        User user = new User(username, displayName, role);
        user.setPasswordHash(Passwords.hash(password));
        try {
            User saved = userDao.insert(user);
            AppLogger.get().info("注册成功：" + username + "（" + role.label() + "，姓名 " + displayName + "）");
            return saved;
        } catch (DataAccessException ex) {
            // 两个请求同时通过存在性检查时，靠唯一约束兜底
            if (isDuplicateKey(ex)) {
                throw new ValidationException("账号「" + username + "」已被注册，请换一个");
            }
            throw ex;
        }
    }

    private String validateUsername(String raw) {
        String username = Text.trimToEmpty(raw);
        if (username.isEmpty()) {
            throw new ValidationException("请输入学号或工号");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException("学号/工号应为 4~20 位字母、数字或下划线");
        }
        return username;
    }

    /**
     * 口令规则。
     *
     * <p>首尾空白不参与摘要（与登录保持一致），所以长度按原文校验、但「全为空白」直接拒绝，
     * 避免用户以为设了密码、实际存的是空串。</p>
     */
    private String validatePassword(String raw, String confirm, String username) {
        String password = raw == null ? "" : raw;
        if (password.trim().isEmpty()) {
            throw new ValidationException("请输入密码");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("密码至少 " + MIN_PASSWORD_LENGTH + " 位");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("密码最多 " + MAX_PASSWORD_LENGTH + " 位");
        }
        if (!password.equals(confirm == null ? "" : confirm)) {
            throw new ValidationException("两次输入的密码不一致");
        }
        if (password.equalsIgnoreCase(username)) {
            throw new ValidationException("密码不能与账号相同");
        }
        return password;
    }

    private void verifyTeacherInvite(Role role, String inviteCode) {
        if (role != Role.TEACHER) {
            return;
        }
        if (teacherInviteCode.isEmpty()) {
            throw new ValidationException("教师账号已关闭自助注册，请联系管理员创建");
        }
        if (!teacherInviteCode.equals(Text.trimToEmpty(inviteCode))) {
            throw new ValidationException("教师邀请码不正确");
        }
    }

    /** 唯一约束冲突：MySQL 为 23000，H2 为 23505，都以 23 开头。 */
    private boolean isDuplicateKey(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException sql
                    && sql.getSQLState() != null && sql.getSQLState().startsWith("23")) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }
}
