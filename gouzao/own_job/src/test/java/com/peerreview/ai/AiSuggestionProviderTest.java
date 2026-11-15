package com.peerreview.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.peerreview.common.AppException;
import com.peerreview.domain.RubricItem;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 提供者的协议层测试。
 *
 * <p>用 JDK 自带的 {@code HttpServer} 起一个本地桩服务，不需要联网、不消耗真实额度，
 * 却能覆盖真实 HTTP 往返的全部路径：请求体拼装、状态码翻译、JSON 解析、越界裁剪。</p>
 */
class AiSuggestionProviderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static final List<RubricItem> RUBRIC = List.of(
            new RubricItem("模块划分", 40, 0, 10),
            new RubricItem("接口设计", 35, 0, 10),
            new RubricItem("异常处理", 25, 0, 5));

    private static SuggestionRequest request() {
        return new SuggestionRequest("实验一：模块化设计与实现", "要求分层清晰，异常可控",
                RUBRIC, "本次实验把计分逻辑抽象成 ScoreCalculator 接口，并补充了 21 个单元测试。");
    }

    // ———————————————————— 正常路径 ————————————————————

    @Test
    @DisplayName("标准 JSON 响应被解析为逐维度建议与总评")
    void parsesWellFormedResponse() throws IOException {
        String content = """
                {"dimensions":[
                  {"name":"模块划分","score":8.5,"reason":"按层分包，职责边界清晰"},
                  {"name":"接口设计","score":9,"reason":"用接口隔离了计分策略"},
                  {"name":"异常处理","score":4,"reason":"对空提交做了校验"}
                ],"comment":"整体分层合理，接口抽象到位，建议补充数据库连接的失败重试。"}""";
        DeepSeekSuggestionProvider provider = providerReturning(200, completion(content));

        ReviewSuggestion suggestion = provider.suggest(request());

        assertEquals(3, suggestion.hints().size());
        assertEquals(8.5, suggestion.hints().get(0).score());
        assertEquals("模块划分", suggestion.hints().get(0).dimension());
        assertTrue(suggestion.comment().startsWith("整体分层合理"));
        assertEquals(123, suggestion.totalTokens());
        assertFalse(suggestion.isEmpty());
    }

    @Test
    @DisplayName("请求体携带全部细则名与作业正文")
    void requestPayloadCarriesRubricAndContent() throws IOException {
        AtomicReference<String> captured = new AtomicReference<>();
        DeepSeekSuggestionProvider provider =
                providerReturning(200, completion("{\"dimensions\":[],\"comment\":\"ok\"}"), captured);

        provider.suggest(request());

        String payload = captured.get();
        assertTrue(payload.contains("模块划分"), "细则名应出现在提示词中");
        assertTrue(payload.contains("异常处理"));
        assertTrue(payload.contains("ScoreCalculator"), "作业正文应被送出");
        assertTrue(payload.contains("\"response_format\""), "应启用 JSON 输出模式");
        assertTrue(payload.contains("\"stream\":false"));
    }

    @Test
    @DisplayName("模型给出的越界分数被裁剪到合法区间")
    void clampsOutOfRangeScore() throws IOException {
        String content = """
                {"dimensions":[
                  {"name":"模块划分","score":999,"reason":"远超上限"},
                  {"name":"接口设计","score":-20,"reason":"低于下限"},
                  {"name":"异常处理","score":4.3,"reason":"对齐到 0.5"}
                ],"comment":"裁剪测试"}""";
        ReviewSuggestion suggestion = providerReturning(200, completion(content)).suggest(request());

        assertEquals(10, suggestion.hints().get(0).score(), "超过上限应压到 10");
        assertEquals(0, suggestion.hints().get(1).score(), "低于下限应抬到 0");
        assertEquals(4.5, suggestion.hints().get(2).score(), "应按 0.5 对齐");
    }

    @Test
    @DisplayName("编造出来的维度被丢弃，不会污染打分表")
    void dropsUnknownDimension() throws IOException {
        String content = """
                {"dimensions":[
                  {"name":"模块划分","score":8,"reason":"合理"},
                  {"name":"团队协作","score":9,"reason":"细则里没有这一项"}
                ],"comment":"部分维度不可用"}""";
        ReviewSuggestion suggestion = providerReturning(200, completion(content)).suggest(request());

        assertEquals(1, suggestion.hints().size());
        assertEquals("模块划分", suggestion.hints().get(0).dimension());
    }

    @Test
    @DisplayName("维度名带后缀或包裹代码围栏时仍能对齐")
    void toleratesNoisyFormatting() throws IOException {
        String content = """
                ```json
                {"dimensions":[{"name":"模块划分（40%）","score":7,"reason":"兼容带权重的写法"}],
                 "comment":"围栏与后缀测试"}
                ```""";
        ReviewSuggestion suggestion = providerReturning(200, completion(content)).suggest(request());

        assertEquals(1, suggestion.hints().size());
        assertEquals("模块划分", suggestion.hints().get(0).dimension());
    }

    @Test
    @DisplayName("模型不返回 JSON 时降级为整段总评，而不是报错")
    void degradesToPlainComment() throws IOException {
        ReviewSuggestion suggestion = providerReturning(200,
                completion("这篇作业分层很清楚，建议把数据库异常统一包装后再抛给界面。"))
                .suggest(request());

        assertTrue(suggestion.hints().isEmpty());
        assertTrue(suggestion.comment().contains("分层很清楚"));
    }

    // ———————————————————— 异常路径 ————————————————————

    @Test
    @DisplayName("401 提示密钥失效，并指出配置项位置")
    void reportsInvalidKey() throws IOException {
        DeepSeekSuggestionProvider provider = providerReturning(401,
                "{\"error\":{\"message\":\"Authentication Fails\",\"type\":\"authentication_error\"}}");

        AppException error = assertThrows(AppException.class, () -> provider.suggest(request()));
        assertTrue(error.getMessage().contains("密钥无效"), error.getMessage());
        assertTrue(error.getMessage().contains("ai.api.key"));
    }

    @Test
    @DisplayName("账户余额不足时给出可执行的提示")
    void reportsInsufficientBalance() throws IOException {
        DeepSeekSuggestionProvider provider = providerReturning(402,
                "{\"error\":{\"message\":\"Insufficient Balance\"}}");

        assertTrue(assertThrows(AppException.class, () -> provider.suggest(request()))
                .getMessage().contains("余额不足"));
    }

    @Test
    @DisplayName("网关超时被翻译成中文提示")
    void reportsTimeout() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
        DeepSeekSuggestionProvider provider = new DeepSeekSuggestionProvider(
                baseUrl(), "sk-test", "deepseek-chat", 0.3, Duration.ofMillis(300));

        AppException error = assertThrows(AppException.class, () -> provider.suggest(request()));
        assertTrue(error.getMessage().contains("超时"), error.getMessage());
    }

    @Test
    @DisplayName("未配置密钥时标记为不可用，调用直接给出原因")
    void unavailableWithoutKey() {
        ReviewSuggestionProvider provider = SuggestionProviders.create(
                true, "", DeepSeekSuggestionProvider.DEFAULT_BASE_URL, "deepseek-chat", 0.3, 60);

        assertFalse(provider.isAvailable());
        assertTrue(provider.unavailableReason().contains("ai.api.key"));
        assertThrows(AppException.class, () -> provider.suggest(request()));
    }

    @Test
    @DisplayName("配置中关闭 AI 时同样不提供入口")
    void unavailableWhenDisabled() {
        ReviewSuggestionProvider provider = SuggestionProviders.create(
                false, "sk-there-is-a-key", DeepSeekSuggestionProvider.DEFAULT_BASE_URL,
                "deepseek-chat", 0.3, 60);

        assertFalse(provider.isAvailable());
        assertTrue(provider.unavailableReason().contains("ai.enabled"));
    }

    // ———————————————————— 辅助 ————————————————————

    private DeepSeekSuggestionProvider providerReturning(int status, String body) throws IOException {
        return providerReturning(status, body, null);
    }

    private DeepSeekSuggestionProvider providerReturning(int status, String body,
                                                         AtomicReference<String> captured) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            String received = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (captured != null) {
                captured.set(received);
            }
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        return new DeepSeekSuggestionProvider(baseUrl(), "sk-test", "deepseek-chat", 0.3,
                Duration.ofSeconds(5));
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** 按 OpenAI 兼容格式包装 content，保证测试关注点落在内容解析而非信封结构。 */
    private static String completion(String content) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", "deepseek-chat");
        ArrayNode choices = root.putArray("choices");
        ObjectNode choice = choices.addObject();
        choice.putObject("message").put("role", "assistant").put("content", content);
        choice.put("finish_reason", "stop");
        root.putObject("usage").put("total_tokens", 123);
        return MAPPER.writeValueAsString(root);
    }
}
