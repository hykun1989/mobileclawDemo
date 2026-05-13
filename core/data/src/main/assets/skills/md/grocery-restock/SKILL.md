---
name: grocery-restock
description: "Task-domain skill for quiet household grocery restocking from family messages, store notifications, and memory preferences."
category: scenario
version: "1"
allowed-tools:
  - query_service
  - resolve_place
  - create_plan
  - device_system
context: inline
effort: medium
risk: low
scenario-id: grocery-restock
display-mode: task_detail
trigger-events:
  - call_ended.family_transcript
  - incoming_sms.family_grocery_request
  - notification.grocery_delivery_window
decision-points:
  - confirm_purchase
  - clarify_replacement
  - confirm_unusual_fee
---

## Purpose

Use this skill when family messages, calls, or grocery service notifications suggest a household restock task. The agent should merge duplicate signals into one task, quietly keep candidates, and interrupt Y only when buying or replacing items needs preference confirmation.

## Rules

- Use memory for household preferences and usual stores.
- Use `resolve_place` for phrases like 常用超市, 家附近, 商场, or 家楼下.
- Use `query_service` only for product or delivery-window discovery declared by the active scenario.
- Do not ask Y just to create or update a task.
- Ask before purchase, substitution, or abnormal delivery fee.

## Blueprint Logs

Append logs in natural order:

- 收到家庭采购信息。
- 新建或更新采购任务。
- 加入候选商品、门店、配送窗口。
- 请求用户确认购买或替换。

