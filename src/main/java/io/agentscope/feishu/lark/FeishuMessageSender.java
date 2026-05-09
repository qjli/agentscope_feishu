package io.agentscope.feishu.lark;

import com.lark.oapi.Client;
import com.lark.oapi.service.im.v1.enums.CreateMessageReceiveIdTypeEnum;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FeishuMessageSender {

    private static final Logger log = LoggerFactory.getLogger(FeishuMessageSender.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Client client;

    public FeishuMessageSender(Client client) {
        this.client = client;
    }

    public void replyTextToChat(String chatId, String text) throws Exception {
        String body = MAPPER.createObjectNode().put("text", text).toString();
        CreateMessageReq req = CreateMessageReq.newBuilder()
                .receiveIdType(CreateMessageReceiveIdTypeEnum.CHAT_ID)
                .createMessageReqBody(CreateMessageReqBody.newBuilder()
                        .receiveId(chatId)
                        .msgType("text")
                        .content(body)
                        .build())
                .build();
        CreateMessageResp resp = client.im().message().create(req);
        if (!resp.success()) {
            log.warn("发送消息失败 code={} msg={}", resp.getCode(), resp.getMsg());
        }
    }

    /**
     * 发送交互卡片（用于人在回路确认）。按钮 value 携带 resumeId / sessionId / decision。
     */
    public void sendApprovalCard(String chatId, String headerTitle, String bodyText, String resumeId, String sessionId)
            throws Exception {
        ObjectNode card = MAPPER.createObjectNode();
        card.put("schema", "2.0");
        card.set("config", MAPPER.createObjectNode().put("wide_screen_mode", true));
        card.set(
                "header",
                MAPPER.createObjectNode()
                        .put("template", "red")
                        .set("title", MAPPER.createObjectNode().put("tag", "plain_text").put("content", headerTitle)));
        var elements = MAPPER.createArrayNode();
        elements.add(MAPPER.createObjectNode()
                .put("tag", "div")
                .set("text", MAPPER.createObjectNode().put("tag", "plain_text").put("content", bodyText)));
        var actions = MAPPER.createArrayNode();
        ObjectNode approveBtn = MAPPER.createObjectNode();
        approveBtn.put("tag", "button");
        approveBtn.put("type", "primary");
        approveBtn.set("text", MAPPER.createObjectNode().put("tag", "plain_text").put("content", "批准"));
        ObjectNode approveVal = MAPPER.createObjectNode();
        approveVal.put("resumeId", resumeId);
        approveVal.put("sessionId", sessionId);
        approveVal.put("decision", "approve");
        approveBtn.set("value", approveVal);
        ObjectNode rejectBtn = MAPPER.createObjectNode();
        rejectBtn.put("tag", "button");
        rejectBtn.put("type", "default");
        rejectBtn.set("text", MAPPER.createObjectNode().put("tag", "plain_text").put("content", "拒绝"));
        ObjectNode rejectVal = MAPPER.createObjectNode();
        rejectVal.put("resumeId", resumeId);
        rejectVal.put("sessionId", sessionId);
        rejectVal.put("decision", "reject");
        rejectBtn.set("value", rejectVal);
        actions.add(approveBtn);
        actions.add(rejectBtn);
        elements.add(MAPPER.createObjectNode().put("tag", "action").set("actions", actions));
        card.set("elements", elements);

        String content = card.toString();
        CreateMessageReq req = CreateMessageReq.newBuilder()
                .receiveIdType(CreateMessageReceiveIdTypeEnum.CHAT_ID)
                .createMessageReqBody(CreateMessageReqBody.newBuilder()
                        .receiveId(chatId)
                        .msgType("interactive")
                        .content(content)
                        .build())
                .build();
        CreateMessageResp resp = client.im().message().create(req);
        if (!resp.success()) {
            log.warn("发送卡片失败 code={} msg={}", resp.getCode(), resp.getMsg());
        }
    }
}
