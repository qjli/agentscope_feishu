package io.agentscope.feishu.crm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.agentscope.feishu.crm.dto.OrdersSummaryResponse;

/**
 * 将 /api/crm/orders 返回映射为飞书订单统计卡片变量（模板 AAqtroVtGb0Yc）。
 */
public final class OrdersCardVariables {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OrdersCardVariables() {}

    public static ObjectNode toTemplateVariableNode(OrdersSummaryResponse r) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("companyName", nz(r.companyName()));
        n.put("unSettledNum", String.valueOf(r.unSettledNum()));
        n.put("settledNum", String.valueOf(r.settledCount()));
        n.put("invoicedNum", String.valueOf(r.invoicedNum()));
        n.put("totalNum", String.valueOf(r.totalCount()));
        return n;
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s.strip();
    }
}
