package io.agentscope.feishu.lark;

import com.lark.oapi.event.cardcallback.model.CallBackToast;
import com.lark.oapi.event.cardcallback.model.P2CardActionTrigger;
import com.lark.oapi.event.cardcallback.model.P2CardActionTriggerResponse;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.session.Session;
import io.agentscope.feishu.session.FeishuSessionLockRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PendingApprovalService {

    private static final Logger log = LoggerFactory.getLogger(PendingApprovalService.class);

    private final ConcurrentHashMap<String, PendingApproval> pendingByResumeId = new ConcurrentHashMap<>();
    private final FeishuMessageSender messageSender;
    private final Session session;
    private final FeishuSessionLockRegistry lockRegistry;
    private final Executor feishuEventExecutor;

    public PendingApprovalService(
            FeishuMessageSender messageSender,
            Session agentscopeJsonSession,
            FeishuSessionLockRegistry lockRegistry,
            @Qualifier("feishuEventExecutor") Executor feishuEventExecutor) {
        this.messageSender = messageSender;
        this.session = agentscopeJsonSession;
        this.lockRegistry = lockRegistry;
        this.feishuEventExecutor = feishuEventExecutor;
    }

    public void registerAndSendCard(
            ReActAgent agent, String sessionId, String chatId, List<ToolUseBlock> toolUses) throws Exception {
        String resumeId = UUID.randomUUID().toString();
        pendingByResumeId.put(resumeId, new PendingApproval(agent, sessionId, chatId, new ArrayList<>(toolUses)));
        String summary = extractSummary(toolUses);
        messageSender.sendApprovalCard(chatId, "请确认操作", summary, resumeId, sessionId);
        agent.saveTo(session, sessionId);
    }

    private static String extractSummary(List<ToolUseBlock> toolUses) {
        if (toolUses == null || toolUses.isEmpty()) {
            return "有待审批的工具调用";
        }
        ToolUseBlock first = toolUses.get(0);
        Object s = first.getInput() != null ? first.getInput().get("summary") : null;
        return s != null ? s.toString() : "有待审批的工具调用";
    }

    public P2CardActionTriggerResponse handleCardAction(P2CardActionTrigger event) {
        P2CardActionTriggerResponse resp = new P2CardActionTriggerResponse();
        CallBackToast toast = new CallBackToast();
        toast.setType("info");
        toast.setContent("已收到，正在处理");
        resp.setToast(toast);

        Map<String, Object> value =
                event.getEvent() != null && event.getEvent().getAction() != null
                        ? event.getEvent().getAction().getValue()
                        : null;
        if (value == null) {
            return resp;
        }
        String resumeId = str(value.get("resumeId"));
        String sessionId = str(value.get("sessionId"));
        String decision = str(value.get("decision"));
        if (resumeId.isBlank() || sessionId.isBlank() || decision.isBlank()) {
            log.warn("卡片回调缺少参数 value={}", value);
            return resp;
        }

        feishuEventExecutor.execute(() -> resumeAfterCard(resumeId, sessionId, decision));
        return resp;
    }

    private void resumeAfterCard(String resumeId, String sessionId, String decision) {
        Object lock = lockRegistry.lockFor(sessionId);
        synchronized (lock) {
            try {
                PendingApproval pending = pendingByResumeId.remove(resumeId);
                if (pending == null) {
                    log.warn("未找到待恢复记录 resumeId={}", resumeId);
                    return;
                }
                boolean approved = "approve".equalsIgnoreCase(decision);
                String resultText = approved ? "人工已批准，可继续执行。" : "人工已拒绝该操作。";
                List<ContentBlock> blocks = new ArrayList<>();
                for (ToolUseBlock t : pending.toolUses()) {
                    blocks.add(ToolResultBlock.of(
                            t.getId(), t.getName(), TextBlock.builder().text(resultText).build()));
                }
                Msg toolMsg = Msg.builder().role(MsgRole.TOOL).content(blocks).build();
                Msg finalResp = pending.agent().call(toolMsg).block();
                if (finalResp != null) {
                    pending.agent().saveTo(session, sessionId);
                    String out =
                            finalResp.getTextContent() != null && !finalResp.getTextContent().isBlank()
                                    ? finalResp.getTextContent()
                                    : (finalResp.getGenerateReason() == GenerateReason.TOOL_CALLS
                                            ? "需要继续处理工具调用"
                                            : "处理完成");
                    messageSender.replyTextToChat(pending.chatId(), out);
                }
            } catch (Exception e) {
                log.error("恢复 Agent 失败 resumeId={}", resumeId, e);
            }
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private record PendingApproval(
            ReActAgent agent, String sessionId, String chatId, List<ToolUseBlock> toolUses) {}
}
