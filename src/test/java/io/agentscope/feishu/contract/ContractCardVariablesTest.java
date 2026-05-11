package io.agentscope.feishu.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agentscope.feishu.contract.dto.ContractInfoResponse;
import org.junit.jupiter.api.Test;

class ContractCardVariablesTest {

    @Test
    void mapsTemplateVariableKeys() {
        var n = ContractCardVariables.toTemplateVariableNode(new ContractInfoResponse(
                "HT-1", "名称", "甲", "乙", "标的说明", "2025-09-10"));
        assertEquals("HT-1", n.get("contractCode").asText());
        assertEquals("名称", n.get("contractName").asText());
        assertEquals("甲", n.get("partyA").asText());
        assertEquals("乙", n.get("partyB").asText());
        assertEquals("标的说明", n.get("contractContext").asText());
        assertEquals("2025-09-10", n.get("signDate").asText());
    }
}
