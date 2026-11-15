package com.peerreview;

import com.peerreview.ai.ReviewSuggestionProvider;
import com.peerreview.ai.SuggestionProviders;
import com.peerreview.common.AppLogger;
import com.peerreview.common.DataAccessException;
import com.peerreview.config.AppConfig;
import com.peerreview.dao.AssignmentDao;
import com.peerreview.dao.CourseDao;
import com.peerreview.dao.ReviewDao;
import com.peerreview.dao.ReviewTaskDao;
import com.peerreview.dao.SubmissionDao;
import com.peerreview.dao.UserDao;
import com.peerreview.dao.jdbc.JdbcAssignmentDao;
import com.peerreview.dao.jdbc.JdbcCourseDao;
import com.peerreview.dao.jdbc.JdbcReviewDao;
import com.peerreview.dao.jdbc.JdbcReviewTaskDao;
import com.peerreview.dao.jdbc.JdbcSubmissionDao;
import com.peerreview.dao.jdbc.JdbcUserDao;
import com.peerreview.db.ConnectionProvider;
import com.peerreview.db.Database;
import com.peerreview.db.DriverManagerConnectionProvider;
import com.peerreview.service.AllocationService;
import com.peerreview.service.AssignmentService;
import com.peerreview.service.AuthService;
import com.peerreview.service.CourseService;
import com.peerreview.service.ReviewService;
import com.peerreview.service.StatisticsService;
import com.peerreview.service.SubmissionService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 组合根（composition root）：只在这里完成「接口 → 实现」的装配，其余模块一律通过构造器注入依赖。
 *
 * <p>好处是业务层与界面层都不依赖具体数据库与 DAO 实现，单测可直接替换为内存实现。</p>
 */
public final class AppContext implements AutoCloseable {

    private final AppConfig config;
    private final ConnectionProvider connections;

    private final AuthService authService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final AllocationService allocationService;
    private final ReviewService reviewService;
    private final StatisticsService statisticsService;
    private final ReviewSuggestionProvider suggestionProvider;

    private AppContext(AppConfig config, ConnectionProvider connections) {
        this.config = config;
        this.connections = connections;

        UserDao userDao = new JdbcUserDao(connections);
        CourseDao courseDao = new JdbcCourseDao(connections);
        AssignmentDao assignmentDao = new JdbcAssignmentDao(connections);
        SubmissionDao submissionDao = new JdbcSubmissionDao(connections);
        ReviewTaskDao reviewTaskDao = new JdbcReviewTaskDao(connections);
        ReviewDao reviewDao = new JdbcReviewDao(connections);

        this.authService = new AuthService(userDao, config.teacherInviteCode());
        this.assignmentService = new AssignmentService(assignmentDao, courseDao,
                submissionDao, reviewTaskDao, reviewDao);
        this.courseService = new CourseService(courseDao, assignmentDao, assignmentService);
        this.submissionService = new SubmissionService(assignmentDao, submissionDao);
        this.allocationService = AllocationService.withDefaultAllocator(assignmentDao, submissionDao, reviewTaskDao);
        this.reviewService = new ReviewService(assignmentDao, reviewTaskDao, reviewDao);
        this.statisticsService = new StatisticsService(submissionDao, reviewDao);
        this.suggestionProvider = SuggestionProviders.fromConfig(config);
    }

    /**
     * 连接配置文件指定的 MySQL 并完成建表。
     *
     * <p>目标库尚未创建时（MySQL 1049）会先连到服务器执行建库，再重试一次——
     * 「换一台机器就要手工执行 DDL」是启动期最常见的失败原因，这里一次性消除。</p>
     *
     * <p>内置 H2 的启动入口是 {@link #connectEmbedded(AppConfig)}，由 {@code Main} 按 db.type 分流。</p>
     */
    public static AppContext connect(AppConfig config) {
        ConnectionProvider provider = new DriverManagerConnectionProvider(
                config.dbUrl(), config.dbUsername(), config.dbPassword(), "MySQL · " + config.dbUrl());
        try {
            ((DriverManagerConnectionProvider) provider).verify();
        } catch (DataAccessException ex) {
            if (!isUnknownDatabase(ex)) {
                throw ex;
            }
            // 「库不存在」是可自愈的启动问题：先连到服务器建库，再重试一次
            createDatabase(config);
            ((DriverManagerConnectionProvider) provider).verify();
        }
        Database.initialize(provider);
        AppLogger.get().info("已连接数据库：" + provider.describe());
        return new AppContext(config, provider);
    }

    /** 判断异常链里是否有 MySQL 1049（Unknown database）。 */
    private static boolean isUnknownDatabase(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException sql
                    && (sql.getErrorCode() == 1049
                    || (sql.getMessage() != null && sql.getMessage().contains("Unknown database")))) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }

    private static void createDatabase(AppConfig config) {
        String database = config.databaseName();
        if (database.isEmpty()) {
            throw new DataAccessException("无法从 db.url 解析出数据库名，请检查 config/config.properties");
        }
        String serverUrl = config.serverUrl();
        try (Connection connection = DriverManager.getConnection(
                serverUrl, config.dbUsername(), config.dbPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database
                    + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            AppLogger.get().info("数据库 " + database + " 不存在，已自动创建（utf8mb4）");
        } catch (SQLException ex) {
            throw new DataAccessException("数据库 " + database + " 不存在，且自动创建失败：" + ex.getMessage()
                    + "\n请手动执行：CREATE DATABASE " + database
                    + " DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;", ex);
        }
    }

    /**
     * 用给定的连接提供者装配上下文：集成测试注入内存库，生产代码走 {@link #connect(AppConfig)}。
     */
    public static AppContext connectWith(AppConfig config, ConnectionProvider provider) {
        Database.initialize(provider);
        return new AppContext(config, provider);
    }

    /** 使用内置 H2 文件库启动，便于没有 MySQL 的环境演示与验收。 */
    public static AppContext connectEmbedded(AppConfig config) {
        ConnectionProvider provider = new DriverManagerConnectionProvider(
                config.embeddedUrl(), config.embeddedUsername(), config.embeddedPassword(),
                "内置 H2 数据库（" + config.embeddedUrl() + "）");
        ((DriverManagerConnectionProvider) provider).verify();
        Database.initialize(provider);
        AppLogger.get().info("已启用内置数据库：" + provider.describe());
        return new AppContext(config, provider);
    }

    public AppConfig config() {
        return config;
    }

    public String connectionDescription() {
        return connections.describe();
    }

    public AuthService auth() {
        return authService;
    }

    public CourseService courses() {
        return courseService;
    }

    public AssignmentService assignments() {
        return assignmentService;
    }

    public SubmissionService submissions() {
        return submissionService;
    }

    public AllocationService allocation() {
        return allocationService;
    }

    public ReviewService reviews() {
        return reviewService;
    }

    public StatisticsService statistics() {
        return statisticsService;
    }

    /**
     * AI 参考意见生成器。功能关闭或未配置密钥时返回空对象实现，
     * 调用方用 {@code isAvailable()} 判断是否展示入口即可，无需判空。
     */
    public ReviewSuggestionProvider ai() {
        return suggestionProvider;
    }

    @Override
    public void close() {
        connections.close();
    }
}
