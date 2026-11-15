package com.peerreview.ai;

import com.peerreview.domain.RubricItem;

import java.util.List;

/**
 * 一次「生成参考意见」请求的输入。
 *
 * <p>刻意只包含模型判断所需的最小信息：作业题面、评分细则、待评审正文。
 * 不传作者姓名、学号、评审人身份——即便开启了实名互评，送给外部模型的也只有内容本身，
 * 这样从数据流上杜绝了通过 AI 泄露评审关系。</p>
 */
public record SuggestionRequest(
        String assignmentTitle,
        String requirements,
        List<RubricItem> rubricItems,
        String submissionContent) {

    public SuggestionRequest {
        assignmentTitle = assignmentTitle == null ? "" : assignmentTitle.trim();
        requirements = requirements == null ? "" : requirements.trim();
        rubricItems = rubricItems == null ? List.of() : List.copyOf(rubricItems);
        submissionContent = submissionContent == null ? "" : submissionContent;
    }
}
