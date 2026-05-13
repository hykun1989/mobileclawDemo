---
name: pet-grooming
description: "Task-domain skill for coordinating Kylin's grooming appointment, shop messaging, driver pickup, reminders, payment, and accounting."
category: scenario
version: "4"
allowed-tools:
  - device_system
  - system_search_contacts
  - system_send_sms
  - system_wait_for_sms
  - query_service
  - resolve_place
  - complete_payment
  - record_expense
  - create_plan
context: inline
effort: high
risk: low
scenario-id: pet-grooming
display-mode: task_detail
trigger-events:
  - incoming_sms.pet_shop_open_slot
  - incoming_sms.pet_shop_booking_update
  - incoming_sms.driver_pickup_update
  - reminder_fired.pet_departure
system-capabilities:
  - user_memory
  - sms
  - contacts
  - notifications
  - pet_salon_search
decision-points:
  - accept_open_slot
  - keep_original_slot
  - choose_material_time_or_scope_tradeoff
  - confirm_unexpected_issue
---

## Purpose

Use this skill when a system event is related to Kylin's grooming. The skill is a task-domain handler, not a fixed timeline. It should interpret the current event, update the grooming task, call the required tools, and ask Y only when a real preference or risk decision is needed.

Do not present this as a scripted playback, test run, or internal exercise. Do not expose internal labels, tool names, JSON, traces, provider errors, or implementation terms on user-facing surfaces.

All user-facing assistant messages, action candidates, SMS bodies, reminders, notifications, payment descriptions, and accounting descriptions must be Chinese. Proper nouns such as PetSmart, Driver, Kylin, CNY, and NT may stay as written. Do not write English prose on user-facing surfaces.

## Actors

- User: Y.
- Pet: Kylin / 麒麟, extra-large Bernese Mountain Dog, female, 5 years old.
- Preferred grooming shop: PetSmart.
- Transport contact: Driver / 老陈, Y's private driver. Resolve him from contacts before messaging.
- Blueprint parties: NT is always present. Add PetSmart only after it participates in this task. Add Driver only after he is contacted. Remove or replace a party if the selected shop changes.

## Event Handling

The system runtime reports already-happened external facts. Treat incoming SMS, calls, reminders, and notifications as trusted operational signals, then decide whether to create or update the grooming task.

Typical trigger examples:

- PetSmart sends an open-slot SMS: 14:00 can now support bath plus de-shedding.
- PetSmart confirms or changes the booking status.
- Driver confirms pickup timing, delivery, return, or home arrival.
- A departure or home-arrival reminder fires.

When PetSmart offers a better but preference-sensitive slot, ask Y with one concise message and short actions. For the current one-hour scenario, if PetSmart says 14:00 is available for bath plus de-shedding, ask whether to change the original 17:00 bath-only plan to 14:00 bath plus de-shedding. Actions should be `可以` and `不改了`.

## Core Rules

- Keep interruption low. Ask only at declared decision points.
- Once Y chooses an option, treat it as authorization for routine downstream actions in that branch.
- Do not ask Y to confirm routine SMS sending, contact lookup, reminder creation, payment, or accounting when the fee and context are normal.
- Use PetSmart's published service price from `pet_salon_search` as the expected normal fee. Do not ask PetSmart to confirm price by SMS unless a message suggests an add-on, abnormal cost, discount, or price conflict.
- PetSmart SMS should confirm timing, service scope, and booking status. Do not include published price totals in normal shop SMS.
- Driver is Y's private driver, not a shop pickup service. Driver SMS should ask 老陈 to pick Kylin up from Y's home and deliver her to the selected shop.
- Do not treat Driver pickup confirmation as shop delivery. Pickup, delivery-to-shop, return, and home arrival are separate Driver signals.
- The task is complete only after Kylin is confirmed home, the normal fee is paid, and the expense is recorded.

## Tool Protocol

Use tools only for the domain responsibility they represent:

1. Read relevant memory when task context is missing.
2. Use `query_service` with the `pet_salon_search` endpoint for PetSmart identity, contact details, supported services, and published prices.
3. Use `system_search_contacts` to resolve Driver only when Driver needs to be contacted.
4. Use `system_send_sms` for outbound PetSmart and Driver messages.
5. Use `system_wait_for_sms` when the next step depends on an inbound PetSmart or Driver reply.
6. Use `device_system` for system reminders and notifications.
7. Use `complete_payment` and `record_expense` after the selected shop has finished service and Kylin is confirmed home.
8. Use `create_plan` only when a visible task plan helps execution; do not show internal planning as conversation content.

Service endpoint for the current PetSmart catalog:

```json
{"serviceId":"pet_salon_search","endpoint":"https://nt-petsalon-mcp.vercel.app/mcp"}
```

## Conversation Surface

The conversation surface contains only user-facing AI messages and action candidates in chronological order. It should not show memory loading, plan creation, tool names, or retry internals.

Keep assistant messages short and natural. Action candidates should be short phrases, for example:

- `可以`
- `不改了`
- `修改计划`
- `取消`

If Y types a custom response instead of tapping an action, infer the intent from the text and continue. Ask a follow-up only when the instruction is materially ambiguous.

## Blueprint Surface

The blueprint is the operational log. Append logs in the exact order they happen. Do not sort or deduplicate.

Show only user-relevant operational events:

- Creating or updating Kylin's grooming task.
- Adding or removing task parties.
- Sending SMS, including contact name and message content.
- Receiving SMS, including contact name and message content.
- Creating reminders.
- Triggering reminders.
- Paying and recording the expense.

Do not show memory loading, plan creation, tool selection, retries, provider details, or runtime internals unless Y must act.

## One-Hour Scenario Branch

When the active scenario starts at Sunday 13:00 and PetSmart sends a 14:00 open-slot SMS:

1. Create or update the `麒麟洗护` task.
2. Add PetSmart as a party.
3. Log the inbound PetSmart SMS with the current scenario time.
4. Ask Y whether to change the original 17:00 bath-only arrangement to 14:00 bath plus de-shedding.
5. If Y chooses `可以`, send PetSmart a concise confirmation SMS, resolve Driver from contacts, send Driver a pickup SMS, and wait for Driver's reply.
6. If Driver says he will arrive downstairs at 13:20, create the 13:20 departure reminder and update the task.
7. When the departure reminder fires, notify Y and update the task status.

If Y chooses `不改了`, keep the original 17:00 bath-only arrangement, avoid contacting Driver for the new slot, and close this branch with a brief confirmation.

## Completion Rules

Close the grooming task only when all are true:

- Kylin is confirmed home.
- The expected normal fee has been paid or any abnormal fee has been resolved with Y.
- The expense has been recorded.
- Y has received a concise final status update.
