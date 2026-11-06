# Cloude Wars developer notes

Cloude Wars is an addon for [New Bedwars](https://github.com/l299l/NewBedwars-Free) by l299l (GPL-3.0, see
`LICENSE.txt`). Until 1.2.1 it was a fork of New Bedwars 2.4-beta; since 2.0.0 it is its own plugin that needs New
Bedwars 2.6-beta and only uses New Bedwars' addon API (`com.l299l.newbedwars.api`, see New Bedwars' `docs/API.md`).
The git history still has the fork; the `upstream` remote is New Bedwars' repository (handy for reading its code, e.g.
`git show v2.6-beta:newbedwars-plugin/...`). Never push its tags. The GitHub repository is
[Gy-mods/Cloude-Wars](https://github.com/Gy-mods/Cloude-Wars) (`origin`).

## Code (`me.sirsz.cloudewars`)

- `CloudeWars`: the plugin. `onEnable` gets `NewBedwarsAPI`, loads `cloudewars.yml` and registers `/bw trim` (GAME),
  `/bw setBuildSymmetry` and `/bw syncSymmetry` (SETUP_BASIC) and the `SNOW_GOLEM` shop logic. `reload()` (also
  `/cloudewars reload`) re-creates the features and their listeners; API registrations stay. It warns when it finds
  Cloude Wars 1.x Bedwars data (`plugins/CloudeWars/data/arenas`) that belongs in `plugins/NewBedwars`.
- `cosmetics`: armor trims. `ArmorTrims` listens to `ArmorEquipEvent` and adds the player's trims in the team color's
  material (`trims.materials`); choices are in `trims.yml` (per server). `TrimCommand` is `/bw trim`; New Bedwars
  allows a command name in one context only, so it is registered for GAME and `TrimCommand.LobbyCommand` answers
  `/bw trim` outside arenas (command preprocess + tab complete events).
- `teams`: `TeamBlocks` (white block -> color, oak -> team wood), `TeamWood` (after a `ShopPurchaseEvent`, oak planks
  in the buyer's inventory turn into team planks; dropped ones when picked up), `TeamBlockMarks` (map maker team
  blocks: `#N#` items placed in an arena world while the arena is disabled, saved in `teamblocks/<arena>.yml`,
  recolored at `GameStartEvent`), `TeamColorRandomizer` (before a round, when the first player joins a waiting arena,
  `ArenaEditor`/`TeamEditor.setColor` give the teams random colors from `team-colors`; each team's set-up color is
  kept in its extra data `base-color` for the team numbers; at game start the bed nearest to each team spawn is
  painted), `GameListener` (the events for both).
- `items`: the snow golem (`SnowGolems`, `SnowGolemLogic` = shop logic `SNOW_GOLEM`). The API has no quick void
  height, so the golem comes back when it falls 3 blocks with no block below it.
- `mapmaker`: symmetry (`BuildSymmetry`, `Layout`, `CopyRule`, `SymmetryCopy`, `Quarter`, `Half`, `Region`); see the
  README for what it does. Choices are in `symmetry/<arena>.yml`. After copying, `/bw save` updates New Bedwars' saved
  map copy (its map reset system since 2.5).

### What New Bedwars' API does not offer yet (workarounds in Cloude Wars)

- Renaming a team: random colors keep the team names.
- Changing the item a shop item gives / shows: team wood swaps the planks after buying; the shop shows oak.
- Adding a shop item (`items.yml` entry) from an addon: the snow golem item is added by hand (README).
- One command name in several contexts: `/bw trim` outside arenas goes through `TrimCommand.LobbyCommand`.
- Who is in setup mode, a team's bed location, an arena's quick void height: disabled arena = setup, nearest bed to
  the spawn, "no block below" for the golem.
- JitPack: New Bedwars' 2.6-beta JitPack build fails ("maven-compiler-plugin 3.13.0 requires Maven version 3.6.3"),
  so the API jar is kept in `libs/` (a file-based Maven repository, see `pom.xml`). Switch to
  `com.github.l299l.NewBedwars-Free:newbedwars-api:<tag>` from JitPack once it builds.

To update the API in `libs/`: build New Bedwars (`mvn install -DskipTests` with Java 25 for its proxy module) and run
`mvn install:install-file -Dfile=<newbedwars-api jar> -DgroupId=com.l299l -DartifactId=newbedwars-api
-Dversion=<version> -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=libs`.

## Build and deploy

```bat
powershell -File tools\deploy.ps1
```

Runs `mvn package` (Maven from PATH, or the copy bundled with IntelliJ IDEA) and copies the jar to
`servers/bedwars1/plugins/CloudeWars.jar`. Stop `bedwars1` first: Windows keeps the loaded jar locked.

## Test network (`servers/`, only the start scripts are in git)

| Server | Address | RCON | What |
| --- | --- | --- | --- |
| `proxy` | **127.0.0.1:25575** | - | Velocity 4.3.0 (needs Java 25) + `NewBedwars-Proxy-2.6-beta.jar`, offline mode, modern forwarding. Players connect here. |
| `lobby` | 127.0.0.1:25591 | 25592 | Paper 1.21.11 + New Bedwars 2.6-beta, `Network.Mode: LOBBY` |
| `bedwars1` | 127.0.0.1:25593 | 25594 | Paper 1.21.11 + New Bedwars 2.6-beta + Cloude Wars, `Network.Mode: GAME`, arena `mini` |

- Start everything: `servers\start-all.bat` (one window per server). Stop the Paper servers with `stop`, the proxy with `shutdown`.
- Players join a game from the lobby with `/bw join mini` (or `/bw join`); the proxy sends them to bedwars1.
- Console commands from outside: `python tools\rcon.py [--server lobby] "list"` (default server: `bedwars1`).
- Arena `mini`: two islands (1 player each, 2 needed to start, 10 s countdown) and a middle island with a diamond
  generator; its teams were set up as red and blue. The arena world's dimension is `minecraft:mini`; console
  teleports into it need `execute in minecraft:mini run tp ...`.
- The proxy is in offline mode and only listens on 127.0.0.1. Before letting other PCs or the internet in, turn on
  `online-mode` in `velocity.toml` and `proxies.velocity.online-mode` in each server's `config/paper-global.yml`.

## In-game tests (MCP Control)

`tools/mc_clients.py` starts offline clients through MCP Control (`../MCPControl`), each with its own bridge, and
drives them over MCP Control's HTTP API (run it from Bash; PowerShell mangles JSON arguments with spaces):

```bash
python tools/mc_clients.py launch Tester1 25582
python tools/mc_clients.py call 25582 connect_to_server '{"address":"localhost:25575"}'
python tools/mc_clients.py call 25582 run_command '{"command":"bw join mini"}'
python tools/mc_clients.py stop 25582
```

An optional third `launch` argument picks another SKLauncher version, e.g. `fabric-loader-0.19.5-26.3` for a 26.3
client (MCP Control has builds for Fabric 1.21.11 and 26.3 and NeoForge 1.21.1).

`Claude` is an op on `bedwars1` (it has `newbedwars.network.bypass`, so it can stay there without a join ticket, e.g.
to set up arenas). If a command answers "Chat disabled in client options", the player is dead: `respawn` first.
Clients can't see chest contents; check them with RCON `data get block`.

## Other folders

- `libs/`: the New Bedwars API (see above).
- `archive/` (not in git): release jars and notes, the earlier Gradle addon attempt.
