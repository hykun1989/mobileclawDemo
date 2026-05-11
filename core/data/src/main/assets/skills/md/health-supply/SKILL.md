---
name: health-supply
description: "Task-domain skill for routine health supply restocking without interrupting the user unless a purchase decision is needed."
category: scenario
version: "1"
allowed-tools:
  - query_service
  - resolve_place
  - device_system
  - create_plan
context: inline
effort: medium
risk: low
scenario-id: health-supply
display-mode: task_detail
trigger-events:
  - notification.health_supply_restock
  - notification.health_supply_bundle
decision-points:
  - confirm_purchase
  - confirm_health_sensitive_item
---

## Purpose

Use this skill when a routine health supply becomes available, such as probiotics or vitamins already present in memory. Keep the task low priority unless Y has explicitly asked to buy now.

## Rules

- Use memory to know routine items and brands.
- Keep restock candidates quietly; do not interrupt Y for low-priority offers.
- Ask before purchase, dosage-sensitive changes, new products, or unusual price changes.
- Do not turn health suggestions into medical advice.

## Blueprint Logs

- 收到补货或配送通知。
- 新建或更新健康补给任务。
- 记录候选品、配送时间和是否需要用户确认。

