package com.peerreview.service;

import com.peerreview.common.ValidationException;
import com.peerreview.dao.UserDao;
import com.peerreview.domain.Role;
import com.peerreview.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 注册规则测试。
 *
 * <p>用内存 DAO 而不是真实数据库：这些用例关心的是「哪条规则先拦、提示说什么」，
 * 与 SQL 无关；跑得快，也不会因为测试库状态互相影响。</p>
 */
class AuthServiceTest {

    private static final String INVITE = "TEACH2026";

    private final InMemoryUserDao dao = new InMemoryUserDao();
    private final AuthService auth = new AuthService(dao, INVITE);

    @Test
    @DisplayName("学生注册成功：角色为学生、口令以摘要保存、主键已回填")
    void registerStudent() {
        User user = auth.register(RegistrationRequest.student("2023007", "林一诺", "abc123", "abc123"));

        assertTrue(user.getId() > 0, "应回填自增主键");
        assertEquals(Role.STUDENT, user.getRole());
        assertEquals("林一诺", user.getDisplayName());
        assertNotNull(user.getPasswordHash());
        assertTrue(user.getPasswordHash().length() >= 32, "不应保存明文口令");
        assertNotEquals("abc123", user.getPasswordHash(), "口令摘要不应等于明文");
    }

    @Test
    @DisplayName("注册后可用同一账号口令登录")
    void registeredUserCanLogin() {
        auth.register(RegistrationRequest.student("2023008", "周与安", "abc123", "abc123"));

        User loggedIn = auth.login("2023008", "abc123");
        assertEquals("周与安", loggedIn.getDisplayName());
    }

    @Test
    @DisplayName("账号重复时提示已被注册")
    void rejectDuplicateUsername() {
        auth.register(RegistrationRequest.student("2023009", "吴桐", "abc123", "abc123"));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> auth.register(RegistrationRequest.student("2023009", "另一个人", "abc123", "abc123")));
        assertTrue(ex.getMessage().contains("已被注册"), ex.getMessage());
    }

    @Test
    @DisplayName("两次密码不一致")
    void rejectPasswordMismatch() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> auth.register(RegistrationRequest.student("2023010", "郑小满", "abc123", "abc124")));
        assertTrue(ex.getMessage().contains("不一致"), ex.getMessage());
    }

    @Test
    @DisplayName("密码短于 6 位")
    void rejectShortPassword() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> auth.register(RegistrationRequest.student("2023011", "何舟", "12345", "12345")));
        assertTrue(ex.getMessage().contains("至少"), ex.getMessage());
    }

    @Test
    @DisplayName("密码与账号相同")
    void rejectPasswordSameAsUsername() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> auth.register(RegistrationRequest.student("2023012", "许南", "2023012", "2023012")));
        assertTrue(ex.getMessage().contains("不能与账号相同"), ex.getMessage());
    }

    @Test
    @DisplayName("全空白的密码被拒绝")
    void rejectBlankPassword() {
        assertThrows(ValidationException.class,
                () -> auth.register(RegistrationRequest.student("2023013", "谢云", "      ", "      ")));
    }

    @Test
    @DisplayName("账号格式非法：过短、含中文、含空格")
    void rejectInvalidUsername() {
        assertThrows(ValidationException.class,
                () -> auth.register(RegistrationRequest.student("123", "短账号", "abc123", "abc123")));
        assertThrows(ValidationException.class,
                () -> auth.register(RegistrationRequest.student("张三2023", "中文账号", "abc123", "abc123")));
        assertThrows(ValidationException.class,
                () -> auth.register(RegistrationRequest.student("2023 001", "含空格", "abc123", "abc123")));
        assertFalseRegistered("123", "张三2023", "2023 001");
    }

    @Test
    @DisplayName("姓名为空被拒绝")
    void rejectBlankDisplayName() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> auth.register(RegistrationRequest.student("2023014", "  ", "abc123", "abc123")));
        assertTrue(ex.getMessage().contains("姓名"), ex.getMessage());
    }

    @Test
    @DisplayName("教师注册：邀请码错误被拒绝，正确则通过")
    void teacherInviteCode() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> auth.register(RegistrationRequest.teacher("T2026", "错误码老师", "abc123", "abc123", "WRONG")));
        assertTrue(ex.getMessage().contains("邀请码"), ex.getMessage());

        User teacher = auth.register(
                RegistrationRequest.teacher("T2026", "苏老师", "abc123", "abc123", INVITE));
        assertEquals(Role.TEACHER, teacher.getRole());
    }

    @Test
    @DisplayName("学生注册不受邀请码限制")
    void studentIgnoresInviteCode() {
        User student = auth.register(
                new RegistrationRequest("2023015", "钱多", "abc123", "abc123", Role.STUDENT, "随便填"));
        assertEquals(Role.STUDENT, student.getRole());
    }

    @Test
    @DisplayName("邀请码配置为空时关闭教师自助注册")
    void teacherRegistrationDisabledWhenCodeBlank() {
        AuthService closed = new AuthService(dao, "");
        ValidationException ex = assertThrows(ValidationException.class,
                () -> closed.register(RegistrationRequest.teacher("T2027", "某老师", "abc123", "abc123", "")));
        assertTrue(ex.getMessage().contains("已关闭"), ex.getMessage());
    }

    @Test
    @DisplayName("角色缺省为学生，界面漏传时不会创建教师")
    void defaultRoleIsStudent() {
        User user = auth.register(
                new RegistrationRequest("2023016", "孙未填", "abc123", "abc123", null, ""));
        assertEquals(Role.STUDENT, user.getRole());
    }

    private void assertFalseRegistered(String... usernames) {
        for (String username : usernames) {
            assertTrue(dao.findByUsername(username).isEmpty(), username + " 不应被写入");
        }
    }

    /** 内存实现：只做注册需要的那几个操作，避免测试依赖数据库。 */
    private static final class InMemoryUserDao implements UserDao {

        private final Map<String, User> users = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(users.get(username));
        }

        @Override
        public Optional<User> findById(long id) {
            return users.values().stream().filter(u -> u.getId() == id).findFirst();
        }

        @Override
        public List<User> findByRole(Role role) {
            return users.values().stream().filter(u -> u.getRole() == role).toList();
        }

        @Override
        public boolean existsByUsername(String username) {
            return users.containsKey(username);
        }

        @Override
        public User insert(User user) {
            user.setId(nextId++);
            users.put(user.getUsername(), user);
            return user;
        }
    }
}
