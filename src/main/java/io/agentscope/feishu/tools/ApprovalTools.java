package io.agentscope.feishu.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.ToolSuspendException;
import org.springframework.stereotype.Component;

/**
 * 演示敏感操作前通过 ToolSuspend 暂停，等待飞书卡片回调恢复。
 */
@Component
public class ApprovalTools {

    @Tool(
            name = "request_sensitive_action_approval",
            description = "在执行可能影响数据或权限的操作之前，请求人工在飞书中通过卡片确认。")
    public String requestSensitiveActionApproval(
            @ToolParam(name = "summary", description = "需要用户确认的操作摘要") String summary) {
        throw new ToolSuspendException("等待人工审批: " + summary);
    }
}
