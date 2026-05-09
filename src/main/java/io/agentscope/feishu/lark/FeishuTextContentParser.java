package io.agentscope.feishu.lark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

/**
 * 解析飞书 IM 事件中 {@code EventMessage.content} JSON 字符串中的文本。
 */
public final class FeishuTextContentParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FeishuTextContentParser() {}

    public static Optional<String> extractText(String messageType, String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = MAPPER.readTree(contentJson);
            if ("text".equalsIgnoreCase(messageType)) {
                JsonNode text = root.get("text");
                return text != null && text.isTextual() ? Optional.of(text.asText()) : Optional.empty();
            }
            // 其他类型可按需扩展
            JsonNode text = root.get("text");
            if (text != null && text.isTextual()) {
                return Optional.of(text.asText());
            }
        } catch (Exception ignored) {
            // 非 JSON 或结构不匹配
        }
        return Optional.empty();
    }
}
