package com.peerreview.ai;

/**
 * AI 参考意见生成器（策略接口）。
 *
 * <p>与评分策略 {@code ScoreCalculator}、分配策略 {@code ReviewAllocator} 一样，
 * 这里把「谁来生成意见」抽象成接口，{@code AppContext} 按配置装配具体实现：
 * 换成别的厂商模型、换成本地小模型，或换成一个返回固定文本的假实现（测试用），
 * 界面与业务层都不需要改动一行。</p>
 *
 * <p>实现约定：</p>
 * <ul>
 *   <li>失败一律抛 {@code AppException}，消息面向用户可直接展示；</li>
 *   <li>不可用时不要抛异常，而应让 {@link #isAvailable()} 返回 false，
 *       界面据此隐藏入口，避免用户点了才看到报错。</li>
 * </ul>
 */
public interface ReviewSuggestionProvider {

    /** 界面上展示的实现名称，例如「DeepSeek · deepseek-chat」。 */
    String displayName();

    /** 是否具备调用条件（开关打开且密钥已配置）。 */
    boolean isAvailable();

    /** 不可用的原因，用于在界面或日志中说明为什么没有这个入口。 */
    String unavailableReason();

    /**
     * 生成参考意见。这是阻塞调用，界面层必须放到后台线程执行。
     *
     * @throws RuntimeException 网络异常、鉴权失败、超时等，消息可直接展示给用户
     */
    ReviewSuggestion suggest(SuggestionRequest request);
}
