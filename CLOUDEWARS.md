# Cloude Wars developer notes

Cloude Wars is an addon for [New Bedwars](https://github.com/l299l/NewBedwars-Free) by l299l (GPL-3.0, see
`LICENSE.txt`). Until 1.2.1 it was a fork of New Bedwars 2.4-beta; since 2.0.0 it is its own plugin that only uses New
Bedwars' addon API (`com.l299l.newbedwars.api`, documented in New Bedwars' `docs/api/`). 2.1.0 needs New Bedwars
2.7-beta. The git history still has the fork; the `upstream` remote is New Bedwars' repository (handy for reading its
code, e.g. `git show v2.7-beta:newbedwars-plugin/...`). Never push its tags (New Bedwars moved its `v2.6-beta` tag once,
so `git fetch upstream --tags` may refuse to update it; that's fine). The GitHub repository is
[Gy-mods/Cloude-Wars](https://github.com/Gy-mods/Cloude-Wars) (`origin`).

## New Bedwars versions and the API

- The API comes from JitPack (`com.github.l299l.NewBedwars-Free:newbedwars-api`, see `pom.xml`). It is built against
  New Bedwars' 2.8-beta development build (commit `aba66b9`); switch to the `v2.8-beta` tag once it is released.
- `CloudeWars.onEnable` refuses New Bedwars older than 2.7-beta (console error, plugin stays off). 2.7-beta added what
  2.1.0 uses everywhere: `registerShopItem`, `getShop()`, `registerCommand` with several contexts, `getSetupArena`,
  `TeamView.getBed()`, `ArenaView.getQuickVoidY()`.
- 2.8-beta additions are used only when `CloudeWars.newBedwarsApiVersion()` (New Bedwars' `getApiVersion()`, which
  2.7-beta doesn't have: 0 then) is 1 or more: `TeamEditor.setDisplayName`, `TeamView.getDisplayName()` and
  `ShopItemStackEvent` (`TeamShopWood`, only registered then: its class would not load on 2.7-beta). Anything else from
  2.8-beta needs the same check. To see what a build calls that an older API lacks, compare the compiled classes
  against the older API jar's members (`javap`); inherited methods like `ArenaEvent.getArena()` are fine.

## Code (`me.sirsz.cloudewars`)

- `CloudeWars`: the plugin. `onEnable` checks the New Bedwars version, loads `cloudewars.yml` and registers `/bw trim`
  (LOBBY and GAME), `/bw setBuildSymmetry` and `/bw syncSymmetry` (SETUP_BASIC), the `SNOW_GOLEM` shop logic and
  (`items.ShopItems`) the snow golem shop item. `reload()` (also `/cloudewars reload`) re-creates the features and
  their listeners; API registrations stay. It warns when it finds Cloude Wars 1.x Bedwars data
  (`plugins/CloudeWars/data/arenas`) that belongs in `plugins/NewBedwars`.
- `cosmetics`: armor trims. `ArmorTrims` listens to `ArmorEquipEvent` and adds the player's trims in the team color's
  material (`trims.materials`); choices are in `trims.yml` (per server). `TrimCommand` is `/bw trim`.
- `teams`: `TeamBlocks` (white block -> color, oak -> team wood), team wood (`TeamShopWood` on 2.8-beta: the shop
  shows and gives team planks; `TeamWood` on 2.7-beta: oak planks in the buyer's inventory are swapped after a
  `ShopPurchaseEvent`, dropped ones when picked up), `TeamBlockMarks` (map maker team blocks: `#N#` items placed by a
  player in setup mode, saved in `teamblocks/<arena>.yml`, recolored at `GameStartEvent`), `TeamColorRandomizer`
  (before a round, when the first player joins a waiting arena, `TeamEditor.setColor` gives the teams random colors
  from `team-colors`, and on 2.8-beta `setDisplayName` a matching name; each team's set-up color is kept in its extra
  data `base-color` for the team numbers; at game start each team's bed is painted), `GameListener` (the events).
- `items`: the snow golem (`SnowGolems`, `SnowGolemLogic` = shop logic `SNOW_GOLEM`; it comes back below the arena's
  quick void height, or after falling 3 blocks over nothing without one) and `ShopItems` (`registerShopItem` adds the
  `SnowGolem` item; the egg and `Wood` are put into every shop GUI once, after `Silverfish` / `Wool`, through the
  shop editor API; done GUIs are listed in `shop.yml`).
- `mapmaker`: symmetry (`BuildSymmetry`, `Layout`, `CopyRule`, `SymmetryCopy`, `Quarter`, `Half`, `Region`); see the
  README for what it does. Choices are in `symmetry/<arena>.yml`. After copying, `/bw save` updates New Bedwars' saved
  map copy (its map reset system since 2.5).

## Build and deploy

```bat
powershell -File tools\deploy.ps1
```

Runs `mvn package` (Maven from PATH, or the copy bundled with IntelliJ IDEA) and copies the jar to
`servers/bedwars1/plugins/CloudeWars.jar`. Stop `bedwars1` first: Windows keeps the loaded jar locked.

## Test network (`servers/`, only the start scripts are in git)

| Server | Address | RCON | What |
| --- | --- | --- | --- |
| `proxy` | **127.0.0.1:25575** | - | Velocity 4.3.0 (needs Java 25) + `NewBedwars-Proxy-<version>.jar`, offline mode, modern forwarding. Players connect here. |
| `lobby` | 127.0.0.1:25591 | 25592 | Paper 1.21.11 + New Bedwars, `Network.Mode: LOBBY` |
| `bedwars1` | 127.0.0.1:25593 | 25594 | Paper 1.21.11 + New Bedwars + Cloude Wars, `Network.Mode: GAME`, arena `mini` |

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

More test tools (each has its usage at the top):

- `tools/symmetry_test.py`: every symmetry kind in fresh test arenas on bedwars1, with team blocks placed by the
  `Claude` client; delete the `sym*` arenas and their `teamblocks/` / `symmetry/` files afterwards.
- `tools/snowball_watch.py`: counts the snow golems' snowballs and follows a player's position (knockback).
- `tools/load_test.py`: starts fresh Paper servers of other versions with plugin jars (downloads Paper) and reports
  how they loaded; the servers stay for a look at their files (e.g. the shop GUIs Cloude Wars changed).

## Other folders

- `archive/` (not in git): release jars and notes, the earlier Gradle addon attempt.
