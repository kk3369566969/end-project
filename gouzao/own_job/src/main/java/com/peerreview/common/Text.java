package com.peerreview.common;

/**
 * 轻量文本工具：统一空值、长度校验与展示截断，避免各处重复写 if 判断。
 */
public final class Text {

    private Text() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 校验必填文本并返回去除首尾空白后的结果。
     *
     * @param value     原始值
     * @param fieldName 字段中文名，用于拼接错误提示
     * @param maxLength 允许的最大长度
     * @throws ValidationException 为空或超长时抛出
     */
    public static String requireText(String value, String fieldName, int maxLength) {
        String text = trimToEmpty(value);
        if (text.isEmpty()) {
            throw new ValidationException(fieldName + "不能为空");
        }
        if (text.length() > maxLength) {
            throw new ValidationException(fieldName + "长度不能超过 " + maxLength + " 个字符");
        }
        return text;
    }

    /** 展示用截断：超出长度时补省略号，同时把换行压成空格。 */
    public static String ellipsis(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String flat = value.replaceAll("\\s+", " ").trim();
        return flat.length() <= maxLength ? flat : flat.substring(0, maxLength) + "…";
    }

    /** 匿名展示：两字姓名保留姓氏，三字及以上保留首尾。 */
    public static String maskName(String name) {
        if (isBlank(name)) {
            return "匿名同学";
        }
        String value = name.trim();
        if (value.length() <= 1) {
            return "*";
        }
        if (value.length() == 2) {
            return value.charAt(0) + "*";
        }
        return value.charAt(0) + "*" + value.charAt(value.length() - 1);
    }

    /** 统计字数（不含空白），用于提交内容与评语的展示。 */
    public static int countVisibleChars(String value) {
        return value == null ? 0 : value.replaceAll("\\s", "").length();
    }
}
