package io.agentscope.feishu.crm;

import io.agentscope.feishu.crm.dto.CustomerInfoResponse;
import io.agentscope.feishu.crm.dto.OrderLine;
import io.agentscope.feishu.crm.dto.OrdersSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CrmReplyFormatter {

    public String formatCustomer(CustomerInfoResponse r) {
        String bar = "━━━━━━━━━━━━━━━━━━━━";
        StringBuilder sb = new StringBuilder(512);
        sb.append("【客户基本信息】\n");
        sb.append(bar).append('\n');
        row(sb, "企业名称", r.companyName());
        row(sb, "统一社会信用代码", nz(r.creditCode()));
        row(sb, "法定代表人", nz(r.legalRepresentative()));
        row(sb, "联系电话", nz(r.contactPhone()));
        row(sb, "注册地址", nz(r.registeredAddress()));
        row(sb, "所属行业", nz(r.industry()));
        row(sb, "注册资本", nz(r.registeredCapital()));
        row(sb, "经营状态", nz(r.businessStatus()));
        row(sb, "企业类型", nz(r.companyType()));
        row(sb, "成立日期", nz(r.establishedDate()));
        row(sb, "危化经营许可证", nz(r.dangerousBusinessPermitCode()));
        row(sb, "危化道路运输许可证", nz(r.dangerousTransportLicenseCode()));
        sb.append(bar).append('\n');
        sb.append("数据来源：").append(nz(r.dataSource())).append('\n');
        return sb.toString();
    }

    public String formatOrders(OrdersSummaryResponse r) {
        String bar = "━━━━━━━━━━━━━━━━━━━━";
        StringBuilder sb = new StringBuilder(768);
        sb.append("【订单量统计】\n");
        sb.append(bar).append('\n');
        row(sb, "企业名称", r.companyName());
        row(sb, "筛选条件", labelOrderFilter(r.orderStatusFilter()));
        row(sb, "订单总数", String.valueOf(r.totalCount()));
        row(sb, "订单总金额", nz(r.totalAmount()));
        row(sb, "已结算笔数", String.valueOf(r.settledCount()));
        row(sb, "未结算笔数", String.valueOf(r.unSettledNum()));
        row(sb, "已开票笔数", String.valueOf(r.invoicedNum()));
        row(sb, "已结算金额", nz(r.settledAmount()));
        sb.append(bar).append('\n');
        sb.append("近期订单（示例）\n");
        List<OrderLine> lines = r.recentOrders();
        if (lines == null || lines.isEmpty()) {
            sb.append("  （无）\n");
        } else {
            int n = Math.min(lines.size(), 5);
            for (int i = 0; i < n; i++) {
                OrderLine o = lines.get(i);
                sb.append("  ▸ ")
                        .append(o.orderNo())
                        .append(" ｜ ")
                        .append(o.title())
                        .append("\n    金额 ")
                        .append(o.amount())
                        .append(" ｜ 状态 ")
                        .append(o.orderStatus());
                if (o.settlementDate() != null && !o.settlementDate().isBlank()) {
                    sb.append(" ｜ 结算日 ").append(o.settlementDate());
                }
                sb.append('\n');
            }
        }
        sb.append(bar).append('\n');
        sb.append("数据来源：CRM Mock API /api/crm/orders\n");
        return sb.toString();
    }

    private static String labelOrderFilter(String code) {
        if ("SETTLED".equalsIgnoreCase(code)) {
            return "已结算订单";
        }
        return "全部订单";
    }

    private static void row(StringBuilder sb, String k, String v) {
        sb.append(padRight(k, 10)).append("  ").append(v).append('\n');
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        return s + " ".repeat(width - s.length());
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
