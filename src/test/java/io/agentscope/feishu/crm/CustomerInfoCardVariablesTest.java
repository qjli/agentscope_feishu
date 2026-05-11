package io.agentscope.feishu.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agentscope.feishu.crm.dto.CustomerInfoResponse;
import org.junit.jupiter.api.Test;

class CustomerInfoCardVariablesTest {

    @Test
    void mapsToFeishuTemplateKeys() {
        CustomerInfoResponse r = new CustomerInfoResponse(
                "陕西中辰海锋新能源有限公司",
                "91610100MA7XXXXXXXX",
                "李伟",
                "029-8888",
                "西安市",
                "新能源",
                "5000万",
                "在业",
                "mock",
                "有限责任公司",
                "2018-06-22",
                "危化证A",
                "危运证B");
        var n = CustomerInfoCardVariables.toTemplateVariableNode(r);
        assertEquals("陕西中辰海锋新能源有限公司", n.get("baseCompanyName").asText());
        assertEquals("91610100MA7XXXXXXXX", n.get("baseCompanyBizCode").asText());
        assertEquals("有限责任公司", n.get("baseCompanyType").asText());
        assertEquals("2018-06-22", n.get("baseCompanyCreateDate").asText());
        assertEquals("李伟", n.get("baseContractor").asText());
        assertEquals("029-8888", n.get("baseTel").asText());
        assertEquals("西安市", n.get("baseAddr").asText());
        assertEquals("危化证A", n.get("baseDangerBussPermitCode").asText());
        assertEquals("危运证B", n.get("baseDangerTransportLicenseCode").asText());
    }
}
