# Cloude-Wars
This is fork (it will be an addon) of New Bedwars giving some customizing features and proxy to it

## Based on New Bedwars

Cloude Wars is built on [New Bedwars](https://github.com/l299l/NewBedwars-Free) by l299l, forked from version
2.4-beta. Like New Bedwars, it is licensed under the GNU GPL v3 (see `LICENSE.txt`). Everything New Bedwars does
(arenas, shop, upgrades, generators, setup commands) works the same way here; see its readme for that.

## What Cloude Wars adds

- **Proxy mode** for Velocity networks: a game server runs one arena, players who connect go straight into it, and
  go back to the lobby server when the game ends. Settings are in `plugins/CloudeWars/cloudewars.yml`.

More is coming: team upgrades, a map maker and armor trim cosmetics.

## Building

Requires Java 21 and Maven: `mvn package` builds `target/CloudeWars-<version>.jar` for Paper 1.21.11.
Developer notes (test network, tools, pulling New Bedwars updates) are in [CLOUDEWARS.md](CLOUDEWARS.md).
