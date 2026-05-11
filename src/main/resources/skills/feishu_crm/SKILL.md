---
name: feishu_crm
description: 当用户要查询企业客户基本信息、订单量/已结算订单量统计，或话术与「查询…客户基本信息 / 查询…订单量」等 CRM 场景时使用本技能。
metadata:
  domain: crm
  channel: feishu
---

# 飞书 CRM 查询（Mock 接口 + 模板卡片）

## 能力概述

通过本应用已暴露的 REST（`/api/crm/customerInfo`、`/api/crm/orders`）拉取数据，并向**当前飞书会话**发送模板卡片或纯文本。工具仅在加载本技能后可用（渐进式披露）。

## 何时加载

仅当用户话术中能明确 **公司名称 + 客户信息或订单统计** 需求时再加载；若读不懂、缺公司名、或超出本技能（仅上述两类查询 + 发卡片/纯文本），**不要**加载本技能。

## 回复约束（必须遵守）

1. **无法理解或超出能力**：用**一句极短**中文说明即可（例如「无法理解您的需求。」或「本助手仅支持客户信息与订单统计查询。」）。**禁止**展开建议、替代方案、操作步骤或其它「热心协助」。
2. **调用 `crm_send_*` 且已成功发到会话后**：**禁止**再向用户总结卡片内容、复述字段、解读数据或延伸建议；最终对用户**不要再写长文**，宁可**不再发文字**（卡片已承载信息）。若必须收尾，**至多**二字「已发。」，不得扩展。

## 使用步骤

1. 调用 `load_skill_through_path` 加载 `feishu_crm`（必要时再按需读取 `references/crm-patterns.md`）。
2. 根据意图选择工具（二选一或依次）：
   - **客户信息**：`crm_send_customer_info_card(companyName)` — 向当前会话发客户模板卡片（若配置关闭卡片则发纯文本）。
   - **订单统计**：`crm_send_orders_summary_card(companyName, orderStatus)` — `orderStatus` 取 `ALL`（全部）或 `SETTLED`（仅已结算）。
3. 工具返回中会说明「勿再总结」；你对用户的收尾遵守上文「回复约束」。勿编造未返回的字段。

## 话术参考

更完整的句式与参数说明见 `references/crm-patterns.md`。

## 可用资源

- `references/crm-patterns.md`：固定中文句式与 `orderStatus` 取值说明
