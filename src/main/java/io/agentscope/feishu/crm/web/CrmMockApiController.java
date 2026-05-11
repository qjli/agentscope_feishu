package io.agentscope.feishu.crm.web;

import io.agentscope.feishu.crm.dto.CustomerInfoResponse;
import io.agentscope.feishu.crm.dto.OrderLine;
import io.agentscope.feishu.crm.dto.OrdersSummaryResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟真实后端：对应路径 /api/crm/customerInfo 与 /api/crm/orders（等价于 xx/xx/customerInfo、orders 的落地形态）。
 */
@RestController
@RequestMapping("/api/crm")
public class CrmMockApiController {

    private static final String DEMO_COMPANY = "陕西中辰海锋新能源有限公司";
    private static final String DEMO_COMPANY1 = "北京燃气";

    @GetMapping("/customerInfo")
    public CustomerInfoResponse customerInfo(@RequestParam("companyName") String companyName) {
        String name = companyName == null ? "" : companyName.strip();
        if (DEMO_COMPANY.equals(name)) {
            return new CustomerInfoResponse(
                    DEMO_COMPANY,
                    "91610100MA7E23JKIJK88",
                    "李伟",
                    "029-8888-6018",
                    "陕西省西安市高新区锦业路1号都市之门B座1206室",
                    "电力、热力生产和供应业 / 新能源技术推广",
                    "人民币伍仟万元整",
                    "在业",
                    "Mock: /api/crm/customerInfo",
                    "有限责任公司（新能源技术服务）",
                    "2018-06-22",
                    "陕危化经字[2019]0021号",
                    "陕交危运[2020]第1088号");
        }
        if (DEMO_COMPANY1.equals(name)) {
            return new CustomerInfoResponse(
                    DEMO_COMPANY1,
                    "BEIJING-RANQI-1002",
                    "张帆",
                    "029-232-6018",
                    "北京市朝阳区大方B座1206室",
                    "电力、热力生产和供应业 / 新能源技术推广",
                    "人民币伍仟万元整",
                    "在业",
                    "Mock: /api/crm/customerInfo",
                    "有限责任公司（新能源技术服务）",
                    "2008-09-02",
                    "京危化经字[2019]0021号",
                    "京交危运[2020]第1088号");
        }
        return new CustomerInfoResponse(
                name,
                "91XXXXXXXXXXXXXXXX",
                "—",
                "—",
                "—",
                "—",
                "—",
                "未知",
                "Mock: /api/crm/customerInfo（未命中演示企业，返回占位数据）",
                "—",
                "—",
                "—",
                "—");
    }

    @GetMapping("/orders")
    public OrdersSummaryResponse orders(
            @RequestParam("companyName") String companyName, @RequestParam("orderStatus") String orderStatus) {
        String name = companyName == null ? "" : companyName.strip();
        String filter = orderStatus == null ? "ALL" : orderStatus.strip().toUpperCase();
        return mockOrders(name, filter);
    }

    private static OrdersSummaryResponse mockOrders(String name, String filter) {
        if (DEMO_COMPANY.equals(name)) {
            List<OrderLine> lines = demoOrderLines();
            String totalAmount = "¥ 38,620,000.00";
            String settledAmount = "¥ 26,940,000.00";
            if ("SETTLED".equals(filter)) {
                List<OrderLine> settledLines = lines.stream()
                        .filter(o -> "SETTLED".equalsIgnoreCase(o.orderStatus()))
                        .toList();
                int settled = 86;
                int invoiced = 74;
                return new OrdersSummaryResponse(
                        name,
                        "SETTLED",
                        settled,
                        settledAmount,
                        settled,
                        settledAmount,
                        settledLines,
                        0,
                        invoiced);
            }
            int total = 128;
            int settled = 86;
            int unsettled = total - settled;
            int invoiced = 73;
            return new OrdersSummaryResponse(
                    name, "ALL", total, totalAmount, settled, settledAmount, lines, unsettled, invoiced);
        }

        int seed = name.isEmpty() ? 1 : Math.abs(name.hashCode());
        int total = 12 + (seed % 88);
        int settled = total * 2 / 3;
        int unsettled = total - settled;
        int invoiced = Math.max(0, settled - 1 - (seed % 7));

        if ("SETTLED".equals(filter)) {
            int t = 8 + (seed % 24);
            int inv = Math.max(0, t - 2 - (seed % 4));
            return new OrdersSummaryResponse(
                    name,
                    "SETTLED",
                    t,
                    "¥ 0.00",
                    t,
                    "¥ 0.00",
                    List.of(new OrderLine(
                            "MOCK-" + (seed % 9000 + 1000),
                            name + "（已结算订单示例）",
                            "¥ 0.00",
                            "SETTLED",
                            "2025-12-01")),
                    0,
                    inv);
        }

        return new OrdersSummaryResponse(
                name,
                "ALL",
                total,
                "¥ 0.00",
                settled,
                "¥ 0.00",
                List.of(new OrderLine(
                        "MOCK-" + (seed % 9000 + 1000),
                        name + "（订单示例）",
                        "¥ 0.00",
                        "PENDING",
                        "")),
                unsettled,
                invoiced);
    }

    private static List<OrderLine> demoOrderLines() {
        List<OrderLine> list = new ArrayList<>();
        list.add(new OrderLine(
                "ORD-2025-1182",
                "储能系统设备采购合同（一期）",
                "¥ 6,800,000.00",
                "SETTLED",
                "2025-11-18"));
        list.add(new OrderLine(
                "ORD-2025-1204",
                "光伏电站运维服务（2025Q4）",
                "¥ 420,000.00",
                "SETTLED",
                "2025-12-02"));
        list.add(new OrderLine(
                "ORD-2026-0007",
                "分布式光伏组件供货",
                "¥ 2,150,000.00",
                "PENDING",
                ""));
        list.add(new OrderLine(
                "ORD-2026-0015",
                "峰谷套利策略软件订阅",
                "¥ 198,000.00",
                "PENDING",
                ""));
        list.add(new OrderLine(
                "ORD-2025-0991",
                "充电桩批量供货（陕北标段）",
                "¥ 9,200,000.00",
                "SETTLED",
                "2025-10-30"));
        return list;
    }
}
