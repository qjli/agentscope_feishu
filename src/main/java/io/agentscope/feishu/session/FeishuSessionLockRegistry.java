package io.agentscope.feishu.session;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class FeishuSessionLockRegistry {

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public Object lockFor(String sessionId) {
        return locks.computeIfAbsent(sessionId, k -> new Object());
    }
}
