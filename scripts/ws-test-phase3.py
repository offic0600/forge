#!/usr/bin/env python3
"""Phase 3 WebSocket acceptance test — captures events from AI chat."""

import json
import sys
import time
import threading
import websocket

SESSION_ID = sys.argv[1] if len(sys.argv) > 1 else "test-session"
WORKSPACE_ID = sys.argv[2] if len(sys.argv) > 2 else "test-workspace"
MESSAGE = sys.argv[3] if len(sys.argv) > 3 else "@开发 写一个简单的 Kotlin data class User"

WS_URL = f"ws://localhost:9000/ws/chat?sessionId={SESSION_ID}&workspaceId={WORKSPACE_ID}"

events = []
done = threading.Event()
start_time = time.time()

def on_message(ws, message):
    elapsed = time.time() - start_time
    try:
        data = json.loads(message)
        event_type = data.get("type", "unknown")
        events.append(data)

        # Print key events
        if event_type == "sub_step":
            print(f"  [{elapsed:.1f}s] SUB_STEP: {data.get('message', '')}")
        elif event_type == "ooda_phase":
            detail = data.get("detail", "")
            turn = data.get("turn", "")
            max_turns = data.get("maxTurns", "")
            turn_info = f" Turn {turn}/{max_turns}" if turn else ""
            print(f"  [{elapsed:.1f}s] OODA: {data.get('phase', '')}{turn_info} {detail}")
        elif event_type == "baseline_check":
            print(f"  [{elapsed:.1f}s] BASELINE: status={data.get('status', '')} summary={data.get('summary', '')}")
        elif event_type == "tool_use_start":
            print(f"  [{elapsed:.1f}s] TOOL_START: {data.get('toolName', '')}")
        elif event_type == "hitl_checkpoint":
            print(f"  [{elapsed:.1f}s] HITL: status={data.get('status', '')} profile={data.get('profile', '')} checkpoint={data.get('checkpoint', '')}")
        elif event_type == "content":
            content = data.get("content", "")[:80]
            print(f"  [{elapsed:.1f}s] CONTENT: {content}")
        elif event_type == "complete":
            print(f"  [{elapsed:.1f}s] COMPLETE")
            done.set()
        elif event_type == "error":
            print(f"  [{elapsed:.1f}s] ERROR: {data.get('error', '')}")
            done.set()
        elif event_type == "profile_badge":
            print(f"  [{elapsed:.1f}s] PROFILE: {data.get('profile', '')} conf={data.get('confidence', '')}")
        elif event_type == "file_changed":
            print(f"  [{elapsed:.1f}s] FILE: {data.get('path', '')}")
        else:
            print(f"  [{elapsed:.1f}s] {event_type}: {str(data)[:100]}")
    except json.JSONDecodeError:
        print(f"  [{elapsed:.1f}s] RAW: {message[:100]}")

def on_error(ws, error):
    print(f"  WS ERROR: {error}")
    done.set()

def on_close(ws, close_status_code, close_msg):
    print(f"  WS CLOSED: {close_status_code} {close_msg}")
    done.set()

def on_open(ws):
    print(f"Connected to WebSocket")
    print(f"Sending: {MESSAGE}")
    msg = json.dumps({
        "type": "message",
        "content": MESSAGE,
        "contexts": []
    })
    ws.send(msg)

print(f"=== Phase 3 WebSocket Test ===")
print(f"Session: {SESSION_ID}")
print(f"Workspace: {WORKSPACE_ID}")
print(f"Message: {MESSAGE}")
print()

ws = websocket.WebSocketApp(
    WS_URL,
    on_open=on_open,
    on_message=on_message,
    on_error=on_error,
    on_close=on_close
)

# Run with timeout
wst = threading.Thread(target=ws.run_forever)
wst.daemon = True
wst.start()

# Wait up to 120 seconds
done.wait(timeout=120)
time.sleep(2)  # Wait for final events
ws.close()

print()
print("=== Event Summary ===")
type_counts = {}
for e in events:
    t = e.get("type", "unknown")
    type_counts[t] = type_counts.get(t, 0) + 1

for t, c in sorted(type_counts.items()):
    print(f"  {t}: {c}")

print(f"\nTotal events: {len(events)}")

# Acceptance criteria checks
print("\n=== Acceptance Checks ===")
sub_steps = [e for e in events if e.get("type") == "sub_step"]
ooda_events = [e for e in events if e.get("type") == "ooda_phase"]
baseline_events = [e for e in events if e.get("type") == "baseline_check"]
hitl_events = [e for e in events if e.get("type") == "hitl_checkpoint"]
tool_events = [e for e in events if e.get("type") == "tool_use_start"]
content_events = [e for e in events if e.get("type") == "content"]
complete_events = [e for e in events if e.get("type") == "complete"]

# TC-1.1: sub_step events >= 5
check = len(sub_steps) >= 5
print(f"TC-1.1 sub_step >= 5: {'PASS' if check else 'FAIL'} (got {len(sub_steps)})")
for s in sub_steps[:3]:
    has_msg = "message" in s
    has_ts = "timestamp" in s
    print(f"  message字段: {has_msg}, timestamp字段: {has_ts}")

# TC-1.2: OODA with turn info
ooda_with_turn = [e for e in ooda_events if e.get("turn")]
check = len(ooda_with_turn) > 0
print(f"TC-1.2 OODA Turn 计数: {'PASS' if check else 'FAIL'} (got {len(ooda_with_turn)} with turn)")

# TC-1.4: baseline_check events
check = len(baseline_events) > 0
print(f"TC-1.4 Baseline 事件: {'PASS' if check else 'N/A — 可能未触发 baseline'} (got {len(baseline_events)})")

# TC-2.1: HITL checkpoint
hitl_awaiting = [e for e in hitl_events if e.get("status") == "awaiting_approval"]
check = len(hitl_awaiting) > 0
print(f"TC-2.1 HITL 暂停: {'PASS' if check else 'N/A — 简单消息可能不触发'} (got {len(hitl_awaiting)})")

# Content + Complete
print(f"Content 事件: {len(content_events)}")
print(f"Complete 事件: {len(complete_events)}")
print(f"Tool 事件: {len(tool_events)}")
