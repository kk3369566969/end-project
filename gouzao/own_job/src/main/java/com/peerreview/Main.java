package com.peerreview;

import com.peerreview.common.AppException;
import com.peerreview.common.AppLogger;
import com.peerreview.config.AppConfig;
import com.peerreview.config.AppHome;
import com.peerreview.ui.Dialogs;
import com.peerreview.ui.LoginFrame;
import com.peerreview.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * 程序入口：读取配置 → 连接数据库 → 安装主题 → 打开登录窗口。
 *
 * <p>数据库连接失败时不会直接崩溃，而是询问是否切换到内置数据库，
 * 保证换一台没有 MySQL 的机器也能完成演示与验收。</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        Theme.install();

        AppConfig config = AppConfig.load();
        AppLogger.get().init(config.logFile());
        AppLogger.get().info("应用根目录：" + AppHome.root());
        installGlobalExceptionHandler();

        AppContext context = connect(config);
        if (context == null) {
            return;
        }
        AppLogger.get().info("应用启动完成，数据库类型：" + config.dbType());
        new LoginFrame(context).setVisible(true);
    }

    /**
     * 按 {@code db.type} 决定连接哪类数据库。
     *
     * <p>配置为 embedded 时直接走内置库，不再先试 MySQL——否则每次启动都要等一次连接超时；
     * 配置为 mysql 时才去连外部库，失败后询问是否降级到内置库并记住这次选择。</p>
     */
    private static AppContext connect(AppConfig config) {
        if (AppConfig.DB_TYPE_EMBEDDED.equals(config.dbType())) {
            return connectEmbeddedQuietly(config);
        }
        try {
            return AppContext.connect(config);
        } catch (AppException ex) {
            AppLogger.get().error("连接配置的数据库失败", ex);
            boolean useEmbedded = Dialogs.confirm(null, "数据库连接失败",
                    ex.getMessage()
                            + "\n\n当前使用的配置文件：\n" + config.sourceFile()
                            + "\n\n是否改用内置数据库（H2）启动？\n"
                            + "数据将保存在程序目录下的 data/ 文件夹，功能与 MySQL 完全一致。",
                    "使用内置数据库");
            if (!useEmbedded) {
                return null;
            }
            AppContext context = connectEmbeddedQuietly(config);
            if (context != null) {
                // 记住这次选择，下次启动直接走内置库，不再弹同样的错误。
                config.switchToEmbedded();
            }
            return context;
        }
    }

    private static AppContext connectEmbeddedQuietly(AppConfig config) {
        try {
            return AppContext.connectEmbedded(config);
        } catch (AppException ex) {
            AppLogger.get().error("启动内置数据库失败", ex);
            Dialogs.error(null, ex);
            return null;
        }
    }

    /** 兜底异常处理：日志留痕 + 用户可读提示，避免界面无响应且无任何反馈。 */
    private static void installGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            AppLogger.get().error("未捕获异常（线程 " + thread.getName() + "）", error);
            SwingUtilities.invokeLater(() -> Dialogs.error(null, "发生未预期的错误：" + error.getMessage()));
        });
    }
}
