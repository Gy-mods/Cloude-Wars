# Cloude-Wars
This is fork (it will be an addon) of New Bedwars giving some customizing features and proxy to it

## Based on New Bedwars

Cloude Wars is built on [New Bedwars](https://github.com/l299l/NewBedwars-Free) by l299l, forked from version
2.4-beta. Like New Bedwars, it is licensed under the GNU GPL v3 (see `LICENSE.txt`). Everything New Bedwars does
(arenas, shop, upgrades, generators, setup commands) works the same way here; see its readme for that.

## What Cloude Wars adds

- **Proxy mode** for Velocity networks: a game server runs one arena, players who connect go straight into it, and
  go back to the lobby server when the game ends. Settings are in `plugins/CloudeWars/cloudewars.yml`.
- **8 team colors**: white, black, red, orange, yellow, lime, blue and purple; `/bw createTeam` refuses the others.
- **Team wood**: planks bought in the shop come in your team's wood (white = pale oak, black = dark oak,
  red = mangrove, orange = acacia, yellow = bamboo, lime = jungle, blue = warped, purple = crimson).
- **Armor trims**: `/bw trim <helmet|chestplate|leggings|boots> <trim>` picks a trim pattern for each armor piece.
  The trim material comes from your team color (white = quartz, black = netherite, red = redstone, orange = resin,
  yellow = gold, lime = emerald, blue = lapis, purple = amethyst), and it stays on armor upgrades too.

More is coming: team upgrades and a map maker.

## Building

Requires Java 21 and Maven: `mvn package` builds `target/CloudeWars-<version>.jar` for Paper 1.21.11.
Developer notes (test network, tools, pulling New Bedwars updates) are in [CLOUDEWARS.md](CLOUDEWARS.md).
