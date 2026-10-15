# -*- coding: utf-8 -*-
# Rosetta remote bridge probe: ping -> console -> exec
import socket, json, sys, io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

TOKEN_FILE = r"H:\MinecraftMods\ThaumicAdditions-Port\run-client\RosettaRemoteDebugBridge\remote-token.txt"
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 48793

token = open(TOKEN_FILE, encoding="utf-8").read().strip()
s = socket.create_connection(("127.0.0.1", PORT), timeout=30)
f = s.makefile("rwb")

def send(cmd, **args):
    msg = {"token": token, "cmd": cmd, "args": args}
    f.write((json.dumps(msg) + "\n").encode("utf-8"))
    f.flush()
    line = f.readline().decode("utf-8", "replace").strip()
    return line

print("ping  ->", send("ping"))
print("console ->", send("console", command="[probe] hello from remote bridge"))
code = ("var mc = net.minecraft.client.Minecraft.getInstance();"
        "var p = mc.player;"
        "return p != null ? (\"player=\" + p.getName().getString() + \" pos=\" + p.blockPosition()) : \"no player\";")
print("exec  ->", send("exec", code=code))
s.close()
