package io.agentscope.feishu.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.feishu.config.FeishuProperties;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LarkEventBodyNormalizerTest {

    private static final ObjectMapper OM = new ObjectMapper();

    @Test
    void wrapsFlatCardActionToSchema2() throws Exception {
        String flat =
                """
                {"app_id":"cli_x","open_id":"ou_x","open_message_id":"om_x","open_chat_id":"oc_x","tenant_key":"tk","token":"c-abc","action":{"tag":"button","form_value":{"k":"v"}}}
                """
                        .strip();
        FeishuProperties p = new FeishuProperties();
        p.setVerificationToken("verify-tok");

        byte[] out = LarkEventBodyNormalizer.maybeWrapFlatCardAction(flat.getBytes(StandardCharsets.UTF_8), p);
        JsonNode root = OM.readTree(out);
        assertEquals("2.0", root.path("schema").asText());
        assertEquals("card.action.trigger", root.path("header").path("event_type").asText());
        assertEquals("verify-tok", root.path("header").path("token").asText());
        assertTrue(root.path("event").path("action").path("form_value").path("k").asText().equals("v"));
        assertEquals("om_x", root.path("event").path("context").path("open_message_id").asText());
    }

    @Test
    void leavesEnvelopeUnchanged() throws Exception {
        String env =
                """
                {"schema":"2.0","header":{"event_type":"card.action.trigger","token":"t"},"event":{}}
                """
                        .strip();
        FeishuProperties p = new FeishuProperties();
        byte[] raw = env.getBytes(StandardCharsets.UTF_8);
        byte[] out = LarkEventBodyNormalizer.maybeWrapFlatCardAction(raw, p);
        assertEquals(env, new String(out, StandardCharsets.UTF_8));
    }
}
