# Cloude Wars

Cloude Wars is a Bedwars plugin for Paper 1.21.11. It is a fork of
[New Bedwars](https://github.com/l299l/NewBedwars-Free) by l299l (GPL-3.0, see `LICENSE.txt`), forked from
**2.4-beta** (upstream commit `29ff496`, 22.10.2026). `README.md` is New Bedwars' own readme and stays as upstream has it.

**Status:** fork set up, no Cloude Wars features yet. Changes from upstream so far are only the plugin identity:
`plugin.yml` (name `CloudeWars`, authors, description) and `pom.xml` (artifact `CloudeWars`, version
`0.1.0-SNAPSHOT`). The Java packages stay `com.l299l.newbedwars`, so upstream updates merge cleanly.

## Build and deploy

```bat
powershell -File tools\deploy.ps1
```

Runs `mvn package` (Maven from PATH, or the copy bundled with IntelliJ IDEA) and copies the jar to
`server/plugins/CloudeWars.jar`. Add `-SkipTests` to skip New Bedwars' unit tests. Stop the test server first:
Windows keeps the loaded jar locked.

## Test server (`server/`, not in git except `start.bat`)

- Paper 1.21.11 (build 132) on **127.0.0.1:25575**, offline mode, RCON on 25576.
- Start: `server\start.bat`. Console commands from outside: `python tools\rcon.py "plugins" "stop"`.
- `/bw` commands only work for players, not the console.

## In-game tests (MCP Control)

The MCP Control bridge (`../MCPControl`) starts an offline 1.21.11 client: `minecraft_launch` with
`version: "fabric-loader-0.18.4-1.21.11"`, then `connect_to_server` with `address: "localhost:25575"`.
Use `op Claude` over RCON when a test needs admin commands.

## Pulling New Bedwars updates

`upstream` is New Bedwars' repository and `main` is our branch.

```bat
git fetch upstream
git merge upstream/master
```

To keep merges easy: put Cloude Wars code in its own package, and only add small hooks to New Bedwars' files.
Expect small conflicts in `pom.xml` and `plugin.yml` (version and name lines).

## Other folders

- `archive/addon-base/`: the earlier addon version of Cloude Wars (Gradle), kept for reference. Not in git.
