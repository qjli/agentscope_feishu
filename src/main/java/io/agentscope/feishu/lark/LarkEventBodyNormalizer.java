package io.agentscope.feishu.lark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.agentscope.feishu.config.FeishuProperties;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 飞书卡片「保存」等场景会先 POST 一份<strong>无 schema 信封</strong>的扁平 JSON，{@link com.lark.oapi.event.EventDispatcher}
 * 无法解析 {@code event_type} 会抛 {@code HandlerNotFoundException}。本类在入站时将其规范化为 {@code schema 2.0 +
 * card.action.trigger} 结构。
 */
public final class LarkEventBodyNormalizer {

    private static final ObjectMapper OM = new ObjectMapper();

    private LarkEventBodyNormalizer() {}

    public static byte[] maybeWrapFlatCardAction(byte[] rawBody, FeishuProperties feishuProperties) {
        if (rawBody == null || rawBody.length == 0) {
            return rawBody;
        }
        try {
            JsonNode root = OM.readTree(rawBody);
            if (root.has("header") && root.path("header").has("event_type")) {
                return rawBody;
            }
            if (root.has("type") && root.has("challenge")) {
                return rawBody;
            }
            if (!looksLikeFlatCardAction(root)) {
                return rawBody;
            }
            byte[] wrapped = OM.writeValueAsBytes(wrapToCardActionEnvelope((ObjectNode) root, feishuProperties));
            return wrapped;
        } catch (Exception e) {
            return rawBody;
        }
    }

    private static boolean looksLikeFlatCardAction(JsonNode root) {
        if (!root.isObject() || !root.has("action")) {
            return false;
        }
        JsonNode action = root.get("action");
        if (!action.isObject()) {
            return false;
        }
        if (action.has("form_value")) {
            return true;
        }
        String token = root.path("token").asText("");
        return token.startsWith("c-") && "button".equals(action.path("tag").asText(""));
    }

    private static ObjectNode wrapToCardActionEnvelope(ObjectNode flat, FeishuProperties props) {
        ObjectNode envelope = OM.createObjectNode();
        envelope.put("schema", "2.0");

        ObjectNode header = envelope.putObject("header");
        header.put("event_id", UUID.randomUUID().toString().replace("-", ""));
        header.put("token", nz(props.getVerificationToken()));
        header.put("create_time", String.valueOf(System.currentTimeMillis() * 1_000_000L));
        header.put("event_type", "card.action.trigger");
        copyIfPresent(flat, header, "tenant_key");
        copyIfPresent(flat, header, "app_id");

        ObjectNode event = envelope.putObject("event");
        ObjectNode operator = event.putObject("operator");
        copyIfPresent(flat, operator, "tenant_key");
        copyIfPresent(flat, operator, "open_id");
        copyIfPresent(flat, operator, "union_id");
        copyIfPresent(flat, event, "token");
        event.set("action", flat.get("action"));
        event.put("host", "im_message");
        ObjectNode context = event.putObject("context");
        copyIfPresent(flat, context, "open_message_id");
        copyIfPresent(flat, context, "open_chat_id");
        return envelope;
    }

    private static void copyIfPresent(ObjectNode from, ObjectNode to, String field) {
        if (from.has(field) && !from.get(field).isNull()) {
            to.set(field, from.get(field));
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
