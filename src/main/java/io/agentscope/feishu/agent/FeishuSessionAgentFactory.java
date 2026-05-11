package io.agentscope.feishu.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.session.Session;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.feishu.crm.skill.CrmClasspathSkillHolder;
import io.agentscope.feishu.crm.skill.CrmSkillTools;
import io.agentscope.feishu.tools.ApprovalTools;
import io.agentscope.feishu.tools.FeishuLarkTools;
import io.agentscope.feishu.tools.FeishuToolContext;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FeishuSessionAgentFactory {

    private final Session jsonSession;
    private final FeishuLarkTools feishuLarkTools;
    private final ApprovalTools approvalTools;
    private final CrmClasspathSkillHolder crmClasspathSkillHolder;
    private final CrmSkillTools crmSkillTools;
    private final String dashScopeApiKey;
    private final String modelName;

    public FeishuSessionAgentFactory(
            Session jsonSession,
            FeishuLarkTools feishuLarkTools,
            ApprovalTools approvalTools,
            CrmClasspathSkillHolder crmClasspathSkillHolder,
            CrmSkillTools crmSkillTools,
            @Value("${agentscope.model.dashscope-api-key:}") String dashScopeApiKey,
            @Value("${agentscope.model.name:qwen-turbo}") String modelName) {
        this.jsonSession = jsonSession;
        this.feishuLarkTools = feishuLarkTools;
        this.approvalTools = approvalTools;
        this.crmClasspathSkillHolder = crmClasspathSkillHolder;
        this.crmSkillTools = crmSkillTools;
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

        String sysPrompt =
                "你是飞书里的企业助手，简洁专业。需要查当前会话信息时使用 getCurrentChatMetadata；"
                        + "需要给用户额外一条独立消息时用 sendFollowUpTextToCurrentChat。"
                        + "若用户明确要求执行敏感/破坏性操作，先调用 request_sensitive_action_approval 并给出摘要。"
                        + " 涉及企业客户信息、订单统计或合同查询时：先 load_skill_through_path 加载 feishu_crm，再按需调用 crm_send_customer_info_card、crm_send_orders_summary_card 或 contract_send_info_card。"
                        + " 用户整句匹配「查询」+合同编号+「合同」（如 查询HT-20250908192882合同）时：必须先加载 feishu_crm，再必须调用 contract_send_info_card(合同编号)；禁止不经过该工具、仅用自然语言复述合同字段作为最终答复（卡片必须由工具发到会话）。"
                        + " 读不懂用户意图、缺关键信息、或超出能力时：只回一句极短说明，不要展开协助、不要给建议清单。"
                        + " 一旦 crm_send_* 已成功把卡片或文本发到会话：不要再总结或扩展，最终对用户不要再写长文（宁可不再发文字，或至多「已发。」二字）。"
                        + " 其它日常问题仍应正常简短作答，不要无理由留空。";

        SkillBox skillBox = new SkillBox(toolkit);
        skillBox.registration().skill(crmClasspathSkillHolder.feishuCrmSkill()).tool(crmSkillTools).apply();

        ReActAgent agent = ReActAgent.builder()
                .name("FeishuAssistant")
                .sysPrompt(sysPrompt)
                .model(model)
                .memory(new InMemoryMemory())
                .toolkit(toolkit)
                .toolExecutionContext(toolCtx)
                .skillBox(skillBox)
                // 会话 JsonSession 恢复后，若上次停在 TOOL_SUSPENDED / 未完成 tool 结果，下一条用户消息会触发该异常；
                // 开启后由框架自动回收挂起的 tool call，避免用户未点卡片就发新消息时进程崩溃。
                .enablePendingToolRecovery(true)
                .build();

        agent.loadIfExists(jsonSession, sessionId);
        return agent;
    }
}
