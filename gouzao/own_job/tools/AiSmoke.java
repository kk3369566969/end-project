import com.peerreview.ai.ReviewSuggestion;
import com.peerreview.ai.ReviewSuggestionProvider;
import com.peerreview.ai.SuggestionProviders;
import com.peerreview.ai.SuggestionRequest;
import com.peerreview.config.AppConfig;
import com.peerreview.domain.RubricItem;

import java.util.List;

/**
 * AI 辅助评审的冒烟验证：读取真实配置、真实调用一次 DeepSeek，打印结构化结果。
 *
 * <p>这不是单元测试（会联网、有额度消耗），而是交付前的连通性检查：
 * 用 {@code mvn test} 跑不到的真实网络路径，靠它人工确认一次。</p>
 */
public final class AiSmoke {

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        ReviewSuggestionProvider provider = SuggestionProviders.fromConfig(config);
        System.out.println("provider = " + provider.displayName());
        System.out.println("available = " + provider.isAvailable());
        if (!provider.isAvailable()) {
            System.out.println("原因：" + provider.unavailableReason());
            return;
        }

        List<RubricItem> rubric = List.of(
                new RubricItem("模块划分", 40, 0, 10),
                new RubricItem("接口设计", 35, 0, 10),
                new RubricItem("异常处理", 25, 0, 5));

        SuggestionRequest request = new SuggestionRequest(
                "实验一：模块化设计与实现",
                "采用分层架构，业务逻辑与界面分离，要求具备单元测试与异常处理。",
                rubric,
                """
                        本次实验把评分计算抽象为 ScoreCalculator 接口，实现了 NormalizedWeightedCalculator
                        与 RawWeightedCalculator 两个策略，由 ScoreCalculators 注册表按 ScoringMode 分发。
                        DAO 层使用接口 + JDBC 实现，通过 AbstractJdbcDao 收敛连接管理与异常转换，
                        业务层完全感知不到 SQL。异常处理定义了 AppException / ValidationException /
                        DataAccessException 三级体系，界面层统一捕获后以对话框提示，不向用户抛堆栈。
                        测试方面有 21 个 JUnit 用例，覆盖计分边界、随机分配与端到端业务流程。
                        不足之处：暂未实现数据库连接失败重试，日志级别固定为 INFO，未做分级配置。
                        """);

        long started = System.nanoTime();
        ReviewSuggestion suggestion = provider.suggest(request);
        long wallClock = (System.nanoTime() - started) / 1_000_000;

        System.out.println();
        System.out.println("model   = " + suggestion.model());
        System.out.println("耗时    = " + suggestion.elapsedMillis() + "ms（含解析，调用方实测 " + wallClock + "ms）");
        System.out.println("tokens  = " + suggestion.totalTokens());
        System.out.println("维度建议：");
        suggestion.hints().forEach(hint -> System.out.println(
                "  - " + hint.dimension() + " | " + hint.score() + " | " + hint.reason()));
        System.out.println("总评：");
        System.out.println("  " + suggestion.comment());
    }
}
