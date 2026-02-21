#!/usr/bin/env python3
"""Send HITL approve response via WebSocket."""

import json
import sys
import time
import threading
import websocket

SESSION_ID = sys.argv[1] if len(sys.argv) > 1 else "test-session"
WORKSPACE_ID = sys.argv[2] if len(sys.argv) > 2 else "test-workspace"
ACTION = sys.argv[3] if len(sys.argv) > 3 else "approve"

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

        if event_type == "hitl_checkpoint":
            status = data.get("status", "")
            print(f"  [{elapsed:.1f}s] HITL: status={status}")
            if status == "awaiting_approval":
                print(f"  -> Sending {ACTION} response...")
                ws.send(json.dumps({
                    "type": "hitl_response",
                    "action": ACTION,
                    "feedback": "Test approve from acceptance test"
                }))
        elif event_type == "content":
            content = data.get("content", "")[:100]
            print(f"  [{elapsed:.1f}s] CONTENT: {content}")
        elif event_type == "complete":
            print(f"  [{elapsed:.1f}s] COMPLETE")
            done.set()
        elif event_type == "sub_step":
            print(f"  [{elapsed:.1f}s] SUB_STEP: {data.get('message', '')}")
        elif event_type == "ooda_phase":
            print(f"  [{elapsed:.1f}s] OODA: {data.get('phase', '')} {data.get('detail', '')}")
        elif event_type == "error":
            print(f"  [{elapsed:.1f}s] ERROR: {data.get('error', '')}")
            done.set()
        elif event_type == "connected":
            print(f"  [{elapsed:.1f}s] CONNECTED")
        else:
            print(f"  [{elapsed:.1f}s] {event_type}")
    except json.JSONDecodeError:
        print(f"  [{elapsed:.1f}s] RAW: {message[:100]}")

def on_error(ws, error):
    print(f"  WS ERROR: {error}")
    done.set()

def on_close(ws, close_status_code, close_msg):
    print(f"  WS CLOSED")
    done.set()

def on_open(ws):
    print(f"Connected — waiting for HITL checkpoint recovery...")

print(f"=== HITL {ACTION.upper()} Test ===")
print(f"Session: {SESSION_ID}")

ws = websocket.WebSocketApp(
    WS_URL,
    on_open=on_open,
    on_message=on_message,
    on_error=on_error,
    on_close=on_close
)

wst = threading.Thread(target=ws.run_forever)
wst.daemon = True
wst.start()

done.wait(timeout=60)
time.sleep(2)
ws.close()

print(f"\n=== Results ===")
hitl_events = [e for e in events if e.get("type") == "hitl_checkpoint"]
print(f"HITL events: {len(hitl_events)}")
for h in hitl_events:
    print(f"  status={h.get('status')}")
complete_events = [e for e in events if e.get("type") == "complete"]
print(f"Complete events: {len(complete_events)}")
