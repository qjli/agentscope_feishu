package io.agentscope.feishu.contract;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentscope.contract")
public class ContractProperties {

    /** 是否用飞书模板卡片展示合同信息。 */
    private boolean useTemplateCard = true;

    /** 合同信息卡片模板 ID（卡片搭建工具）。 */
    private String cardTemplateId = "AAqtmy18CRaGt";

    private String cardTemplateVersion = "";

    public boolean isUseTemplateCard() {
        return useTemplateCard;
    }

    public void setUseTemplateCard(boolean useTemplateCard) {
        this.useTemplateCard = useTemplateCard;
    }

    public String getCardTemplateId() {
        return cardTemplateId;
    }

    public void setCardTemplateId(String cardTemplateId) {
        this.cardTemplateId = cardTemplateId;
    }

    public String getCardTemplateVersion() {
        return cardTemplateVersion;
    }

    public void setCardTemplateVersion(String cardTemplateVersion) {
        this.cardTemplateVersion = cardTemplateVersion;
    }
}
