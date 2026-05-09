package io.agentscope.feishu.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SessionIdSanitizerTest {

    @Test
    void buildsSessionId() {
        assertEquals("oc_abc_ou_xyz", SessionIdSanitizer.sessionIdFromChatAndUser("oc_abc", "ou_xyz"));
    }

    @Test
    void rejectsUnsafe() {
        assertThrows(IllegalArgumentException.class, () -> SessionIdSanitizer.sessionIdFromChatAndUser("../x", "ou_1"));
    }

    @Test
    void threadSuffix() {
        assertEquals("_t_th_1", SessionIdSanitizer.normalizeThreadSuffix("th_1"));
    }
}
