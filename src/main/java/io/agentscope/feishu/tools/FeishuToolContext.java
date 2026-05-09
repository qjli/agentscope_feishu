package io.agentscope.feishu.tools;

/**
 * 注入到 Tool 方法中的飞书会话上下文（不暴露给模型 JSON Schema）。
 */
public record FeishuToolContext(String chatId) {}
