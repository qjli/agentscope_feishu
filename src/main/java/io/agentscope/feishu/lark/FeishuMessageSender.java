package io.agentscope.feishu.lark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lark.oapi.Client;
import com.lark.oapi.service.im.v1.enums.CreateMessageReceiveIdTypeEnum;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import io.agentscope.feishu.contract.ContractCardVariables;
import io.agentscope.feishu.contract.ContractProperties;
import io.agentscope.feishu.contract.dto.ContractInfoResponse;
import io.agentscope.feishu.crm.CrmApiProperties;
import io.agentscope.feishu.crm.CustomerInfoCardVariables;
import io.agentscope.feishu.crm.OrdersCardVariables;
import io.agentscope.feishu.crm.dto.CustomerInfoResponse;
import io.agentscope.feishu.crm.dto.OrdersSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FeishuMessageSender {

    private static final Logger log = LoggerFactory.getLogger(FeishuMessageSender.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Client client;
    private final CrmApiProperties crmApiProperties;
    private final ContractProperties contractProperties;

    public FeishuMessageSender(Client client, CrmApiProperties crmApiProperties, ContractProperties contractProperties) {
        this.client = client;
        this.crmApiProperties = crmApiProperties;
        this.contractProperties = contractProperties;
    }

    /**
     * 发送「卡片搭建工具」模板卡片（参见飞书文档：msg_type=interactive，content 内 type=template）。
     *
     * @return 是否调用成功（HTTP 业务成功）
     */
    public boolean sendCustomerInfoTemplateCard(String chatId, CustomerInfoResponse data) throws Exception {
        return sendInteractiveTemplate(
                chatId,
                crmApiProperties.getCustomerInfoCardTemplateId(),
                crmApiProperties.getCustomerInfoCardTemplateVersion(),
                CustomerInfoCardVariables.toTemplateVariableNode(data));
    }

    /** 订单统计模板卡片（变量 companyName / unSettledNum / settledNum / invoicedNum / totalNum）。 */
    public boolean sendOrdersTemplateCard(String chatId, OrdersSummaryResponse data) throws Exception {
        return sendInteractiveTemplate(
                chatId,
                crmApiProperties.getOrdersCardTemplateId(),
                crmApiProperties.getOrdersCardTemplateVersion(),
                OrdersCardVariables.toTemplateVariableNode(data));
    }

    /** 合同信息模板卡片（变量 contractCode、contractName、partyA、partyB、contractContext、signDate）。 */
    public boolean sendContractTemplateCard(String chatId, ContractInfoResponse data) throws Exception {
        return sendInteractiveTemplate(
                chatId,
                contractProperties.getCardTemplateId(),
                contractProperties.getCardTemplateVersion(),
                ContractCardVariables.toTemplateVariableNode(data));
    }

    private boolean sendInteractiveTemplate(String chatId, String templateId, String templateVersion, JsonNode templateVariable)
            throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "template");
        ObjectNode templateData = MAPPER.createObjectNode();
        templateData.put("template_id", templateId);
        if (templateVersion != null && !templateVersion.isBlank()) {
            templateData.put("template_version_name", templateVersion.strip());
        }
        templateData.set("template_variable", templateVariable);
        root.set("data", templateData);

        String content = MAPPER.writeValueAsString(root);
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
            log.warn(
                    "发送模板卡片失败 templateId={} code={} msg={} contentPreview={}",
                    templateId,
                    resp.getCode(),
                    resp.getMsg(),
                    content.length() > 800 ? content.substring(0, 800) + "…" : content);
            return false;
        }
        return true;
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
