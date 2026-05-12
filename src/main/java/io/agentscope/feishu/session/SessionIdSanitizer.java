package io.agentscope.feishu.session;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 为 JsonSession 生成安全 sessionId，防止路径穿越（参见 AgentScope Session 文档）。
 *
 * SessionIdSanitizer 的作用就是：
 * 在把飞书传来的 chatId、openId、threadId 拼成 sessionId 之前，强制校验成「安全片段」，只允许白名单字符与长度，从源头堵住这类风险。
 *
 *
 *
 * SessionIdSanitizer 是 JsonSession 磁盘路径前的最后一道闸门：
 * 把 chatId、openId、threadId 限制在 字母数字 + _ + -、长度 1～128，从而 防止路径穿越；
 * sessionIdFromChatAndUser 负责主键，
 * normalizeThreadSuffix 在话题场景下追加 _t_<threadId小写>，与 FeishuMessageEventService 中的会话隔离逻辑一致。
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
