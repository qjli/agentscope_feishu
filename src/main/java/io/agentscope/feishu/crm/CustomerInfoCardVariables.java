package io.agentscope.feishu.crm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.agentscope.feishu.crm.dto.CustomerInfoResponse;

/**
 * 将 /api/crm/customerInfo 返回映射为飞书卡片搭建工具中的变量名（与模板 AAqtrZyWSproW 绑定字段一致）。
 */
public final class CustomerInfoCardVariables {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CustomerInfoCardVariables() {}

    /**
     * @see <a href="https://open.feishu.cn/document/feishu-cards/quick-start/send-feishu-cards-with-app-bots">发送模板卡片</a>
     */
    public static ObjectNode toTemplateVariableNode(CustomerInfoResponse r) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("baseCompanyName", nz(r.companyName()));
        n.put("baseCompanyBizCode", nz(r.creditCode()));
        n.put("baseCompanyType", nz(firstNonBlank(r.companyType(), r.industry())));
        n.put("baseCompanyCreateDate", nz(r.establishedDate()));
        n.put("baseContractor", nz(r.legalRepresentative()));
        n.put("baseTel", nz(r.contactPhone()));
        n.put("baseAddr", nz(r.registeredAddress()));
        n.put("baseDangerBussPermitCode", nz(r.dangerousBusinessPermitCode()));
        n.put("baseDangerTransportLicenseCode", nz(r.dangerousTransportLicenseCode()));
        return n;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.strip();
        }
        if (b != null && !b.isBlank()) {
            return b.strip();
        }
        return "—";
    }

    private static String nz(String s) {
        if (s == null || s.isBlank()) {
            return "—";
        }
        return s.strip();
    }
}
