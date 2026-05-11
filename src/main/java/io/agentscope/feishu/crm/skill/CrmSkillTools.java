package io.agentscope.feishu.crm.skill;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.feishu.contract.ContractProperties;
import io.agentscope.feishu.contract.ContractRemoteClient;
import io.agentscope.feishu.contract.ContractReplyFormatter;
import io.agentscope.feishu.crm.CrmApiProperties;
import io.agentscope.feishu.crm.CrmRemoteClient;
import io.agentscope.feishu.crm.CrmReplyFormatter;
import io.agentscope.feishu.lark.FeishuMessageSender;
import io.agentscope.feishu.tools.FeishuToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 绑定到 {@code feishu_crm} 技能的渐进式工具：CRM/合同 HTTP 与飞书模板卡片或纯文本。
 */
@Component
public class CrmSkillTools {

    private static final Logger log = LoggerFactory.getLogger(CrmSkillTools.class);

    /** 回传给模型：约束其不要再对用户总结卡片或扩展说明。 */
    private static final String MODEL_SILENCE_AFTER_SEND =
            "【工具回执】已发到当前会话。你对用户的最终回复：不要总结卡片、不要复述字段、不要延伸建议；不要写长文，宁可不再发文字，至多「已发。」二字。";

    private final CrmRemoteClient crmRemoteClient;
    private final ContractRemoteClient contractRemoteClient;
    private final FeishuMessageSender messageSender;
    private final CrmApiProperties crmApiProperties;
    private final ContractProperties contractProperties;
    private final CrmReplyFormatter crmReplyFormatter;
    private final ContractReplyFormatter contractReplyFormatter;

    public CrmSkillTools(
            CrmRemoteClient crmRemoteClient,
            ContractRemoteClient contractRemoteClient,
            FeishuMessageSender messageSender,
            CrmApiProperties crmApiProperties,
            ContractProperties contractProperties,
            CrmReplyFormatter crmReplyFormatter,
            ContractReplyFormatter contractReplyFormatter) {
        this.crmRemoteClient = crmRemoteClient;
        this.contractRemoteClient = contractRemoteClient;
        this.messageSender = messageSender;
        this.crmApiProperties = crmApiProperties;
        this.contractProperties = contractProperties;
        this.crmReplyFormatter = crmReplyFormatter;
        this.contractReplyFormatter = contractReplyFormatter;
    }

    @Tool(
            name = "crm_send_customer_info_card",
            description = "按公司名查询客户基本信息，并向当前飞书会话发送客户模板卡片（若关闭卡片则发送纯文本）。需已通过 load_skill_through_path 激活 feishu_crm 技能。成功后不要向用户总结或扩展，见工具返回中的约束。")
    public Mono<String> crmSendCustomerInfoCard(
            @ToolParam(name = "companyName", description = "企业名称") String companyName, FeishuToolContext ctx) {
        return Mono.fromCallable(() -> doSendCustomer(companyName, ctx))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("crm_send_customer_info_card", e))
                .onErrorResume(e -> Mono.just("【CRM】客户信息查询失败：" + e.getMessage()));
    }

    @Tool(
            name = "crm_send_orders_summary_card",
            description = "按公司名与订单状态查询订单汇总，并向当前飞书会话发送订单模板卡片（若关闭卡片则发送纯文本）。orderStatus 取 ALL 或 SETTLED。成功后不要向用户总结或扩展，见工具返回中的约束。")
    public Mono<String> crmSendOrdersSummaryCard(
            @ToolParam(name = "companyName", description = "企业名称") String companyName,
            @ToolParam(name = "orderStatus", description = "ALL=全部订单统计；SETTLED=仅已结算") String orderStatus,
            FeishuToolContext ctx) {
        return Mono.fromCallable(() -> doSendOrders(companyName, normalizeOrderStatus(orderStatus), ctx))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("crm_send_orders_summary_card", e))
                .onErrorResume(e -> Mono.just("【CRM】订单统计查询失败：" + e.getMessage()));
    }

    @Tool(
            name = "contract_send_info_card",
            description = "按合同编号查询合同信息，并向当前飞书会话发送合同模板卡片（变量含 contractCode 等）。例如用户说「查询HT-20250908192882合同」。成功后不要向用户总结或扩展，见工具返回中的约束。")
    public Mono<String> contractSendInfoCard(
            @ToolParam(name = "contractCode", description = "合同编号，如 HT-20250908192882") String contractCode,
            FeishuToolContext ctx) {
        return Mono.fromCallable(() -> doSendContract(contractCode, ctx))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("contract_send_info_card", e))
                .onErrorResume(e -> Mono.just("【合同】查询失败：" + e.getMessage()));
    }

    private String doSendCustomer(String companyName, FeishuToolContext ctx) throws Exception {
        String name = companyName == null ? "" : companyName.strip();
        if (name.isEmpty()) {
            return "companyName 不能为空";
        }
        var dto = crmRemoteClient.fetchCustomerInfo(name);
        String chatId = ctx.chatId();
        if (crmApiProperties.isCustomerInfoUseTemplateCard()
                && crmApiProperties.getCustomerInfoCardTemplateId() != null
                && !crmApiProperties.getCustomerInfoCardTemplateId().isBlank()) {
            boolean ok = messageSender.sendCustomerInfoTemplateCard(chatId, dto);
            if (ok) {
                return MODEL_SILENCE_AFTER_SEND;
            }
            String fallback = crmReplyFormatter.formatCustomer(dto);
            messageSender.replyTextToChat(chatId, fallback);
            return MODEL_SILENCE_AFTER_SEND + "（卡片失败已发纯文本到会话，仍勿总结。）";
        }
        messageSender.replyTextToChat(chatId, crmReplyFormatter.formatCustomer(dto));
        return MODEL_SILENCE_AFTER_SEND + "（纯文本已发到会话，仍勿总结。）";
    }

    private String doSendOrders(String companyName, String orderStatus, FeishuToolContext ctx) throws Exception {
        String name = companyName == null ? "" : companyName.strip();
        if (name.isEmpty()) {
            return "companyName 不能为空";
        }
        var dto = crmRemoteClient.fetchOrders(name, orderStatus);
        String chatId = ctx.chatId();
        if (crmApiProperties.isOrdersUseTemplateCard()
                && crmApiProperties.getOrdersCardTemplateId() != null
                && !crmApiProperties.getOrdersCardTemplateId().isBlank()) {
            boolean ok = messageSender.sendOrdersTemplateCard(chatId, dto);
            if (ok) {
                return MODEL_SILENCE_AFTER_SEND;
            }
            String fallback = crmReplyFormatter.formatOrders(dto);
            messageSender.replyTextToChat(chatId, fallback);
            return MODEL_SILENCE_AFTER_SEND + "（卡片失败已发纯文本到会话，仍勿总结。）";
        }
        messageSender.replyTextToChat(chatId, crmReplyFormatter.formatOrders(dto));
        return MODEL_SILENCE_AFTER_SEND + "（纯文本已发到会话，仍勿总结。）";
    }

    private static String normalizeOrderStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "ALL";
        }
        String u = raw.strip().toUpperCase();
        if ("SETTLED".equals(u)) {
            return "SETTLED";
        }
        return "ALL";
    }

    private String doSendContract(String contractCode, FeishuToolContext ctx) throws Exception {
        String code = contractCode == null ? "" : contractCode.strip();
        if (code.isEmpty()) {
            return "contractCode 不能为空";
        }
        var dto = contractRemoteClient.fetchContractInfo(code);
        String chatId = ctx.chatId();
        if (contractProperties.isUseTemplateCard()
                && contractProperties.getCardTemplateId() != null
                && !contractProperties.getCardTemplateId().isBlank()) {
            boolean ok = messageSender.sendContractTemplateCard(chatId, dto);
            if (ok) {
                return MODEL_SILENCE_AFTER_SEND;
            }
            String fallback = contractReplyFormatter.format(dto);
            messageSender.replyTextToChat(chatId, fallback);
            return MODEL_SILENCE_AFTER_SEND + "（卡片失败已发纯文本到会话，仍勿总结。）";
        }
        messageSender.replyTextToChat(chatId, contractReplyFormatter.format(dto));
        return MODEL_SILENCE_AFTER_SEND + "（纯文本已发到会话，仍勿总结。）";
    }
}
