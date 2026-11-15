package com.peerreview.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.peerreview.common.AppException;
import com.peerreview.common.AppLogger;
import com.peerreview.common.Text;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * DeepSeek 实现：走 OpenAI 兼容的 {@code /chat/completions} 协议。
 *
 * <p>只用 JDK 自带的 {@link HttpClient}，不引入额外的 HTTP 框架；
 * 请求体用 Jackson 的节点树拼装，避免手拼字符串时被正文里的引号、换行破坏。</p>
 *
 * <p>错误处理分三层：网络层（连接失败 / 超时）、协议层（HTTP 状态码）、
 * 内容层（返回结构不符）。每一层都翻译成一句用户能看懂的话，
 * 而不是把堆栈或裸 JSON 甩到界面上。</p>
 */
public final class DeepSeekSuggestionProvider implements ReviewSuggestionProvider {

    /** 官方默认接入点；企业代理可改配置指向兼容网关。 */
    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    public static final String DEFAULT_MODEL = "deepseek-chat";

    private static final String CHAT_PATH = "/chat/completions";
    /** 一次生成大约 700~900 token，留出余量避免总评被截断。 */
    private static final int MAX_OUTPUT_TOKENS = 900;

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final Duration timeout;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public DeepSeekSuggestionProvider(String baseUrl, String apiKey, String model,
                                      double temperature, Duration timeout) {
        String normalized = Text.trimToEmpty(baseUrl);
        this.baseUrl = normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
        this.apiKey = Text.trimToEmpty(apiKey);
        this.model = Text.isBlank(model) ? DEFAULT_MODEL : model.trim();
        this.temperature = temperature;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public String displayName() {
        return "DeepSeek · " + model;
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isEmpty();
    }

    @Override
    public String unavailableReason() {
        return "未配置 DeepSeek 密钥（config/config.properties 的 ai.api.key）";
    }

    @Override
    public ReviewSuggestion suggest(SuggestionRequest request) {
        return handle(post(buildPayload(request)), request);
    }

    // ———————————————————— 传输 ————————————————————

    /** 一次请求的往返结果：响应体 + 耗时，避免用可变字段在方法间传值。 */
    private record Exchange(HttpResponse<String> response, long elapsedMillis) {
    }

    private Exchange post(String payload) {
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint())
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        long startedNanos = System.nanoTime();
        try {
            HttpResponse<String> response =
                    http.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Exchange(response, (System.nanoTime() - startedNanos) / 1_000_000);
        } catch (HttpTimeoutException ex) {
            throw new AppException("AI 服务响应超时（超过 " + timeout.toSeconds()
                    + " 秒），请检查网络后重试");
        } catch (IOException ex) {
            throw new AppException("无法连接 AI 服务：" + rootCause(ex) + "，请检查网络或代理设置");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AppException("AI 请求已取消");
        }
    }

    private URI endpoint() {
        return URI.create(baseUrl + CHAT_PATH);
    }

    private String buildPayload(SuggestionRequest request) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", SuggestionPrompt.system());
        messages.addObject().put("role", "user").put("content", SuggestionPrompt.user(request));
        body.put("temperature", temperature);
        body.put("max_tokens", MAX_OUTPUT_TOKENS);
        body.put("stream", false);
        body.putObject("response_format").put("type", "json_object");
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new AppException("构造 AI 请求失败：" + ex.getMessage());
        }
    }

    // ———————————————————— 响应 ————————————————————

    private ReviewSuggestion handle(Exchange exchange, SuggestionRequest request) {
        HttpResponse<String> response = exchange.response();
        if (response.statusCode() != 200) {
            String detail = errorMessage(response.body());
            AppLogger.get().warn("AI 调用失败：HTTP " + response.statusCode()
                    + (detail.isEmpty() ? "" : "，" + detail));
            throw new AppException(describeStatus(response.statusCode(), detail));
        }

        JsonNode root;
        try {
            root = mapper.readTree(response.body());
        } catch (JsonProcessingException ex) {
            throw new AppException("AI 服务返回格式异常，请稍后重试");
        }

        String content = root.path("choices").path(0).path("message").path("content").asText("");
        int tokens = root.path("usage").path("total_tokens").asInt(0);
        // 服务端可能路由到 flash/reasoner 等变体，以实际返回的模型标识为准，便于日志追溯
        String actualModel = root.path("model").asText(model);
        long elapsed = exchange.elapsedMillis();

        ReviewSuggestion suggestion =
                SuggestionParser.parse(content, actualModel, elapsed, tokens, request.rubricItems());
        AppLogger.get().info("AI 参考意见生成成功：model=" + actualModel
                + " 维度=" + suggestion.hints().size()
                + " 耗时=" + elapsed + "ms tokens=" + tokens);
        return suggestion;
    }

    /** 把状态码翻译成可执行的提示，顺带指出用户下一步该做什么。 */
    private String describeStatus(int status, String detail) {
        String suffix = detail.isEmpty() ? "" : "（" + detail + "）";
        return switch (status) {
            case 400 -> "AI 请求参数有误" + suffix;
            case 401 -> "DeepSeek 密钥无效或已失效，请检查 config/config.properties 中的 ai.api.key";
            case 402 -> "DeepSeek 账户余额不足，请充值后重试";
            case 403 -> "DeepSeek 拒绝访问，请确认密钥权限";
            case 429 -> "调用过于频繁，请稍后再试";
            case 500, 502, 503, 504 -> "AI 服务暂时不可用（HTTP " + status + "），请稍后重试";
            default -> "AI 服务返回异常（HTTP " + status + "）" + suffix;
        };
    }

    private String errorMessage(String body) {
        try {
            String message = mapper.readTree(body).path("error").path("message").asText("");
            return Text.ellipsis(message, 120);
        } catch (JsonProcessingException | RuntimeException ex) {
            return "";
        }
    }

    private String rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return Text.isBlank(message) ? current.getClass().getSimpleName() : Text.ellipsis(message, 80);
    }
}
