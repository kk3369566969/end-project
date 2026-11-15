package com.peerreview.ai;

import com.peerreview.domain.RubricItem;

/**
 * 提示词构造：把领域对象翻译成模型能稳定理解的文本。
 *
 * <p>单独成类而不是拼在 Provider 里，原因有两个：一是提示词是「会反复调参的产品资产」，
 * 需要独立演进且便于被单元测试断言；二是换厂商时只有传输层不同，提示词可以整体复用。</p>
 *
 * <p>三条约束贯穿始终：不许臆造正文里没有的内容、建议分必须落在细则区间内、
 * 输出必须是纯 JSON。前两条决定生成质量，第三条决定解析是否能稳定成功。</p>
 */
public final class SuggestionPrompt {

    /** 正文过长时截断，控制单次调用的 token 成本，也避开上下文上限。 */
    static final int MAX_CONTENT_CHARS = 6000;

    private SuggestionPrompt() {
    }

    public static String system() {
        return """
                你是一位严谨的大学《软件构造》课程助教，正在协助学生撰写同伴互评意见。

                必须遵守的原则：
                1. 只依据给出的作业正文与评分细则评价，禁止臆造正文里不存在的功能、类名或章节。
                2. 建议分必须落在该维度的分值区间内，取 0.5 的整数倍。
                3. 判断依据要具体到正文中的做法（例如「把计分规则抽象为 ScoreCalculator 接口」），
                   不要写「很好」「不错」「有待提高」这类空话。
                4. 语气客观中立、对事不对人，不评价作者的身份、能力、态度或努力程度。
                5. 你的输出只是给学生参考的草稿，最终分数与评语由学生本人决定。

                输出要求：只输出一个 JSON 对象，不要使用 markdown 代码块，不要输出 JSON 之外的任何文字。
                JSON 结构如下：
                {
                  "dimensions": [
                    {
                      "name": "评分维度名称，必须与给定细则名称完全一致",
                      "score": 建议分，数字类型,
                      "reason": "不超过 45 字的判断依据"
                    }
                  ],
                  "comment": "120 至 220 字的总评：先指出具体优点，再给出可执行的改进建议，最后一句总结"
                }
                """;
    }

    /** 组装用户消息：作业题面 + 细则表 + 正文，全部用分隔标记包围，降低提示注入的干扰。 */
    public static String user(SuggestionRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【作业】").append(request.assignmentTitle()).append('\n');

        if (!request.requirements().isEmpty()) {
            prompt.append("\n【课程要求】\n")
                    .append(clip(request.requirements(), 1200))
                    .append('\n');
        }

        prompt.append("\n【评分细则】（维度 / 分值区间 / 权重）\n");
        int index = 1;
        for (RubricItem item : request.rubricItems()) {
            prompt.append(index++).append(". ")
                    .append(item.getName())
                    .append(" | ").append(item.rangeLabel())
                    .append(" | 权重 ").append(item.getWeight()).append("%\n");
        }

        prompt.append("\n【待评审作业正文】\n<<<\n")
                .append(clip(request.submissionContent(), MAX_CONTENT_CHARS))
                .append("\n>>>\n");

        prompt.append("\n请严格按上面的 JSON 结构输出，dimensions 覆盖全部 ")
                .append(request.rubricItems().size())
                .append(" 个维度，名称保持一字不差。");
        return prompt.toString();
    }

    private static String clip(String text, int limit) {
        if (text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit) + "\n……（正文过长，以下内容已省略）";
    }
}
