package com.peerreview.service.query;

import com.peerreview.common.RegexTextMatcher;
import com.peerreview.domain.SubmissionRow;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 提交记录的多条件过滤器（表驱动）。
 *
 * <p>每个筛选维度对应表里的一个条目，只在该维度「被启用」时登记；
 * 新增筛选条件只需加一个枚举值和一个 setter，调用方与遍历逻辑都不需要改。</p>
 */
public final class SubmissionFilter {

    /** 筛选维度：课程、作业、提交内容（正则）。 */
    public enum Key {
        COURSE, ASSIGNMENT, CONTENT
    }

    private final Map<Key, Predicate<SubmissionRow>> rules = new EnumMap<>(Key.class);

    public SubmissionFilter course(Long courseId) {
        if (courseId != null) {
            rules.put(Key.COURSE, row -> row.courseId() == courseId);
        }
        return this;
    }

    public SubmissionFilter assignment(Long assignmentId) {
        if (assignmentId != null) {
            rules.put(Key.ASSIGNMENT, row -> row.assignmentId() == assignmentId);
        }
        return this;
    }

    /** 正则匹配提交内容与学生姓名，空表达式不参与过滤。 */
    public SubmissionFilter keyword(RegexTextMatcher matcher) {
        if (matcher != null && matcher.isActive()) {
            rules.put(Key.CONTENT, row -> matcher.matchesAny(row.content(), row.studentName()));
        }
        return this;
    }

    public boolean accepts(SubmissionRow row) {
        return rules.values().stream().allMatch(rule -> rule.test(row));
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }

    public int activeRuleCount() {
        return rules.size();
    }
}
