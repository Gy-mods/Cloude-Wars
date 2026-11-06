# Cloude Wars

An addon for [New Bedwars](https://github.com/l299l/NewBedwars-Free) by l299l that adds armor trims, team wood, a map
maker (team blocks and symmetry), random team colors and a snow golem. New Bedwars does the Bedwars game itself
(arenas, shop, upgrades, generators, parties, network mode); Cloude Wars only uses its addon API.

## Installing

1. Install **New Bedwars 2.6-beta** (or newer).
2. Put `CloudeWars-<version>.jar` into `plugins/` and restart. Settings are in `plugins/CloudeWars/cloudewars.yml`.
3. Add the snow golem and the wood to the shop (see below), so nobody forgets them.

On a Velocity network, use New Bedwars' own network mode (its `docs/NETWORK.md`) and put Cloude Wars on the game
servers, where the arenas are.

### Snow golem and wood in the shop (don't forget!)

New Bedwars' addon API can add what the snow golem egg does, but not the item itself, so add it by hand (servers
updated from Cloude Wars 1.x already have all of this):

1. In `plugins/NewBedwars/data/items.yml`, add:
   ```yaml
   SnowGolem:
     material: SNOW_GOLEM_SPAWN_EGG
     amount: 1
     price: 5
     priceType: GOLD
     itemLogic: SNOW_GOLEM
     onBuyEvent: GIVE_ITEM
     isPermanent: false
     description: shop-item-description
     iconDesc: shop-itemIcon-description
     permission: ''
     enchantments: {}
     itemFlags: []
   ```
2. In the shop file (`plugins/NewBedwars/data/guis/exampleShop.yml`), add `- snowgolem` to the `misc` category's
   `Items`, after `- silverfish`. While you're there, add `- Wood` to the `blocks` category (New Bedwars has the
   item, but its default shop doesn't show it).
3. In `plugins/NewBedwars/language/en.yml`, under `CustomItemsNames:`, add:
   ```yaml
   SnowGolem-name: "&fSnow Golem"
   SnowGolem-icon-description: "&fSummons a fast snow golem that; &fpelts enemies with snowballs.; &7Comes back to you if it falls; &7into the void.; ; &7Cost: &e/price/"
   ```
4. Restart the server (or `/bw reload`).

### Updating from Cloude Wars 1.x

Cloude Wars 1.x was a whole Bedwars plugin and kept New Bedwars' files in `plugins/CloudeWars/`. Since 2.0 that's
New Bedwars' job, so:

1. Stop the server and remove the old `CloudeWars.jar`.
2. Install New Bedwars 2.6-beta and Cloude Wars 2.0.
3. Move `data/`, `generators/`, `language/`, `config.yml`, `bossBars.yml` and `scoreboards.yml` from
   `plugins/CloudeWars/` to `plugins/NewBedwars/`. Cloude Wars' own files (`cloudewars.yml`, `trims.yml`,
   `teamblocks/`, `symmetry/`) stay where they are.
4. Start the server. Your arenas, shop, stats and the snow golem item come along.

If you forget step 3, Cloude Wars reminds you in the console. Proxy mode from 1.x is gone: use New Bedwars' network
mode instead (lobby servers, arena selector, parties, rejoining and map voting across servers).

## What Cloude Wars adds

- **Armor trims**: `/bw trim <helmet|chestplate|leggings|boots> <trim>` picks a trim pattern for each armor piece.
  The trim material comes from your team color (white = quartz, black = netherite, red = redstone, orange = resin,
  yellow = gold, lime = emerald, blue = lapis, purple = amethyst), and it stays on armor upgrades too.
- **Team wood**: planks bought in the shop turn into your team's wood (white = pale oak, black = dark oak,
  red = mangrove, orange = acacia, yellow = bamboo, lime = jungle, blue = warped, purple = crimson). The shop still
  shows oak planks.
- **Random team colors**: before every game, each team gets a random color from the 8, so there aren't always the same
  four teams. Beds, armor, wood, trims and team blocks follow the color. Team names stay as the arena was set up
  (New Bedwars' API can't rename teams yet), so neutral names like "A" and "B" work best.
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
  `#2#`, and `#3#`, `#4#` with quarters). Then save the map with `/bw save`.
- **Snow golem**: a snow golem egg for 5 gold on the shop's Misc page. Like the snowman from Hypixel SkyWars'
  Snowman kit, it's faster than a normal one, throws snowballs 5x as fast (they knock enemies back), and if it falls
  into the void it comes back next to you with the same health.

Every team color works in New Bedwars; trims and team wood are set for the 8 colors above in `cloudewars.yml`, and
you can add others there. `/cloudewars reload` loads the settings again.

More is coming: team upgrades and the rest of the map maker (symmetry with a middle block line, copying team spawns,
shops and generators).

## Supported versions

**Paper 1.21.4 to 26.3** with **New Bedwars 2.6-beta** (Java 21; Minecraft 26.x itself needs Java 25). Cloude Wars 2.0
was tested in-game on 1.21.11 (on a Velocity network with New Bedwars' network mode) and started cleanly on 1.21.4
and 26.3. Folia is not supported.

## Building

Requires Java 21 and Maven: `mvn package` builds `target/CloudeWars-<version>.jar`. The New Bedwars API comes from
`libs/` for now (its JitPack build doesn't work yet). Developer notes are in [CLOUDEWARS.md](CLOUDEWARS.md).

Cloude Wars is licensed under the GNU GPL v3 (see `LICENSE.txt`), like New Bedwars.
