package com.peerreview.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日期时间的展示与解析。界面上统一使用 {@code yyyy-MM-dd HH:mm} 形式。
 */
public final class Dates {

    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private Dates() {
    }

    public static String format(LocalDateTime time) {
        return time == null ? "—" : DISPLAY.format(time);
    }

    /** 供导出文件名使用的紧凑时间戳。 */
    public static String fileStamp(LocalDateTime time) {
        return FILE_STAMP.format(time == null ? LocalDateTime.now() : time);
    }

    /**
     * 解析用户输入的时间文本，空输入返回 {@code null}（表示不限制截止时间）。
     *
     * @throws ValidationException 格式非法时抛出，提示里给出正确样例
     */
    public static LocalDateTime parseOptional(String text, String fieldName) {
        if (Text.isBlank(text)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), DISPLAY);
        } catch (DateTimeParseException ex) {
            throw new ValidationException(fieldName + "格式应为 yyyy-MM-dd HH:mm，例如 2026-12-31 23:59");
        }
    }
}
