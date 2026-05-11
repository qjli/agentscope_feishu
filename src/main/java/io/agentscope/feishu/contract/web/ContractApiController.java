package io.agentscope.feishu.contract.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.feishu.contract.dto.ContractInfoResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合同 Mock：查询接口供 Agent 工具调用；表单保存接口供回调或联调（收到即打印日志）。
 */
@RestController
@RequestMapping("/api/contract")
public class ContractApiController {

    private static final Logger log = LoggerFactory.getLogger(ContractApiController.class);
    private static final ObjectMapper OM = new ObjectMapper();

    private static final String DEMO_HT = "HT-20250908192882";

    @GetMapping("/info")
    public ContractInfoResponse info(@RequestParam("contractCode") String contractCode) {
        String code = contractCode == null ? "" : contractCode.strip();
        if (DEMO_HT.equalsIgnoreCase(code)) {
            return new ContractInfoResponse(
                    DEMO_HT,
                    "储能系统设备采购与安装（示范合同）",
                    "陕西中辰海锋新能源有限公司",
                    "某电力工程集团有限公司",
                    "磷酸铁锂储能系统 2.5MW/5MWh 一套，含安装调试与一年质保。",
                    "2025-09-10");
        }
        int seed = code.isEmpty() ? 0 : Math.abs(code.hashCode());
        return new ContractInfoResponse(
                code.isEmpty() ? "—" : code,
                "Mock 合同（未命中演示编号 " + DEMO_HT + "）",
                "甲方（占位-" + (1000 + seed % 9000) + "）",
                "乙方（占位-" + (2000 + seed % 8000) + "）",
                "占位合同标的说明，实际以业务系统为准。",
                "2025-01-01");
    }

    @PostMapping(value = "/form-save", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> formSave(@RequestBody JsonNode body) {
        try {
            log.info("[合同表单保存] 收到 JSON：\n{}", OM.writerWithDefaultPrettyPrinter().writeValueAsString(body));
        } catch (Exception e) {
            log.info("[合同表单保存] 收到 body（序列化失败）: {}", body);
        }
        return Map.of("ok", true);
    }
}
