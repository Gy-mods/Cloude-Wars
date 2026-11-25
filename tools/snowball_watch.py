"""Counts the snow golems' snowballs and follows a player's position on bedwars1 for a few seconds (one RCON
connection), to see the snowball rate and the knockback.

Usage: python tools/snowball_watch.py <seconds> <player>
"""
import pathlib
import re
import socket
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import rcon  # noqa: E402

props = rcon.read_properties("bedwars1")
seconds = float(sys.argv[1]) if len(sys.argv) > 1 else 3
player = sys.argv[2] if len(sys.argv) > 2 else "Tester2"

with socket.create_connection(("127.0.0.1", int(props["rcon.port"])), timeout=10) as sock:
    sock.sendall(rcon.packet(1, 3, props["rcon.password"]))
    rcon.receive(sock)

    def run(command):
        sock.sendall(rcon.packet(2, 2, command))
        return re.sub("§.", "", rcon.receive(sock)[1])

    seen = set()
    positions = []
    start = time.time()
    while time.time() - start < seconds:
        for line in run("execute in minecraft:mini as @e[type=snowball] run data get entity @s UUID").splitlines():
            if "data:" in line:
                seen.add(line.split("data:")[-1].strip())
        positions.append((round(time.time() - start, 2), run(f"data get entity {player} Pos").split("data:")[-1].strip()))
print(f"different snowballs in {seconds}s: {len(seen)} ({len(seen) / seconds:.1f} per second)")
for t, pos in positions[:: max(1, len(positions) // 8)]:
    print(f"  {t:5.2f}s {player} {pos}")
print(f"  last  {player} {positions[-1][1]}")
