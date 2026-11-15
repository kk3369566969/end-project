package com.peerreview.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 极简日志器（对应课程要求「简单日志模块」）。
 *
 * <p>单例 + 文件追加写，记录任务分配、打分等关键业务动作；同时输出到控制台便于调试。
 * 写日志失败不影响主流程（仅打印到 stderr），避免日志拖垮业务。</p>
 */
public final class AppLogger {

    private static final AppLogger INSTANCE = new AppLogger();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private volatile Path logFile;

    private AppLogger() {
    }

    public static AppLogger get() {
        return INSTANCE;
    }

    /** 由应用启动流程调用，指定日志文件位置。 */
    public void init(Path file) {
        this.logFile = file;
        if (file.getParent() != null) {
            try {
                Files.createDirectories(file.getParent());
            } catch (IOException ex) {
                System.err.println("无法创建日志目录：" + ex.getMessage());
            }
        }
    }

    public void info(String message) {
        write("INFO", message, null);
    }

    public void warn(String message) {
        write("WARN", message, null);
    }

    public void error(String message, Throwable error) {
        write("ERROR", message, error);
    }

    private synchronized void write(String level, String message, Throwable error) {
        String line = "[" + STAMP.format(LocalDateTime.now()) + "] [" + level + "] " + message;
        System.out.println(line);
        if (error != null) {
            error.printStackTrace();
        }
        Path target = logFile;
        if (target == null) {
            return;
        }
        try {
            StringBuilder block = new StringBuilder(line).append(System.lineSeparator());
            if (error != null) {
                block.append("    ").append(error.getClass().getSimpleName())
                        .append(": ").append(error.getMessage()).append(System.lineSeparator());
            }
            Files.writeString(target, block.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println("写日志失败：" + ex.getMessage());
        }
    }
}
