"""Starts and drives several MCP Control game clients at once, to test the network with more than one player.

Each client is launched through its own MCP Control bridge (own MCP port, own data folder under
%LOCALAPPDATA%\\MCPControl\\clients\\<name>) as an offline player with SKLauncher's Fabric 1.21.11. The bridge
exits after the launch; the game keeps running and is driven through MCP Control's HTTP API on its port.
Keep 25580 free for the normal bridge: use 25581 and up.

Usage:
  python tools/mc_clients.py launch <name> <port> [version]   start a client as offline player <name>
                                                              (version: an SKLauncher version id, default 1.21.11)
  python tools/mc_clients.py call <port> <tool> [json-args]   call one MCP Control tool on that client
  python tools/mc_clients.py stop <port>                      close that client
"""
import base64
import json
import os
import pathlib
import subprocess
import sys
import time
import urllib.error
import urllib.request

BRIDGE = pathlib.Path(__file__).resolve().parents[2] / "MCPControl" / "bridge" / "McpBridge.java"
VERSION = "fabric-loader-0.18.4-1.21.11"
CLIENTS = pathlib.Path(os.environ["LOCALAPPDATA"]) / "MCPControl" / "clients"


def launch(name, port, version=VERSION):
    data_dir = CLIENTS / name
    data_dir.mkdir(parents=True, exist_ok=True)
    bridge = subprocess.Popen(["java", str(BRIDGE), "--port", str(port), "--data-dir", str(data_dir)],
                              cwd=data_dir, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                              stderr=subprocess.DEVNULL, text=True, encoding="utf-8")

    def rpc(request_id, method, params):
        bridge.stdin.write(json.dumps({"jsonrpc": "2.0", "id": request_id, "method": method, "params": params}) + "\n")
        bridge.stdin.flush()
        while True:
            line = bridge.stdout.readline()
            if not line:
                raise RuntimeError("the bridge exited")
            reply = json.loads(line)
            if reply.get("id") == request_id:
                return reply

    rpc(1, "initialize", {"protocolVersion": "2025-06-18", "capabilities": {},
                          "clientInfo": {"name": "mc_clients", "version": "1"}})
    bridge.stdin.write(json.dumps({"jsonrpc": "2.0", "method": "notifications/initialized"}) + "\n")
    reply = rpc(2, "tools/call", {"name": "minecraft_launch", "arguments": {
        "version": version, "username": name, "max_memory_mb": 2048, "wait_seconds": 300}})
    print_result(reply.get("result", reply.get("error")), port)
    bridge.stdin.close()  # the bridge exits, the game keeps running
    bridge.wait(timeout=30)


def call(port, tool, args):
    request = urllib.request.Request(f"http://127.0.0.1:{port}/api/call/{tool}", data=json.dumps(args).encode(),
                                     headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=600) as response:
            result = json.loads(response.read().decode("utf-8"))
    except urllib.error.URLError as e:
        sys.exit(f"No game on port {port}: {e}")
    print_result(result, port)
    return result


def print_result(result, port):
    if not isinstance(result, dict) or "content" not in result:
        print(json.dumps(result, indent=2))
        return
    if result.get("isError"):
        print("[ERROR]")
    for item in result["content"]:
        if item.get("type") == "image":
            path = CLIENTS / "screenshots" / f"{port}-{int(time.time() * 1000)}.png"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(base64.b64decode(item["data"]))
            print(f"[screenshot saved to {path}]")
        else:
            print(item.get("text", ""))


def main(argv):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if len(argv) >= 3 and argv[0] == "launch":
        launch(argv[1], int(argv[2]), *argv[3:4])
    elif len(argv) >= 3 and argv[0] == "call":
        call(int(argv[1]), argv[2], json.loads(argv[3]) if len(argv) > 3 else {})
    elif len(argv) == 2 and argv[0] == "stop":
        call(int(argv[1]), "quit_game", {})
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
