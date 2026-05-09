package io.agentscope.feishu.lark;

import com.github.benmanes.caffeine.cache.Cache;
import com.lark.oapi.service.im.v1.model.EventMessage;
import com.lark.oapi.service.im.v1.model.EventSender;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.session.Session;
import io.agentscope.feishu.agent.FeishuSessionAgentFactory;
import io.agentscope.feishu.session.FeishuSessionLockRegistry;
import io.agentscope.feishu.session.SessionIdSanitizer;
import java.util.List;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class FeishuMessageEventService {

    private static final Logger log = LoggerFactory.getLogger(FeishuMessageEventService.class);

    private final Executor feishuEventExecutor;
    private final FeishuSessionAgentFactory agentFactory;
    private final FeishuMessageSender messageSender;
    private final Session jsonSession;
    private final Cache<String, Boolean> processedMessageIds;
    private final FeishuSessionLockRegistry lockRegistry;
    private final PendingApprovalService pendingApprovalService;

    public FeishuMessageEventService(
            @Qualifier("feishuEventExecutor") Executor feishuEventExecutor,
            FeishuSessionAgentFactory agentFactory,
            FeishuMessageSender messageSender,
            Session agentscopeJsonSession,
            Cache<String, Boolean> processedMessageIds,
            FeishuSessionLockRegistry lockRegistry,
            PendingApprovalService pendingApprovalService) {
        this.feishuEventExecutor = feishuEventExecutor;
        this.agentFactory = agentFactory;
        this.messageSender = messageSender;
        this.jsonSession = agentscopeJsonSession;
        this.processedMessageIds = processedMessageIds;
        this.lockRegistry = lockRegistry;
        this.pendingApprovalService = pendingApprovalService;
    }

    public void handleMessageEvent(P2MessageReceiveV1 event) {
        feishuEventExecutor.execute(() -> {
            try {
                handleMessageEventSync(event);
            } catch (Exception e) {
                log.error("处理飞书消息事件失败", e);
            }
        });
    }

    private void handleMessageEventSync(P2MessageReceiveV1 event) throws Exception {
        if (event.getEvent() == null) {
            return;
        }
        EventMessage message = event.getEvent().getMessage();
        EventSender sender = event.getEvent().getSender();
        if (message == null) {
            return;
        }
        if (sender != null && "app".equalsIgnoreCase(sender.getSenderType())) {
            return;
        }
        String messageId = message.getMessageId();
        if (messageId != null && !messageId.isBlank()) {
            if (processedMessageIds.asMap().putIfAbsent(messageId, Boolean.TRUE) != null) {
                log.debug("跳过重复 message_id={}", messageId);
                return;
            }
        }
        String chatId = message.getChatId();
        String openId =
                sender != null && sender.getSenderId() != null ? sender.getSenderId().getOpenId() : "";
        String threadSuffix =
                message.getThreadId() != null && !message.getThreadId().isBlank()
                        ? SessionIdSanitizer.normalizeThreadSuffix(message.getThreadId())
                        : "";
        String sessionId =
                SessionIdSanitizer.sessionIdFromChatAndUser(chatId, openId) + threadSuffix;

        String text = FeishuTextContentParser.extractText(message.getMessageType(), message.getContent())
                .orElse("");
        if (text.isBlank()) {
            return;
        }

        Object lock = lockRegistry.lockFor(sessionId);
        synchronized (lock) {
            if (!agentFactory.isModelConfigured()) {
                messageSender.replyTextToChat(
                        chatId, "（未配置模型）Echo: " + text + "\n\n请设置环境变量 AGENTSCOPE_MODEL_DASHSCOPE_API_KEY 或配置 agentscope.model.dashscope-api-key。");
                return;
            }

            ReActAgent agent = agentFactory.createAgentForSession(sessionId, chatId);
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .name("feishu_user")
                    .textContent(text)
                    .build();

            Msg response = agent.call(userMsg).block();
            if (response == null) {
                messageSender.replyTextToChat(chatId, "未收到模型响应");
                return;
            }

            if (response.getGenerateReason() == GenerateReason.TOOL_SUSPENDED) {
                List<ToolUseBlock> pending = response.getContentBlocks(ToolUseBlock.class);
                pendingApprovalService.registerAndSendCard(agent, sessionId, chatId, pending);
                return;
            }

            String reply = response.getTextContent();
            if (reply == null || reply.isBlank()) {
                reply = "（无文本回复）";
            }
            messageSender.replyTextToChat(chatId, reply);
            agent.saveTo(jsonSession, sessionId);
        }
    }
}
