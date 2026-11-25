"""In-game test of every /bw setBuildSymmetry kind + /bw syncSymmetry, one fresh test arena each (delete them after).
Drives the client "Claude" (an op on the server) over MCP Control's HTTP API (see tools/mc_clients.py).

Usage: python tools/symmetry_test.py <client port> <server address> <rcon script> <plugin data dir> [flags] [modes...]
  proxy:     the address is the Velocity proxy (go to server bedwars1 first)
  noconnect: the client is already on the server
  old:       Minecraft before 26.3 (no wool/concrete slabs and stairs: other team blocks are used)
  modes:     quarter, mirror, mirrorx, central (default: all)
Example: python tools/symmetry_test.py 25581 localhost:25575 tools/rcon.py servers/bedwars1/plugins/CloudeWars noconnect old
"""
import json
import pathlib
import subprocess
import sys
import urllib.request

sys.stdout.reconfigure(encoding="utf-8", errors="replace")
PORT, ADDRESS, RCON_SCRIPT, DATA = int(sys.argv[1]), sys.argv[2], sys.argv[3], pathlib.Path(sys.argv[4])
FLAGS = sys.argv[5:]
MODES = [m for m in FLAGS if m in ("quarter", "mirror", "mirrorx", "central")] or ["quarter", "mirror", "mirrorx", "central"]
SIZE = 16
ARENAS = {"quarter": "symquad", "mirror": "symmirror", "mirrorx": "symmirrorx", "central": "symcentral"}
COMMANDS = {"quarter": f"quarter nomiddle {SIZE}", "mirror": f"half nomiddle mirror {SIZE}",
            "mirrorx": f"half nomiddle mirror {SIZE}", "central": f"half nomiddle central {SIZE}"}
# Where the builder stands: in the south-east quarter / south half (far from z = 0) / east half (far from x = 0)
STAND = {"quarter": (1.5, 64, 1.5), "mirror": (1.5, 64, 6.5), "mirrorx": (12.5, 64, 1.5), "central": (1.5, 64, 6.5)}
TEAM_ITEMS = ([("white_wool", "#1#a#"), ("white_concrete", "#1#b#"), ("white_terracotta", "#2#c#")] if "old" in FLAGS
              else [("white_wool_slab", "#1#a#"), ("white_concrete_stairs", "#1#b#"), ("white_wool", "#2#c#")])
TEAM_POSITIONS = [(2, 64, 4), (4, 64, 4), (6, 64, 3)]
TURN = {"north": "east", "east": "south", "south": "west", "west": "north"}
failures = []


def mc(tool, **args):
    request = urllib.request.Request(f"http://127.0.0.1:{PORT}/api/call/{tool}", data=json.dumps(args).encode(),
                                     headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(request, timeout=600) as response:
        result = json.loads(response.read().decode("utf-8"))
    return "\n".join(c.get("text", "") for c in result.get("content", []))


def rcon(*commands):
    return subprocess.run([sys.executable, "-I", RCON_SCRIPT, *commands], capture_output=True, text=True,
                          encoding="utf-8").stdout


def chat(count):
    return [m.get("text", "") for m in json.loads(mc("get_chat", count=count)).get("messages", [])]


def block(x, y, z):
    return json.loads(mc("get_block", x=x, y=y, z=z)).get("block", "")


def turn_rule(steps):
    def move(x, z):
        for _ in range(steps):
            x, z = -z - 1, x
        return x, z

    def facing(f):
        for _ in range(steps):
            f = TURN[f]
        return f
    return move, facing, lambda r: (r + 4 * steps) % 16, False


MIRROR_Z = (lambda x, z: (x, -z - 1), lambda f: {"north": "south", "south": "north"}.get(f, f),
            lambda r: (8 - (r - 16 if r > 8 else r) + 16) % 16, True)
MIRROR_X = (lambda x, z: (-x - 1, z), lambda f: {"east": "west", "west": "east"}.get(f, f),
            lambda r: (16 - (r - 16 if r > 8 else r)) % 16, True)
RULES = {"quarter": [turn_rule(1), turn_rule(2), turn_rule(3)], "mirror": [MIRROR_Z], "mirrorx": [MIRROR_X],
         "central": [turn_rule(2)]}


def check(name, pos, expect):
    state = block(*pos)
    ok = all(part in state for part in expect)
    print(f"    {'ok  ' if ok else 'FAIL'} {name} at {pos}: {state}")
    if not ok:
        failures.append(f"{MODE}: {name}")


def leave_setup():
    mc("run_command", command="bw leave")
    mc("wait", seconds=1)
    mc("click_slot", slot=0)
    mc("wait", seconds=1)
    rcon("gamemode creative Claude", "execute in minecraft:overworld run tp Claude 0 100 0")
    mc("wait", seconds=2)


if "noconnect" not in FLAGS:
    mc("connect_to_server", address=ADDRESS)
    mc("wait", seconds=4)
    if "proxy" in FLAGS:
        mc("run_command", command="server bedwars1")
        mc("wait", seconds=4)
rcon("gamemode creative Claude", "execute in minecraft:overworld run tp Claude 0 100 0")

for MODE in MODES:
    arena = ARENAS[MODE]
    dim = f"minecraft:{arena}"
    print(f"== {MODE}: arena {arena}, /bw setBuildSymmetry {COMMANDS[MODE]}")
    mc("run_command", command=f"bw arena create {arena}")
    mc("wait", seconds=4)
    mc("click_slot", slot=11)
    mc("wait", seconds=1)
    rcon(f"execute in {dim} run fill 0 63 0 13 63 7 minecraft:stone",
         f"execute in {dim} run setblock 3 64 2 minecraft:oak_stairs[facing=north]",
         f'execute in {dim} run setblock 5 64 5 minecraft:chest[facing=west]{{Items:[{{Slot:0b,id:"minecraft:diamond",count:3}}]}}',
         f"execute in {dim} run setblock 8 64 8 minecraft:red_bed[facing=east,part=foot]",
         f"execute in {dim} run setblock 9 64 8 minecraft:red_bed[facing=east,part=head]",
         f"execute in {dim} run setblock 10 65 3 minecraft:oak_log[axis=x]",
         f"execute in {dim} run setblock 2 90 2 minecraft:glowstone",
         f'execute in {dim} run setblock 6 64 1 minecraft:oak_sign[rotation=2]{{front_text:{{messages:["hello","","",""]}}}}',
         f"execute in {dim} run setblock 12 64 5 minecraft:oak_door[facing=north,half=lower,hinge=left]",
         f"execute in {dim} run setblock 12 65 5 minecraft:oak_door[facing=north,half=upper,hinge=left]",
         f"execute in {dim} run setblock 20 64 20 minecraft:gold_block",
         f"execute in {dim} run tp Claude 1.5 64 1.5 0 30")
    mc("wait", seconds=1)
    for (item, name), pos in zip(TEAM_ITEMS, TEAM_POSITIONS):
        mc("give_item", item=f'minecraft:{item}[minecraft:custom_name="{name}"]', slot=0)
        mc("select_hotbar_slot", slot=0)
        mc("place_block", x=pos[0], y=pos[1], z=pos[2], item=f"minecraft:{item}")
        mc("wait", seconds=0.3)
    sx, sy, sz = STAND[MODE]
    rcon(f"execute in {dim} run tp Claude {sx} {sy} {sz}")
    mc("wait", seconds=1)
    mc("run_command", command=f"bw setBuildSymmetry {COMMANDS[MODE]}")
    mc("wait", seconds=1)
    print("    chat:", chat(1)[-1])
    rules = RULES[MODE]
    for move, *_ in rules:
        tx, tz = move(1, 12)
        check("floor in the rest of the map", (tx, 64, tz), ["minecraft:terracotta"])
    check("no floor in your part", (1, 64, 12), ["minecraft:air"])
    mc("run_command", command="bw syncSymmetry")
    done = []
    for _ in range(30):
        mc("wait", seconds=1)
        done = [line for line in chat(3) if line.startswith("Copied")]
        if done:
            break
    print("    chat:", done)
    for number, (move, facing, rotation, flips) in enumerate(rules, 1):
        at = lambda x, y, z: (lambda p: (p[0], y, p[1]))(move(x, z))
        print(f"  -- copy {number}")
        check("stairs", at(3, 64, 2), [f"oak_stairs[facing={facing('north')}"])
        check("chest", at(5, 64, 5), [f"chest[facing={facing('west')}"])
        check("bed foot", at(8, 64, 8), ["red_bed", f"facing={facing('east')}", "part=foot"])
        check("bed head", at(9, 64, 8), ["red_bed", f"facing={facing('east')}", "part=head"])
        turns_axis = MODE == "quarter" and number % 2 == 1
        check("log", at(10, 65, 3), ["oak_log", "axis=z" if turns_axis else "axis=x"])
        check("glowstone up high", at(2, 90, 2), ["glowstone"])
        check("sign", at(6, 64, 1), [f"oak_sign[rotation={rotation(2)}"])
        hinge = "right" if flips else "left"
        check("door bottom", at(12, 64, 5), ["oak_door", f"facing={facing('north')}", f"hinge={hinge}", "half=lower"])
        check("door top", at(12, 65, 5), ["oak_door", f"facing={facing('north')}", f"hinge={hinge}", "half=upper"])
        check("team block", at(*TEAM_POSITIONS[0]), [TEAM_ITEMS[0][0]])
        check("floor stone", at(0, 63, 0), ["minecraft:stone"])
        check("floor replaced where you built nothing", at(5, 64, 4), ["minecraft:air"])
        check("outside the map not copied", at(20, 64, 20), ["minecraft:air"])
        chest = rcon(f"execute in {dim} run data get block {at(5, 64, 5)[0]} 64 {at(5, 64, 5)[2]} Items")
        sign = rcon(f"execute in {dim} run data get block {at(6, 64, 1)[0]} 64 {at(6, 64, 1)[2]} front_text")
        for name, ok in (("chest keeps its diamonds", "diamond" in chest), ("sign keeps its text", "hello" in sign)):
            print(f"    {'ok  ' if ok else 'FAIL'} {name}")
            if not ok:
                failures.append(f"{MODE}: {name}")
    marks_file = DATA / "teamblocks" / f"{arena}.yml"
    marks = marks_file.read_text(encoding="utf-8") if marks_file.exists() else ""
    want = {}
    for number, (move, *_) in enumerate(rules, 1):
        for (x, y, z), team in zip(TEAM_POSITIONS, (1, 1, 2)):
            tx, tz = move(x, z)
            want[f"{tx},{y},{tz}"] = team + 2 * number
    got = {line.split(":")[0].strip(): int(line.split(":")[1]) for line in marks.splitlines() if line.startswith("  ")}
    ok = all(got.get(key) == team for key, team in want.items()) and len(got) == 3 + len(want)
    print(f"    {'ok  ' if ok else 'FAIL'} team block numbers: {sorted(set(want.values()))} ({len(got)} marks)")
    if not ok:
        failures.append(f"{MODE}: team block numbers {got}")
    leave_setup()

print("FAILURES:", failures or "none")
