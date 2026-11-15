package com.peerreview.ai;

import com.peerreview.common.AppException;

/**
 * 空对象实现：功能被关闭或未配置密钥时占位。
 *
 * <p>用它而不是让 {@code AppContext.ai()} 返回 {@code null}，界面就不必到处写判空；
 * 一旦有人绕过 {@link #isAvailable()} 直接调用，也会得到一句明确的解释而不是 NPE。</p>
 */
public final class UnavailableSuggestionProvider implements ReviewSuggestionProvider {

    private final String reason;

    public UnavailableSuggestionProvider(String reason) {
        this.reason = reason;
    }

    @Override
    public String displayName() {
        return "AI 辅助未启用";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String unavailableReason() {
        return reason;
    }

    @Override
    public ReviewSuggestion suggest(SuggestionRequest request) {
        throw new AppException(reason);
    }
}
