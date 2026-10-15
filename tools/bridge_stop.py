# -*- coding: utf-8 -*-
"""Shutdown a live Rosetta dev client cleanly (saves + closes the window).

Usage: python tools/bridge_stop.py <port> <game-dir>

Never kill the gradle task: several dev instances may be running at once, and
killing by task id / process name risks taking down the wrong one.
"""
import io
import json
import os
import socket
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

port = int(sys.argv[1])
gamedir = sys.argv[2]
token = open(os.path.join(gamedir, "RosettaRemoteDebugBridge", "remote-token.txt"), encoding="utf-8").read().strip()

CODE = """
net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
mc.execute(() -> mc.stop());
return "shutdown scheduled";
"""

sock = socket.create_connection(("127.0.0.1", port), timeout=30)
stream = sock.makefile("rwb")
stream.write((json.dumps({"token": token, "cmd": "exec", "args": {"code": CODE}}) + "\n").encode("utf-8"))
stream.flush()
reply = json.loads(stream.readline().decode("utf-8", "replace"))
sock.close()

if reply.get("ok"):
    print(reply.get("result", {}).get("result", "ok"))
else:
    print("ERROR:", reply.get("error"), reply.get("exception", ""), file=sys.stderr)
    sys.exit(1)
