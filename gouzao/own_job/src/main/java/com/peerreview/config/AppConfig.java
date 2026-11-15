package com.peerreview.config;

import com.peerreview.common.AppLogger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 应用配置（对应课程要求「软件的配置参数保存在本地文件中」）。
 *
 * <p>首次启动时在 {@code config/config.properties} 生成带注释的默认配置；
 * 之后每次启动读取该文件，修改数据库连接无需改代码。</p>
 */
public final class AppConfig {

    /** 数据库类型：mysql = 连接外部 MySQL；embedded = 使用内置 H2（MySQL 兼容模式）。 */
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String DB_TYPE_EMBEDDED = "embedded";

    /** 教师注册邀请码的默认值（演示用，正式部署应在配置文件中改掉）。 */
    public static final String DEFAULT_TEACHER_INVITE_CODE = "TEACH2026";

    /**
     * 默认配置文件位置：{@code <应用根目录>/config/config.properties}。
     *
     * <p>不写成相对路径——双击 jar 时工作目录是 jar 所在目录（打包后在 target/），
     * 相对路径会导致程序读不到项目根的配置，而在 target/ 下另生成一份默认配置。</p>
     */
    private static Path defaultFile() {
        return AppHome.root().resolve("config").resolve("config.properties");
    }

    private final Path file;
    private final Properties properties;

    private AppConfig(Path file, Properties properties) {
        this.file = file;
        this.properties = properties;
    }

    /** 读取配置；文件不存在则创建默认配置后继续使用默认值。 */
    public static AppConfig load() {
        Path file = defaultFile();
        if (!Files.exists(file)) {
            writeTemplate(file);
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException ex) {
            AppLogger.get().warn("读取配置文件失败，将使用内置默认值：" + ex.getMessage());
        }
        return new AppConfig(file, properties);
    }

    private static Properties defaultProperties() {
        Properties defaults = new Properties();
        defaults.setProperty("db.type", DB_TYPE_MYSQL);
        defaults.setProperty("db.url",
                "jdbc:mysql://localhost:3306/peer_review"
                        + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
                        + "&useSSL=false&allowPublicKeyRetrieval=true");
        defaults.setProperty("db.username", "root");
        defaults.setProperty("db.password", "123456");
        defaults.setProperty("db.embedded.path", "data/peer_review");
        defaults.setProperty("log.file", "logs/app.log");
        defaults.setProperty("register.teacher.code", "TEACH2026");
        defaults.setProperty("ai.enabled", "true");
        defaults.setProperty("ai.base.url", "https://api.deepseek.com");
        defaults.setProperty("ai.model", "deepseek-chat");
        defaults.setProperty("ai.api.key", "");
        defaults.setProperty("ai.timeout.seconds", "60");
        defaults.setProperty("ai.temperature", "0.3");
        return defaults;
    }

    private static void writeTemplate(Path file) {
        new AppConfig(file, defaultProperties()).save();
    }

    /**
     * 写回配置文件：手工拼装文本而不使用 {@link Properties#store}，
     * 避免出现 {@code jdbc\://} 这类转义与自动时间戳注释，保证文件可人工编辑。
     */
    public void save() {
        String content = """
                # 同伴互评工具配置（修改后重启生效）
                # db.type=mysql     连接外部 MySQL，需先执行 CREATE DATABASE peer_review
                # db.type=embedded  使用内置 H2 数据库，免安装，便于演示与验收
                db.type=%s
                db.url=%s
                db.username=%s
                db.password=%s
                db.embedded.path=%s
                log.file=%s

                # ———— 注册 ————
                # 教师账号注册需要邀请码；学生注册不需要。改成空串则禁止教师自助注册。
                register.teacher.code=%s

                # ———— AI 辅助评审（DeepSeek，OpenAI 兼容协议）————
                # ai.enabled=false 可整体关闭：打分窗口不再出现「AI 参考意见」按钮
                # ai.api.key 为空的处理方式：同样视作未启用，界面自动隐藏该功能
                # 密钥申请地址：https://platform.deepseek.com/api_keys
                ai.enabled=%s
                ai.base.url=%s
                ai.model=%s
                ai.api.key=%s
                ai.timeout.seconds=%s
                ai.temperature=%s
                """.formatted(
                dbType(),
                dbUrl(),
                dbUsername(),
                dbPassword(),
                get("db.embedded.path", "data/peer_review"),
                get("log.file", "logs/app.log"),
                get("register.teacher.code", "TEACH2026"),
                get("ai.enabled", "true"),
                get("ai.base.url", "https://api.deepseek.com"),
                get("ai.model", "deepseek-chat"),
                get("ai.api.key", ""),
                get("ai.timeout.seconds", "60"),
                get("ai.temperature", "0.3"));
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            AppLogger.get().warn("写入配置文件失败：" + ex.getMessage());
        }
    }

    /** 切换到内置数据库并持久化，下次启动不再重复报错。 */
    public void switchToEmbedded() {
        properties.setProperty("db.type", DB_TYPE_EMBEDDED);
        save();
    }

    public String dbType() {
        return get("db.type", DB_TYPE_MYSQL).trim().toLowerCase();
    }

    public String dbUrl() {
        return get("db.url", "jdbc:mysql://localhost:3306/peer_review");
    }

    /**
     * 库名（MySQL 连接串中主机端口之后、问号之前的那一段）。
     * 取不到时返回空串，调用方据此跳过「自动建库」。
     */
    public String databaseName() {
        String url = dbUrl();
        int hostEnd = url.indexOf("//");
        if (hostEnd < 0) {
            return "";
        }
        int slash = url.indexOf('/', hostEnd + 2);
        if (slash < 0) {
            return "";
        }
        int query = url.indexOf('?', slash);
        String name = query < 0
                ? url.substring(slash + 1)
                : url.substring(slash + 1, query);
        // 库名会拼进 DDL，只接受安全字符
        return name.matches("[A-Za-z0-9_$]+") ? name : "";
    }

    /**
     * 服务器级连接串：去掉库名，保留其余参数。
     * 用于「目标库尚未创建」时先连到服务器执行建库语句。
     */
    public String serverUrl() {
        String url = dbUrl();
        int hostEnd = url.indexOf("//");
        if (hostEnd < 0 || databaseName().isEmpty()) {
            return url;
        }
        int slash = url.indexOf('/', hostEnd + 2);
        int query = url.indexOf('?', slash);
        return query < 0 ? url.substring(0, slash) : url.substring(0, slash) + url.substring(query);
    }

    public String dbUsername() {
        return get("db.username", "root");
    }

    public String dbPassword() {
        return get("db.password", "");
    }

    /** H2 文件库连接串：与 MySQL 保持同一套 SQL 方言。 */
    public String embeddedUrl() {
        String base = get("db.embedded.path", "data/peer_review").replace('\\', '/');
        Path absolute = AppHome.resolve(base);
        return "jdbc:h2:file:" + absolute + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE";
    }

    public String embeddedUsername() {
        return "sa";
    }

    public String embeddedPassword() {
        return "";
    }

    public Path logFile() {
        return AppHome.resolve(get("log.file", "logs/app.log"));
    }

    // ———————————————————— 注册 ————————————————————

    /**
     * 教师注册邀请码。
     *
     * <p>刻意不用 {@link #get}：那里会把空串当作"未配置"而回退到默认值，
     * 而这里空串是有意义的配置——表示关闭教师自助注册。</p>
     */
    public String teacherInviteCode() {
        String value = properties.getProperty("register.teacher.code", DEFAULT_TEACHER_INVITE_CODE);
        return value == null ? "" : value.trim();
    }

    // ———————————————————— AI 辅助评审 ————————————————————

    /** 是否启用 AI 参考意见（关闭或未配置密钥时，界面不显示该入口）。 */
    public boolean aiEnabled() {
        return Boolean.parseBoolean(get("ai.enabled", "true").trim());
    }

    public String aiBaseUrl() {
        String value = get("ai.base.url", "https://api.deepseek.com").trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public String aiModel() {
        return get("ai.model", "deepseek-chat").trim();
    }

    /** DeepSeek 密钥；未配置返回空串，由上层判定为「不可用」而不是启动失败。 */
    public String aiApiKey() {
        return get("ai.api.key", "").trim();
    }

    public int aiTimeoutSeconds() {
        return parsePositiveInt(get("ai.timeout.seconds", "60"), 60);
    }

    public double aiTemperature() {
        String raw = get("ai.temperature", "0.3").trim();
        try {
            double value = Double.parseDouble(raw);
            return value < 0 || value > 2 ? 0.3 : value;
        } catch (NumberFormatException ex) {
            return 0.3;
        }
    }

    /** 配置项容错：写错数字不应该让程序起不来，退回默认值并记一条告警。 */
    private int parsePositiveInt(String raw, int fallback) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // 落到下面统一告警
        }
        AppLogger.get().warn("配置项 ai.timeout.seconds 非法，已回退为 " + fallback);
        return fallback;
    }

    public String get(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    public Path sourceFile() {
        return file;
    }
}
