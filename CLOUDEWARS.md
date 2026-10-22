# Cloude Wars

Cloude Wars is a Bedwars plugin for Paper 1.21.11. It is a fork of
[New Bedwars](https://github.com/l299l/NewBedwars-Free) by l299l (GPL-3.0, see `LICENSE.txt`), forked from
**2.4-beta** (upstream commit `29ff496`, 22.10.2026). `README.md` is New Bedwars' own readme and stays as upstream has it.

## What Cloude Wars changes

- **Identity:** `plugin.yml` (name `CloudeWars`, authors, description) and `pom.xml` (artifact `CloudeWars`,
  version `0.1.0-SNAPSHOT`). The Java packages stay `com.l299l.newbedwars`, so upstream updates merge cleanly.
- **Proxy mode** (`me.sirsz.cloudewars.proxy.ProxyMode`, settings in `plugins/CloudeWars/cloudewars.yml`): the server
  runs one arena behind a Velocity proxy. Players who connect go straight into the arena's waiting room, and go back
  to the lobby server when the game ends or they leave (leave item, `/bw lobby`). A player who connects while a game
  is running is sent back with a message. Players with `cloudewars.proxy.admin` (ops by default) are never moved:
  they join with `/bw join` themselves.

Cloude Wars code lives in `me.sirsz.cloudewars`. It is connected to New Bedwars by one-line hooks, each calling
`me.sirsz.cloudewars.CloudeWars`:

| Hook | Where | What it does |
| --- | --- | --- |
| `CloudeWars.reload(this)` | end of `NewBedwars.reloadAll()` | Loads `cloudewars.yml` and registers listeners (also after `/bw reload`, which unregisters all listeners) |
| `CloudeWars.onArenaLeave(player)` | `Arena.leave()`, after the teleport to the lobby location | Proxy mode sends the player to the lobby server |

## Build and deploy

```bat
powershell -File tools\deploy.ps1
```

Runs `mvn package` (Maven from PATH, or the copy bundled with IntelliJ IDEA) and copies the jar to
`servers/bedwars1/plugins/CloudeWars.jar`. Add `-SkipTests` to skip New Bedwars' unit tests. Stop `bedwars1`
first: Windows keeps the loaded jar locked.

## Test network (`servers/`, only the start scripts are in git)

| Server | Address | RCON | What |
| --- | --- | --- | --- |
| `proxy` | **127.0.0.1:25575** | - | Velocity 4.3.0 (needs Java 25), offline mode, modern forwarding. Players connect here. |
| `lobby` | 127.0.0.1:25591 | 25592 | Paper 1.21.11, flat world, adventure mode |
| `bedwars1` | 127.0.0.1:25593 | 25594 | Paper 1.21.11 + Cloude Wars, proxy mode on, arena `mini` |

- Start everything: `servers\start-all.bat` (one window per server). Stop the Paper servers with `stop`, the proxy with `shutdown`.
- Console commands from outside: `python tools\rcon.py [--server lobby] "list"` (default server: `bedwars1`).
- Arena `mini`: a red and a blue island (1 player each, 2 needed to start, 10 s countdown) and a middle island with a
  diamond generator. The arena world's dimension is `minecraft:mini`; console teleports into it need
  `execute in minecraft:mini run tp ...`.
- Velocity's `/server lobby` and `/server bedwars1` switch servers. `/bw` commands only work for players.
- The proxy is in offline mode and only listens on 127.0.0.1. Before letting other PCs or the internet in, turn on
  `online-mode` in `velocity.toml` and `proxies.velocity.online-mode` in each server's `config/paper-global.yml`.

## In-game tests (MCP Control)

One client: the MCP Control bridge (`../MCPControl`) starts an offline 1.21.11 client: `minecraft_launch` with
`version: "fabric-loader-0.18.4-1.21.11"`, then `connect_to_server` with `address: "localhost:25575"`.

Several clients at once: `tools/mc_clients.py` starts each through its own bridge and drives it over MCP Control's
HTTP API (run it from Bash; PowerShell mangles JSON arguments with spaces):

```bash
python tools/mc_clients.py launch Tester1 25582
python tools/mc_clients.py call 25582 connect_to_server '{"address":"localhost:25575"}'
python tools/mc_clients.py call 25582 run_command '{"command":"server bedwars1"}'
python tools/mc_clients.py stop 25582
```

`Claude` is an op on `bedwars1` (admin, not moved by proxy mode). If a command answers "Chat disabled in client
options", the player is dead: `respawn` first.

## Pulling New Bedwars updates

`upstream` is New Bedwars' repository and `main` is our branch.

```bat
git fetch upstream
git merge upstream/master
```

To keep merges easy: put Cloude Wars code in `me.sirsz.cloudewars`, and only add one-line hooks to New Bedwars'
files. Expect small conflicts in `pom.xml` and `plugin.yml` (version and name lines).

## Other folders

- `archive/addon-base/`: the earlier addon version of Cloude Wars (Gradle), kept for reference. Not in git.
