package io.agentscope.feishu.contract;

import io.agentscope.feishu.contract.dto.ContractInfoResponse;
import io.agentscope.feishu.lark.FeishuMessageSender;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 固定句式「查询&lt;合同编号&gt;合同」直连拉取合同并发飞书模板卡片，不依赖大模型是否调用工具，保证卡片展示。
 */
@Component
public class ContractQueryShortcut {

    private static final Logger log = LoggerFactory.getLogger(ContractQueryShortcut.class);

    private static final Pattern CONTRACT_QUERY =
            Pattern.compile("^查询\\s*(.+?)\\s*合同\\s*$", Pattern.DOTALL);

    private final ContractRemoteClient contractRemoteClient;
    private final FeishuMessageSender messageSender;
    private final ContractProperties contractProperties;
    private final ContractReplyFormatter contractReplyFormatter;

    public ContractQueryShortcut(
            ContractRemoteClient contractRemoteClient,
            FeishuMessageSender messageSender,
            ContractProperties contractProperties,
            ContractReplyFormatter contractReplyFormatter) {
        this.contractRemoteClient = contractRemoteClient;
        this.messageSender = messageSender;
        this.contractProperties = contractProperties;
        this.contractReplyFormatter = contractReplyFormatter;
    }

    /**
     * @return 已按合同句式处理并发消息，则 true（调用方应跳过后续 Agent）
     */
    public boolean tryHandle(String chatId, String userText) throws Exception {
        if (userText == null) {
            return false;
        }
        String t = userText.strip();
        Matcher m = CONTRACT_QUERY.matcher(t);
        if (!m.matches()) {
            return false;
        }
        String code = m.group(1) == null ? "" : m.group(1).strip();
        if (code.isEmpty()) {
            return false;
        }

        ContractInfoResponse dto = contractRemoteClient.fetchContractInfo(code);
        boolean useCard =
                contractProperties.isUseTemplateCard()
                        && contractProperties.getCardTemplateId() != null
                        && !contractProperties.getCardTemplateId().isBlank();
        if (useCard) {
            boolean ok = messageSender.sendContractTemplateCard(chatId, dto);
            if (ok) {
                log.info("合同查询句式直连：已发送模板卡片 chatId={} contractCode={}", chatId, code);
                return true;
            }
            log.warn("合同模板卡片发送失败，回退纯文本 contractCode={}", code);
        }
        messageSender.replyTextToChat(chatId, contractReplyFormatter.format(dto));
        return true;
    }
}
