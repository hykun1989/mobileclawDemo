---
name: family-errand
description: "Task-domain skill for turning family calls or messages into tracked household errands."
category: scenario
version: "1"
allowed-tools:
  - device_system
  - create_plan
context: inline
effort: medium
risk: low
scenario-id: family-errand
display-mode: task_detail
trigger-events:
  - call_ended.family_transcript
  - incoming_sms.family_request
system-capabilities:
  - contacts
  - notifications
  - reminders
decision-points:
  - clarify_uncertain_request
  - confirm_purchase_or_payment
  - confirm_schedule_conflict
---

## Purpose

Use this skill when a family call or message contains a household errand that should become a tracked task. The system runtime reports call state and audio facts; the Agent is responsible for transcription, intent extraction, task creation, reminders, and any user decision points.

All user-facing assistant messages, action candidates, reminders, and notifications must be Chinese. Do not expose transcripts unless they are useful to Y.

## Event Handling

When a call ends:

1. Use the call transcript or transcription tool result to identify concrete errands, owners, deadlines, and required confirmations.
2. Create or update a task card at the same level as other active tasks.
3. Add the caller as a task party only after the call or message is tied to this task.
4. Enter the task detail and tell Y what was captured in one concise message.

## Decision Boundaries

Ask Y only when:

- The request is ambiguous.
- A purchase, payment, schedule change, or personal preference needs confirmation.
- The task conflicts with another active task.

Do not ask Y to approve routine task creation, reminder creation, or note extraction.

## Blueprint Surface

Append logs in the order they happen:

- Call started or ended.
- Transcription completed.
- Errand task created or updated.
- Reminder or notification created.
- User decision requested or resolved.

Do not show internal reasoning, provider details, tool names, or retry internals.
