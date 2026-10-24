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
- **Team blocks** (map maker): while building an arena, rename white wool, terracotta, concrete, glass, oak planks,
  stairs, logs and so on to `#<team>#...` (for example `#1#wall#`) in an anvil and place them. When a game starts
  they turn into that team's color or wood, so a map stores team numbers instead of colors. On 26.3 and newer this
  includes the new wool and concrete slabs and stairs.
- **Symmetry** (map maker): build only part of a map and let Cloude Wars copy it. Stand in the part you want to
  build and pick a symmetry (the map is 2 x size blocks each way around x 0, z 0, and the rest of the map gets a
  terracotta floor at y 64 so you can see where your part ends):
  - `/bw setBuildSymmetry quarter nomiddle [size]` for 4 teams: you build a quarter, the copies are turned.
  - `/bw setBuildSymmetry half nomiddle mirror [size]` for 2 teams: you build a half (the line between the halves is
    x 0 or z 0, whichever you stand farther from) and the copy is mirrored.
  - `/bw setBuildSymmetry half nomiddle central [size]` for 2 teams: the copy is turned around the middle instead,
    so both teams have the same things on the same side (like the ender chest on their right).

  When you're done, `/bw syncSymmetry` copies your part to the rest of the map: stairs, beds, logs, doors and so on
  face the right way, chests and signs keep their contents, and team blocks go to the next teams (`#1#` becomes
  `#2#`, and `#3#`, `#4#` with quarters).
- **Armor trims**: `/bw trim <helmet|chestplate|leggings|boots> <trim>` picks a trim pattern for each armor piece.
  The trim material comes from your team color (white = quartz, black = netherite, red = redstone, orange = resin,
  yellow = gold, lime = emerald, blue = lapis, purple = amethyst), and it stays on armor upgrades too.
- **Random team colors**: every game, each team gets a random color from the 8, so there aren't always the same
  four teams. Beds, team blocks, armor, wood and trims follow the color.
- **Snow golem**: a snow golem egg for 5 gold on the shop's Misc page. Like the snowman from Hypixel SkyWars'
  Snowman kit, it's faster than a normal one, throws snowballs 5x as fast (they knock enemies back), and if it falls
  into the void it comes back next to you with the same health.

More is coming: team upgrades and the rest of the map maker (symmetry with a middle block line, copying team spawns,
shops and generators).

## Supported versions

**Paper 1.21.4 to 26.3** (Java 21; Minecraft 26.x itself needs Java 25). Tested in-game on 1.21.11 and 26.3, and
started cleanly on 1.21.4, 1.21.8, 1.21.10, 26.1.2 and 26.2. On 1.21.1 to 1.21.3 the plugin starts, but team blocks
do not work there and the game has no resin (orange trims) or pale oak (white wood) yet. Proxy mode was tested with
Velocity 4.3. Folia is not supported.

## Building

Requires Java 21 and Maven: `mvn package` builds `target/CloudeWars-<version>.jar` for Paper 1.21.11.
Developer notes (test network, tools, pulling New Bedwars updates) are in [CLOUDEWARS.md](CLOUDEWARS.md).
