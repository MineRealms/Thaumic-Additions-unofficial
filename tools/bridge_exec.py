# -*- coding: utf-8 -*-
"""Run a Java snippet inside a live Rosetta dev client.

Usage: python tools/bridge_exec.py <port> <java-snippet-file-or-"-">
       echo 'return "hi";' | python tools/bridge_exec.py 48793 -

The snippet is compiled in-memory by the bridge and executed on the server thread.
It may use `return` to hand a value back (String.valueOf'd into the reply).
"""
import io
import json
import os
import socket
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

port = int(sys.argv[1]) if len(sys.argv) > 1 else 48793
source = sys.argv[2] if len(sys.argv) > 2 else "-"

# The auth token lives per dev instance; map the known bridge ports to their game dirs.
_game = "run-client-1.21.1" if port == 48794 else "run-client"
TOKEN_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), _game,
                          "RosettaRemoteDebugBridge", "remote-token.txt")

if source == "-":
    code = sys.stdin.read()
else:
    code = open(source, encoding="utf-8").read()

token = open(TOKEN_FILE, encoding="utf-8").read().strip()
sock = socket.create_connection(("127.0.0.1", port), timeout=60)
stream = sock.makefile("rwb")
stream.write((json.dumps({"token": token, "cmd": "exec", "args": {"code": code}}) + "\n").encode("utf-8"))
stream.flush()
reply = json.loads(stream.readline().decode("utf-8", "replace"))
sock.close()

if reply.get("ok"):
    result = reply.get("result", {})
    print(result.get("result", ""))
    print("[exec %s]" % result.get("class", "?"))
else:
    print("ERROR:", reply.get("error"), reply.get("exception", ""), file=sys.stderr)
    sys.exit(1)
