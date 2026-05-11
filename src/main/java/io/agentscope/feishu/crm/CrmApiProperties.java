package io.agentscope.feishu.crm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentscope.crm")
public class CrmApiProperties {

    /**
     * CRM 后端根地址，例如 {@code http://127.0.0.1:8080}。留空则使用本进程 {@code server.port} 拼
     * {@code http://127.0.0.1:{port}}。
     */
    private String baseUrl = "";

    /**
     * 客户信息是否用飞书「卡片搭建工具」模板消息回复（msg_type=interactive + type=template）。
     */
    private boolean customerInfoUseTemplateCard = true;

    /**
     * 飞书卡片模板 ID（搭建工具中复制）。
     */
    private String customerInfoCardTemplateId = "AAqtrZyWSproW";

    /**
     * 模板版本号，如 1.0.0；留空表示使用平台当前最新已发布版本。
     */
    private String customerInfoCardTemplateVersion = "";

    /** 订单统计是否用飞书模板卡片回复。 */
    private boolean ordersUseTemplateCard = true;

    private String ordersCardTemplateId = "AAqtroVtGb0Yc";

    private String ordersCardTemplateVersion = "";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isCustomerInfoUseTemplateCard() {
        return customerInfoUseTemplateCard;
    }

    public void setCustomerInfoUseTemplateCard(boolean customerInfoUseTemplateCard) {
        this.customerInfoUseTemplateCard = customerInfoUseTemplateCard;
    }

    public String getCustomerInfoCardTemplateId() {
        return customerInfoCardTemplateId;
    }

    public void setCustomerInfoCardTemplateId(String customerInfoCardTemplateId) {
        this.customerInfoCardTemplateId = customerInfoCardTemplateId;
    }

    public String getCustomerInfoCardTemplateVersion() {
        return customerInfoCardTemplateVersion;
    }

    public void setCustomerInfoCardTemplateVersion(String customerInfoCardTemplateVersion) {
        this.customerInfoCardTemplateVersion = customerInfoCardTemplateVersion;
    }

    public boolean isOrdersUseTemplateCard() {
        return ordersUseTemplateCard;
    }

    public void setOrdersUseTemplateCard(boolean ordersUseTemplateCard) {
        this.ordersUseTemplateCard = ordersUseTemplateCard;
    }

    public String getOrdersCardTemplateId() {
        return ordersCardTemplateId;
    }

    public void setOrdersCardTemplateId(String ordersCardTemplateId) {
        this.ordersCardTemplateId = ordersCardTemplateId;
    }

    public String getOrdersCardTemplateVersion() {
        return ordersCardTemplateVersion;
    }

    public void setOrdersCardTemplateVersion(String ordersCardTemplateVersion) {
        this.ordersCardTemplateVersion = ordersCardTemplateVersion;
    }
}
