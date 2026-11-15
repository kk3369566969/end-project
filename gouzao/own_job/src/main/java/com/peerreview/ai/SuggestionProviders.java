package com.peerreview.ai;

import com.peerreview.common.AppLogger;
import com.peerreview.common.Text;
import com.peerreview.config.AppConfig;

import java.time.Duration;

/**
 * 按配置装配 AI 提供者（工厂）。
 *
 * <p>把「配置 → 实现」的映射收在一个方法里，{@code AppContext} 只调一次，
 * 换模型厂商时只改这里，其余代码不动。</p>
 */
public final class SuggestionProviders {

    private SuggestionProviders() {
    }

    public static ReviewSuggestionProvider fromConfig(AppConfig config) {
        return create(config.aiEnabled(), config.aiApiKey(), config.aiBaseUrl(),
                config.aiModel(), config.aiTemperature(), config.aiTimeoutSeconds());
    }

    /** 真正的装配逻辑，与配置读取解耦，便于单元测试直接驱动开关组合。 */
    static ReviewSuggestionProvider create(boolean enabled, String apiKey, String baseUrl,
                                           String model, double temperature, int timeoutSeconds) {
        if (!enabled) {
            return new UnavailableSuggestionProvider("AI 辅助已在配置中关闭（ai.enabled=false）");
        }
        if (Text.isBlank(apiKey)) {
            return new UnavailableSuggestionProvider(
                    "尚未配置 DeepSeek 密钥（config/config.properties 中的 ai.api.key）");
        }
        DeepSeekSuggestionProvider provider = new DeepSeekSuggestionProvider(
                baseUrl, apiKey, model, temperature, Duration.ofSeconds(timeoutSeconds));
        AppLogger.get().info("AI 辅助评审已启用：" + provider.displayName() + "，端点 " + baseUrl);
        return provider;
    }
}
