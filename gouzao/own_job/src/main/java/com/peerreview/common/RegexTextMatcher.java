package com.peerreview.common;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 正则文本检索器（对应课程要求「使用正则表达式搜索作业内容、评语文本」）。
 *
 * <p>空表达式视为「不过滤」，非法表达式给出可读的校验错误，而不是让异常穿透到事件线程。</p>
 */
public final class RegexTextMatcher {

    private static final RegexTextMatcher MATCH_ALL = new RegexTextMatcher(null, "");

    private final Pattern pattern;
    private final String expression;

    private RegexTextMatcher(Pattern pattern, String expression) {
        this.pattern = pattern;
        this.expression = expression;
    }

    public static RegexTextMatcher matchAll() {
        return MATCH_ALL;
    }

    /**
     * 根据用户输入创建检索器，忽略大小写并支持中文。
     *
     * @throws ValidationException 表达式语法错误时抛出
     */
    public static RegexTextMatcher of(String expression) {
        String trimmed = Text.trimToEmpty(expression);
        if (trimmed.isEmpty()) {
            return MATCH_ALL;
        }
        try {
            return new RegexTextMatcher(
                    Pattern.compile(trimmed, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
                    trimmed);
        } catch (PatternSyntaxException ex) {
            throw new ValidationException("正则表达式语法错误：" + ex.getDescription());
        }
    }

    public boolean matches(String text) {
        if (pattern == null) {
            return true;
        }
        return pattern.matcher(text == null ? "" : text).find();
    }

    /** 任一字段命中即视为匹配。 */
    public boolean matchesAny(String... texts) {
        if (pattern == null) {
            return true;
        }
        for (String text : texts) {
            if (matches(text)) {
                return true;
            }
        }
        return false;
    }

    public boolean isActive() {
        return pattern != null;
    }

    public String expression() {
        return expression;
    }
}
