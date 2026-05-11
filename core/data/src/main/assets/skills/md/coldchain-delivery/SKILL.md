---
name: coldchain-delivery
description: "Task-domain skill for tracking cold-chain parcel arrival, temporary storage, and pickup reminders."
category: scenario
version: "1"
allowed-tools:
  - system_search_contacts
  - system_send_sms
  - system_wait_for_sms
  - resolve_place
  - device_system
  - create_plan
context: inline
effort: medium
risk: low
scenario-id: coldchain-delivery
display-mode: task_detail
trigger-events:
  - notification.coldchain_arriving
  - notification.coldchain_delivered
  - incoming_sms.property_storage_offer
decision-points:
  - confirm_manual_pickup
  - confirm_delegate_pickup
---

## Purpose

Use this skill when a cold-chain package or similar time-sensitive delivery arrives while Y is busy. The agent should track arrival, temporary storage, and whether a building service or trusted contact can hold the package.

## Rules

- SystemRuntime reports courier and property messages as facts.
- The agent resolves storage places and contacts.
- Do not ask Y if the property service can safely hold the package in a front-desk freezer.
- Ask Y only when a person must physically pick it up or when storage is not safe.

## Blueprint Logs

- 收到配送通知。
- 新建冷链收货任务。
- 更新到达、保管、取件状态。
- 必要时创建提醒。

