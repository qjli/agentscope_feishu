package io.agentscope.feishu.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.session.Session;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.Toolkit;
import java.util.HashMap;
import java.util.Map;
import io.agentscope.feishu.tools.ApprovalTools;
import io.agentscope.feishu.tools.FeishuLarkTools;
import io.agentscope.feishu.tools.FeishuToolContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FeishuSessionAgentFactory {

    private final Session jsonSession;
    private final FeishuLarkTools feishuLarkTools;
    private final ApprovalTools approvalTools;
    private final String dashScopeApiKey;
    private final String modelName;

    public FeishuSessionAgentFactory(
            Session jsonSession,
            FeishuLarkTools feishuLarkTools,
            ApprovalTools approvalTools,
            @Value("${agentscope.model.dashscope-api-key:}") String dashScopeApiKey,
            @Value("${agentscope.model.name:qwen-turbo}") String modelName) {
        this.jsonSession = jsonSession;
        this.feishuLarkTools = feishuLarkTools;
        this.approvalTools = approvalTools;
        this.dashScopeApiKey = dashScopeApiKey;
        this.modelName = modelName;
    }

    public Session getJsonSession() {
        return jsonSession;
    }

    public boolean isModelConfigured() {
        return dashScopeApiKey != null && !dashScopeApiKey.isBlank();
    }

    /**
     * 为一次会话构造独立 ReActAgent（禁止跨请求共享有状态实例）。
     */
    public ReActAgent createAgentForSession(String sessionId, String chatId) {
        Toolkit toolkit = new Toolkit();
        Map<String, Map<String, Object>> presets = new HashMap<>();
        presets.put("feishuPing", Map.of("marker", "server-preset"));
        presets.put("feishu_ping", Map.of("marker", "server-preset"));
        toolkit.registration().tool(feishuLarkTools).presetParameters(presets).apply();
        toolkit.registerTool(approvalTools);

        ToolExecutionContext toolCtx =
                ToolExecutionContext.builder().register(new FeishuToolContext(chatId)).build();

        if (!isModelConfigured()) {
            throw new IllegalStateException("未配置 agentscope.model.dashscope-api-key，无法创建 Agent");
        }

        var model = DashScopeChatModel.builder()
                .apiKey(dashScopeApiKey)
                .modelName(modelName)
                .build();

        ReActAgent agent = ReActAgent.builder()
                .name("FeishuAssistant")
                .sysPrompt(
                        "你是飞书里的企业助手，简洁专业。需要查当前会话信息时使用 getCurrentChatMetadata；"
                                + "需要给用户额外一条独立消息时用 sendFollowUpTextToCurrentChat。"
                                + "若用户明确要求执行敏感/破坏性操作，先调用 request_sensitive_action_approval 并给出摘要。")
                .model(model)
                .memory(new InMemoryMemory())
                .toolkit(toolkit)
                .toolExecutionContext(toolCtx)
                .build();

        agent.loadIfExists(jsonSession, sessionId);
        return agent;
    }
}
