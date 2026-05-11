package io.agentscope.feishu.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 合同卡片 / HTTP 表单保存统一落日志（与 {@code POST /api/contract/form-save} 行为一致）；飞书回调不经过 HTTP 路由，由此服务承接。
 */
@Service
public class ContractFormSaveService {

    private static final Logger log = LoggerFactory.getLogger(ContractFormSaveService.class);
    private static final ObjectMapper OM = new ObjectMapper();

    /** 飞书同一次操作可能先发扁平体再发 2.0 信封，短时去重避免两条相同保存日志。 */
    private final Cache<String, Boolean> recentDedup =
            Caffeine.newBuilder().maximumSize(50_000).expireAfterWrite(Duration.ofSeconds(25)).build();

    public void recordFormSave(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return;
        }
        String dedupeKey = buildDedupeKey(payload);
        if (dedupeKey != null && recentDedup.asMap().putIfAbsent(dedupeKey, Boolean.TRUE) != null) {
            log.debug("[合同表单保存] 跳过重复 key={}", dedupeKey);
            return;
        }
        try {
            log.info("[合同表单保存] 收到 JSON：\n{}", OM.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
        } catch (Exception e) {
            log.info("[合同表单保存] 收到 payload（序列化失败）: {}", payload);
        }
    }

    private static String buildDedupeKey(JsonNode payload) {
        String msgId = payload.path("context").path("open_message_id").asText("");
        JsonNode fv = payload.get("form_value");
        if (fv != null && fv.isObject() && !msgId.isBlank()) {
            return msgId + ":" + fv.toString().hashCode();
        }
        return "body:" + payload.toString().hashCode();
    }
}
