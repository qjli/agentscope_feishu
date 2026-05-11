---
name: feishu_crm
description: 当用户要查询企业客户基本信息、订单量/已结算订单量、合同编号对应合同信息（如「查询HT-xxx合同」），或相关 CRM/合同话术时使用本技能。
metadata:
  domain: crm
  channel: feishu
---

# 飞书 CRM 查询（Mock 接口 + 模板卡片）

## 能力概述

通过本应用已暴露的 REST（`/api/crm/*`、`/api/contract/info`）拉取数据，并向**当前飞书会话**发送模板卡片或纯文本。工具仅在加载本技能后可用（渐进式披露）。

## 何时加载

当用户话术中能明确 **公司名 + 客户/订单**，或 **合同编号 + 合同查询**（如「查询HT-20250908192882合同」）时再加载。读不懂、缺关键信息、或超出本技能能力时，**不要**加载本技能。

## 合同查询强制规则

用户消息为 **「查询」+ 合同编号 +「合同」** 整句（中间可有空格，如 `查询 HT-20250908192882 合同`）时：

1. **必须**先 `load_skill_through_path` 加载 `feishu_crm`。
2. **必须**调用 `contract_send_info_card(contractCode)`，`contractCode` 取编号部分（如 `HT-20250908192882`）。
3. **禁止**仅用自然语言把合同字段复述给用户而不调用本工具（飞书展示依赖工具发模板卡片）。

## 回复约束（必须遵守）

1. **无法理解或超出能力**：用**一句极短**中文说明即可（例如「无法理解您的需求。」或「本助手仅支持客户信息、订单统计与合同编号查询。」）。**禁止**展开建议、替代方案、操作步骤或其它「热心协助」。
2. **调用 `crm_send_*` 或 `contract_send_info_card` 且已成功发到会话后**：**禁止**再向用户总结卡片内容、复述字段、解读数据或延伸建议；最终对用户**不要再写长文**，宁可**不再发文字**（卡片已承载信息）。若必须收尾，**至多**二字「已发。」，不得扩展。

## 使用步骤

1. 调用 `load_skill_through_path` 加载 `feishu_crm`（必要时再按需读取 `references/crm-patterns.md`）。
2. 根据意图选择工具：
   - **客户信息**：`crm_send_customer_info_card(companyName)` — 客户模板卡片或纯文本。
   - **订单统计**：`crm_send_orders_summary_card(companyName, orderStatus)` — `orderStatus` 为 `ALL` 或 `SETTLED`。
   - **合同信息**：`contract_send_info_card(contractCode)` — 合同模板卡片（变量含 contractCode、contractName、partyA、partyB、contractContext、signDate）或纯文本。
3. 用户在卡片内编辑表单并点「保存」时，由飞书回调服务端并打印表单字段（无需模型处理）；若需 HTTP 联调可 POST `/api/contract/form-save`（JSON 体，服务端打印日志）。
4. 工具返回中会说明「勿再总结」；你对用户的收尾遵守上文「回复约束」。勿编造未返回的字段。

## 话术参考

更完整的句式与参数说明见 `references/crm-patterns.md`。

## 可用资源

- `references/crm-patterns.md`：固定中文句式与 `orderStatus` 取值说明
