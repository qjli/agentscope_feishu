package io.agentscope.feishu.contract;

import io.agentscope.feishu.contract.dto.ContractInfoResponse;
import org.springframework.stereotype.Component;

@Component
public class ContractReplyFormatter {

    public String format(ContractInfoResponse r) {
        return "【合同】"
                + r.contractCode()
                + " | "
                + r.contractName()
                + "\n甲："
                + r.partyA()
                + "\n乙："
                + r.partyB()
                + "\n标的："
                + r.contractContext()
                + "\n签订日："
                + r.signDate();
    }
}
