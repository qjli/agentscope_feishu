package io.agentscope.feishu.session;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 为 JsonSession 生成安全 sessionId，防止路径穿越（参见 AgentScope Session 文档）。
 */
public final class SessionIdSanitizer {

    private static final Pattern SAFE_SEGMENT = Pattern.compile("^[a-zA-Z0-9_-]{1,128}$");

    private SessionIdSanitizer() {}

    public static String requireSafeSegment(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        String s = raw.trim();
        if (!SAFE_SEGMENT.matcher(s).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " 仅允许字母数字下划线与中划线，且长度 1-128: " + s);
        }
        return s;
    }

    /**
     * 使用 chatId 与 openId 构造会话主键（已分别校验安全字符）。
     */
    public static String sessionIdFromChatAndUser(String chatId, String openId) {
        String c = requireSafeSegment(chatId, "chatId");
        String o = requireSafeSegment(openId, "openId");
        return c + "_" + o;
    }

    public static String normalizeThreadSuffix(String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return "";
        }
        String t = requireSafeSegment(threadId, "threadId");
        return "_t_" + t.toLowerCase(Locale.ROOT);
    }
}
