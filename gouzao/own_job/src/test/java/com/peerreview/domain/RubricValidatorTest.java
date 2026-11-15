package com.peerreview.domain;

import com.peerreview.common.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 评分细则规则测试。 */
@DisplayName("评分细则校验")
class RubricValidatorTest {

    private RubricItem item(String name, int weight, double min, double max) {
        return new RubricItem(name, weight, min, max);
    }

    @Test
    @DisplayName("权重合计 100% 时通过校验")
    void validRubricPasses() {
        assertDoesNotThrow(() -> RubricValidator.validate(List.of(
                item("完整性", 40, 0, 10),
                item("规范性", 30, 0, 10),
                item("创新性", 30, 0, 10))));
    }

    @Test
    @DisplayName("权重合计不等于 100% 时给出明确提示")
    void totalWeightMustBeHundred() {
        ValidationException ex = assertThrows(ValidationException.class, () -> RubricValidator.validate(List.of(
                item("完整性", 60, 0, 10),
                item("规范性", 30, 0, 10))));
        assertTrue(ex.getMessage().contains("100%"));
    }

    @Test
    @DisplayName("空细则、重复维度、非法区间都会被拒绝")
    void invalidCases() {
        assertThrows(ValidationException.class, () -> RubricValidator.validate(List.of()));
        assertThrows(ValidationException.class, () -> RubricValidator.validate(List.of(
                item("完整性", 50, 0, 10),
                item("完整性", 50, 0, 10))));
        assertThrows(ValidationException.class, () -> RubricValidator.validate(List.of(
                item("完整性", 100, 8, 2))));
        assertThrows(ValidationException.class, () -> RubricValidator.validate(List.of(
                item("完整性", 100, 0, 200))));
        assertThrows(ValidationException.class, () -> RubricValidator.validate(List.of(
                item("  ", 100, 0, 10))));
    }

    @Test
    @DisplayName("权重合计计算与展示格式化")
    void totalsAndFormatting() {
        List<RubricItem> items = List.of(item("完整性", 40, 0, 10), item("规范性", 60, 0, 10));
        assertEquals(100, RubricValidator.totalWeight(items));
        assertEquals("40%", items.get(0).weightLabel());
        assertEquals("0 ~ 10 分", items.get(0).rangeLabel());
        assertEquals("8.5", RubricItem.trim(8.50));
    }
}
