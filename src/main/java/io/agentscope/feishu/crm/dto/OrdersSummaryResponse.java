package io.agentscope.feishu.crm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrdersSummaryResponse(
        String companyName,
        String orderStatusFilter,
        int totalCount,
        String totalAmount,
        int settledCount,
        String settledAmount,
        List<OrderLine> recentOrders,
        /** 未结算订单笔数（飞书卡片变量 unSettledNum） */
        int unSettledNum,
        /** 已开票笔数（飞书卡片变量 invoicedNum） */
        int invoicedNum) {}
