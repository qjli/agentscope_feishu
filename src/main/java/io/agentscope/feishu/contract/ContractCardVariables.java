package io.agentscope.feishu.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.agentscope.feishu.contract.dto.ContractInfoResponse;

/** 合同模板卡片变量（模板 ID 默认 AAqtmy18CRaGt）。 */
public final class ContractCardVariables {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ContractCardVariables() {}

    public static ObjectNode toTemplateVariableNode(ContractInfoResponse r) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("contractCode", nz(r.contractCode()));
        n.put("contractName", nz(r.contractName()));
        n.put("partyA", nz(r.partyA()));
        n.put("partyB", nz(r.partyB()));
        n.put("contractContext", nz(r.contractContext()));
        n.put("signDate", nz(r.signDate()));
        return n;
    }

    private static String nz(String s) {
        if (s == null || s.isBlank()) {
            return "—";
        }
        return s.strip();
    }
}
