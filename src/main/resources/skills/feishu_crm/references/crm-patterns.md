# CRM 话术与参数

## 推荐用户话术（模型据此选择工具）


| 用户输入               | 建议工具调用                                        |
| ------------------ | --------------------------------------------- |
| `查询<公司> 客户基本信息`    | `crm_send_customer_info_card(<公司>)`           |
| `查询<公司>`（整句不含「订单」） | 同上                                            |
| `查询<公司>订单量`        | `crm_send_orders_summary_card(<公司>, ALL)`     |
| `查询<公司>已结算的订单量`    | `crm_send_orders_summary_card(<公司>, SETTLED)` |
| `查询<合同编号>合同`（如 `查询HT-20250908192882合同`） | `contract_send_info_card(<合同编号>)` |

## orderStatus

- `ALL`：`/api/crm/orders?orderStatus=ALL`
- `SETTLED`：`/api/crm/orders?orderStatus=SETTLED`

自然语言也可表达相同意图，由模型抽取公司名与订单范围后调用工具。

## 回复风格（与 SKILL.md 一致）

抽不出公司名/合同号、或需求不是客户/订单/合同查询时：**不要**加载本技能；对用户**一句短拒**即可，勿展开协助。成功 `crm_send_*` / `contract_send_info_card` 后：**勿**总结卡片、**勿**扩展；最终文字宁可不发。