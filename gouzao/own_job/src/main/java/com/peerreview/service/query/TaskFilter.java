package com.peerreview.service.query;

import com.peerreview.common.RegexTextMatcher;
import com.peerreview.domain.TaskRow;
import com.peerreview.domain.TaskStatus;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;

/** 互评任务的多条件过滤器（表驱动，思路同 {@link SubmissionFilter}）。 */
public final class TaskFilter {

    public enum Key {
        STATUS, KEYWORD
    }

    private final Map<Key, Predicate<TaskRow>> rules = new EnumMap<>(Key.class);

    public TaskFilter status(TaskStatus status) {
        if (status != null) {
            rules.put(Key.STATUS, row -> row.status() == status);
        }
        return this;
    }

    public TaskFilter keyword(RegexTextMatcher matcher) {
        if (matcher != null && matcher.isActive()) {
            rules.put(Key.KEYWORD, row -> matcher.matchesAny(
                    row.assignmentTitle(), row.courseName(), row.content()));
        }
        return this;
    }

    public boolean accepts(TaskRow row) {
        return rules.values().stream().allMatch(rule -> rule.test(row));
    }
}
