# Cloude Wars

Cloude Wars is a Bedwars plugin for Paper 1.21.11. It is a fork of
[New Bedwars](https://github.com/l299l/NewBedwars-Free) by l299l (GPL-3.0, see `LICENSE.txt`), forked from
**2.4-beta** (upstream commit `29ff496`, 22.10.2026). `README.md` is Cloude Wars' own; New Bedwars' readme is in
its repository. The GitHub repository is [Gy-mods/Cloude-Wars](https://github.com/Gy-mods/Cloude-Wars) (`origin`).

## What Cloude Wars changes

- **Identity:** `plugin.yml` (name `CloudeWars`, authors, description) and `pom.xml` (artifact `CloudeWars`,
  version `0.1.0-SNAPSHOT`). The Java packages stay `com.l299l.newbedwars`, so upstream updates merge cleanly.
- **Paper 1.21.11 API and Java 21** instead of New Bedwars' Paper 1.17 / Java 16 (needed for trims). This renamed
  three constants in New Bedwars' code (`Particle.DUST`, `EntityType.FIREWORK_ROCKET`, `EntityType.TNT` in
  `Arena.java` and `ArenaGameplayEvents.java`) and moved SnakeYAML to 2.2, the version Paper ships.
- **Team colors**: only white, black, red, orange, yellow, lime, blue and purple (`team-colors` in `cloudewars.yml`,
  same names as `/bw createTeam`). `/bw createTeam` refuses other colors and its Tab completion lists only these.
  Colors are compared after New Bedwars' own name translation (`GOLD` = orange). Existing arenas still load.
- **Team wood** (`me.sirsz.cloudewars.teams.TeamBlocks`): each team color has a wood type (`team-wood` in
  `cloudewars.yml`: white pale_oak, black dark_oak, red mangrove, orange acacia, yellow bamboo, lime jungle, blue
  warped, purple crimson). Oak is the placeholder: the shop's Wood item (oak planks) shows and gives the buyer's team
  planks. The default shop (`exampleShop.yml`) now lists Wood in the Blocks category.
- **Armor trims** (`me.sirsz.cloudewars.cosmetics`): `/bw trim <helmet|chestplate|leggings|boots> <trim|none>` picks
  a trim pattern per armor piece, for everyone. The trim material comes from the team color (`trims.materials` in
  `cloudewars.yml`, keyed by the color names `/bw createTeam` takes: white, black, red, orange, yellow, lime, blue,
  purple). Every armor New Bedwars gives gets the trims: starting leather, chainmail/iron/diamond upgrades, respawns.
  Changing a trim mid-game updates the worn armor at once. Choices are saved per player in
  `plugins/CloudeWars/trims.yml` (per server; not shared across a network yet).
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
| `CloudeWars.decorateArmor(player, team.getColor(), ...)` | `ArmorContents.loadPlayerArmorContents()` | Puts the player's trims on the armor New Bedwars hands out |
| `CloudeWars.addPlayerCommands(...)` | `MainCommand.setSubCommands()`, before the help list is built | Adds `/bw trim` for players in the lobby and in games |
| `CloudeWars.teamMaterial(player, material)` | `CustomItem.getItem()` and `getIcon(Player)`, after the wool swap | Shop oak planks become the team's planks |
| `CloudeWars.checkTeamColor(player, color)` | `CreateTeamCommand.perform()`, after parsing the color | Refuses colors outside `team-colors` |
| `CloudeWars.teamColorNames()` | `CreateTeamCommand.getSubcommandArguments()` | Tab completion lists only the allowed colors |

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
files. Expect small conflicts in `pom.xml` and `plugin.yml` (version and name lines) and in `README.md` (keep ours).

## Other folders

- `archive/addon-base/`: the earlier addon version of Cloude Wars (Gradle), kept for reference. Not in git.
