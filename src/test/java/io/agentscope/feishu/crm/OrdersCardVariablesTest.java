package io.agentscope.feishu.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agentscope.feishu.crm.dto.OrderLine;
import io.agentscope.feishu.crm.dto.OrdersSummaryResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrdersCardVariablesTest {

    @Test
    void mapsToFeishuTemplateKeys() {
        OrdersSummaryResponse r = new OrdersSummaryResponse(
                "测试公司",
                "ALL",
                100,
                "¥1",
                60,
                "¥0.6",
                List.of(new OrderLine("1", "t", "¥0", "PENDING", "")),
                40,
                55);
        var n = OrdersCardVariables.toTemplateVariableNode(r);
        assertEquals("测试公司", n.get("companyName").asText());
        assertEquals("40", n.get("unSettledNum").asText());
        assertEquals("60", n.get("settledNum").asText());
        assertEquals("55", n.get("invoicedNum").asText());
        assertEquals("100", n.get("totalNum").asText());
    }
}
