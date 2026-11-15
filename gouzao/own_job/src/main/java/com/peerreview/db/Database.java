package com.peerreview.db;

import com.peerreview.common.AppLogger;
import com.peerreview.common.DataAccessException;
import com.peerreview.common.Passwords;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据库初始化：建表 + 种子数据。
 *
 * <p>建表语句刻意保持 MySQL / H2 双兼容（只用标准类型与通用语法），
 * 这样切换到内置数据库演示时无需维护两套 DDL。</p>
 */
public final class Database {

    private static final List<String> SCHEMA = List.of(
            """
            CREATE TABLE IF NOT EXISTS app_user (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(64) NOT NULL UNIQUE,
                password_hash VARCHAR(128) NOT NULL,
                display_name VARCHAR(64) NOT NULL,
                role VARCHAR(16) NOT NULL,
                created_at TIMESTAMP NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS course (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                code VARCHAR(32) NOT NULL,
                name VARCHAR(128) NOT NULL,
                semester VARCHAR(32) NOT NULL,
                teacher_id BIGINT NOT NULL,
                created_at TIMESTAMP NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS assignment (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                course_id BIGINT NOT NULL,
                title VARCHAR(128) NOT NULL,
                description VARCHAR(2000) NOT NULL,
                deadline TIMESTAMP NULL,
                anonymous BOOLEAN NOT NULL,
                reviewers_per_submission INT NOT NULL,
                scoring_mode VARCHAR(32) NOT NULL,
                status VARCHAR(16) NOT NULL,
                created_at TIMESTAMP NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS rubric_item (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                assignment_id BIGINT NOT NULL,
                name VARCHAR(64) NOT NULL,
                weight INT NOT NULL,
                min_score DECIMAL(6,2) NOT NULL,
                max_score DECIMAL(6,2) NOT NULL,
                sort_no INT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS submission (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                assignment_id BIGINT NOT NULL,
                student_id BIGINT NOT NULL,
                content TEXT NOT NULL,
                submitted_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                UNIQUE (assignment_id, student_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS review_task (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                assignment_id BIGINT NOT NULL,
                submission_id BIGINT NOT NULL,
                reviewer_id BIGINT NOT NULL,
                status VARCHAR(16) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                completed_at TIMESTAMP NULL,
                UNIQUE (submission_id, reviewer_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS review (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                task_id BIGINT NOT NULL,
                submission_id BIGINT NOT NULL,
                reviewer_id BIGINT NOT NULL,
                total_score DECIMAL(6,2) NOT NULL,
                comment VARCHAR(2000) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                UNIQUE (task_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS review_score (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                review_id BIGINT NOT NULL,
                rubric_item_id BIGINT NOT NULL,
                item_name VARCHAR(64) NOT NULL,
                score DECIMAL(6,2) NOT NULL
            )
            """
    );

    private Database() {
    }

    /** 建表（幂等）并在首次启动时写入账号与演示数据。 */
    public static void initialize(ConnectionProvider provider) {
        try (Connection connection = provider.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String ddl : SCHEMA) {
                    statement.execute(ddl);
                }
            }
            seedUsers(connection);
            seedDemoData(connection);
            AppLogger.get().info("数据库初始化完成：" + provider.describe());
        } catch (SQLException ex) {
            throw new DataAccessException("初始化数据库失败：" + ex.getMessage(), ex);
        }
    }

    private static void seedUsers(Connection connection) throws SQLException {
        if (count(connection, "app_user") > 0) {
            return;
        }
        insertUser(connection, "teacher", "张明远", "TEACHER");
        insertUser(connection, "2023001", "李思远", "STUDENT");
        insertUser(connection, "2023002", "王雨桐", "STUDENT");
        insertUser(connection, "2023003", "陈嘉禾", "STUDENT");
        insertUser(connection, "2023004", "赵子涵", "STUDENT");
        insertUser(connection, "2023005", "周梓萱", "STUDENT");
        insertUser(connection, "2023006", "孙博文", "STUDENT");
        AppLogger.get().info("已创建初始账号：教师 teacher，学生 2023001~2023006，初始口令均为 123456");
    }

    private static void insertUser(Connection connection, String username, String displayName, String role)
            throws SQLException {
        String sql = "INSERT INTO app_user (username, password_hash, display_name, role, created_at) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, Passwords.hash("123456"));
            ps.setString(3, displayName);
            ps.setString(4, role);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    /** 演示数据：一门课 + 一次作业（含三条细则）+ 四份提交，方便首次运行即可体验完整流程。 */
    private static void seedDemoData(Connection connection) throws SQLException {
        if (count(connection, "course") > 0) {
            return;
        }
        long teacherId = findUserId(connection, "teacher");
        long courseId = insertCourse(connection, teacherId);
        long assignmentId = insertAssignment(connection, courseId);
        insertRubric(connection, assignmentId);

        insertSubmission(connection, assignmentId, findUserId(connection, "2023001"), """
                本实验把命令行参数解析模块拆成了 Lexer、Parser、OptionRegistry 三层：Lexer 只负责把字符流切成 Token，
                Parser 依据注册表把 Token 组装成命令对象，OptionRegistry 通过接口接收不同选项类型，新增选项无需改动解析主流程。
                测试覆盖了空参数、缺失值、非法短选项组合等边界情况，共 18 个 JUnit 用例全部通过。""");
        insertSubmission(connection, assignmentId, findUserId(connection, "2023002"), """
                采用 ADT 思路定义 Command 抽象类，具体命令（StartCommand、StopCommand）实现 execute()，
                调用方只依赖抽象，符合里氏替换。参数校验做了防御性编程，对 null、超长字符串与重复参数都给出明确提示。
                不足是模块间仍通过静态工具类耦合，后续计划改为构造注入。""");
        insertSubmission(connection, assignmentId, findUserId(connection, "2023003"), """
                实现了作业要求的模块化拆分，数据层用 JDBC 封装了通用查询方法，业务层与界面层通过事件回调通信，
                界面更换时不需要改动业务代码。重构过程中把重复的分支判断抽取为策略表驱动，圈复杂度从 12 降到 5。
                实验中遇到的问题主要是事务边界不清晰，已通过把一次业务操作收敛到一个服务方法中解决。""");
        insertSubmission(connection, assignmentId, findUserId(connection, "2023004"), """
                完成了基本的模块划分与单元测试，但对规格与实现不变量的理解不够深入，仅在类注释中简单说明了前置条件，
                没有用 assert 或校验方法固化。测试中发现两处异常未捕获，已补充 try-with-resources 与统一的异常提示。""");
        AppLogger.get().info("已写入演示课程与作业数据");
    }

    private static long insertCourse(Connection connection, long teacherId) throws SQLException {
        String sql = "INSERT INTO course (code, name, semester, teacher_id, created_at) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "CS301");
            ps.setString(2, "软件构造");
            ps.setString(3, "2026 秋");
            ps.setLong(4, teacherId);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    private static long insertAssignment(Connection connection, long courseId) throws SQLException {
        String sql = "INSERT INTO assignment (course_id, title, description, deadline, anonymous, "
                + "reviewers_per_submission, scoring_mode, status, created_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, courseId);
            ps.setString(2, "实验一：模块化设计与实现");
            ps.setString(3, "任选一个小型命令行工具或课堂管理功能，按模块化思想完成设计与实现，"
                    + "要求给出模块划分说明、关键接口定义，并对核心模块编写单元测试。");
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().plusDays(14)));
            ps.setBoolean(5, true);
            ps.setInt(6, 2);
            ps.setString(7, "NORMALIZED");
            ps.setString(8, "PUBLISHED");
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    private static void insertRubric(Connection connection, long assignmentId) throws SQLException {
        String sql = "INSERT INTO rubric_item (assignment_id, name, weight, min_score, max_score, sort_no) "
                + "VALUES (?,?,?,?,?,?)";
        String[][] items = {
                {"功能完整性", "40", "0", "10"},
                {"代码规范性", "30", "0", "10"},
                {"设计与创新", "30", "0", "10"}
        };
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < items.length; i++) {
                ps.setLong(1, assignmentId);
                ps.setString(2, items[i][0]);
                ps.setInt(3, Integer.parseInt(items[i][1]));
                ps.setDouble(4, Double.parseDouble(items[i][2]));
                ps.setDouble(5, Double.parseDouble(items[i][3]));
                ps.setInt(6, i + 1);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void insertSubmission(Connection connection, long assignmentId, long studentId, String content)
            throws SQLException {
        String sql = "INSERT INTO submission (assignment_id, student_id, content, submitted_at, updated_at) "
                + "VALUES (?,?,?,?,?)";
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, assignmentId);
            ps.setLong(2, studentId);
            ps.setString(3, content.strip());
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.executeUpdate();
        }
    }

    private static long findUserId(Connection connection, String username) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM app_user WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("缺少初始账号：" + username);
    }

    private static long count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private static long generatedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("插入后未获取到自增主键");
    }
}
