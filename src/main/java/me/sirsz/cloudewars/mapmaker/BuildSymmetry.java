package me.sirsz.cloudewars.mapmaker;

import com.l299l.newbedwars.arena.IArena;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import me.sirsz.cloudewars.BlockPos;
import me.sirsz.cloudewars.CloudeWars;
import me.sirsz.cloudewars.teams.TeamBlockMarks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

/**
 * The map maker's symmetry: the builder builds one quarter of the map and {@code /bw syncSymmetry} copies it to the
 * other three, turned around the map's middle (the corner at x = 0, z = 0, where New Bedwars starts every setup).
 * Each arena's choice is saved in symmetry/&lt;arena&gt;.yml. For now there is one kind: four quarters with no middle
 * block line between them.
 */
public final class BuildSymmetry {

    /** The largest quarter size (bigger maps would take very long to copy). */
    public static final int MAX_SIZE = 1000;
    private static final String FOLDER = "symmetry";

    private record Settings(int size, Quarter build) {
    }

    private final JavaPlugin plugin;
    private final TeamBlockMarks teamBlockMarks;
    private final int defaultSize;
    private final Material markerBlock;
    private final int markerY;
    /** Arenas whose quarter is being copied right now. */
    private final Set<String> copying = new HashSet<>();

    public BuildSymmetry(JavaPlugin plugin, TeamBlockMarks teamBlockMarks, YamlConfiguration config) {
        this.plugin = plugin;
        this.teamBlockMarks = teamBlockMarks;
        this.defaultSize = Math.max(1, Math.min(MAX_SIZE, config.getInt("symmetry.default-size")));
        String marker = config.getString("symmetry.marker-block"); // without a fallback argument, the jar's default is used
        Material material = marker == null ? null : Material.matchMaterial(marker);
        if (material == null || !material.isBlock()) {
            plugin.getLogger().warning("cloudewars.yml symmetry.marker-block: '" + marker + "' is no block, using terracotta");
            material = Material.TERRACOTTA;
        }
        this.markerBlock = material;
        this.markerY = config.getInt("symmetry.marker-y");
    }

    public int defaultSize() {
        return defaultSize;
    }

    /**
     * /bw setBuildSymmetry quarter nomiddle: the quarter the player stands in becomes the build quarter, and the other
     * three get a marker floor (only where there is air, so nothing built is lost).
     */
    public void setQuarters(Player player, IArena arena, int size) {
        Location location = player.getLocation();
        Quarter build = Quarter.of(location.getBlockX(), location.getBlockZ());
        save(arena, new Settings(size, build));
        World world = arena.getArenaWorld();
        for (int steps = 1; steps < 4; steps++) {
            Quarter other = build.clockwise(steps);
            for (int x = other.minX(size); x <= other.maxX(size); x++) {
                for (int z = other.minZ(size); z <= other.maxZ(size); z++) {
                    Block block = world.getBlockAt(x, markerY, z);
                    if (block.isEmpty()) {
                        block.setType(markerBlock, false);
                    }
                }
            }
        }
        CloudeWars.send(player, "symmetry-set", "quarter", build.id(),
                "x", build.minX(size) + " to " + build.maxX(size), "z", build.minZ(size) + " to " + build.maxZ(size),
                "block", markerBlock.getKey().getKey().replace('_', ' '), "y", String.valueOf(markerY));
    }

    /** /bw syncSymmetry: copies the build quarter's blocks and team blocks to the other three quarters. */
    public void sync(Player player, IArena arena) {
        Settings settings = load(arena);
        if (settings == null) {
            CloudeWars.send(player, "symmetry-none");
            return;
        }
        if (!copying.add(arena.getArenaName())) {
            CloudeWars.send(player, "symmetry-busy");
            return;
        }
        int teamBlocks = copyTeamBlocks(player, arena, settings);
        new QuarterCopy(arena.getArenaWorld(), settings.build(), settings.size(), player, changed -> {
            copying.remove(arena.getArenaName());
            if (player.isOnline()) {
                CloudeWars.send(player, "symmetry-synced", "blocks", String.valueOf(changed), "teamblocks", String.valueOf(teamBlocks));
            }
        }).runTaskTimer(plugin, 1, 1);
    }

    /**
     * Gives the other quarters the build quarter's team blocks, turned with the map and moved on to the next teams
     * clockwise: with teams 1 and 2 in the build quarter, the copies get teams 3 and 4, 5 and 6, 7 and 8. The other
     * quarters' old team blocks are removed first. Returns how many team blocks were added.
     */
    private int copyTeamBlocks(Player player, IArena arena, Settings settings) {
        Map<BlockPos, Integer> marks = teamBlockMarks.marks(arena);
        Map<BlockPos, Integer> built = new HashMap<>();
        marks.entrySet().removeIf(mark -> {
            BlockPos pos = mark.getKey();
            if (!Quarter.inMap(pos.x(), pos.z(), settings.size())) {
                return false;
            }
            if (Quarter.of(pos.x(), pos.z()) == settings.build()) {
                built.put(pos, mark.getValue());
                return false;
            }
            return true;
        });
        int teamsPerQuarter = built.values().stream().max(Integer::compare).orElse(0);
        int added = 0;
        int skipped = 0;
        for (Map.Entry<BlockPos, Integer> mark : built.entrySet()) {
            for (int steps = 1; steps < 4; steps++) {
                int team = mark.getValue() + steps * teamsPerQuarter;
                if (team > teamBlockMarks.teamCount()) {
                    skipped++;
                    continue;
                }
                marks.put(Quarter.turn(mark.getKey(), steps), team);
                added++;
            }
        }
        teamBlockMarks.setMarks(arena, marks);
        if (skipped > 0) {
            CloudeWars.send(player, "symmetry-team-blocks-skipped", "count", String.valueOf(skipped),
                    "max", String.valueOf(teamBlockMarks.teamCount()));
        }
        return added;
    }

    private File file(IArena arena) {
        return new File(new File(plugin.getDataFolder(), FOLDER), arena.getArenaName() + ".yml");
    }

    private @Nullable Settings load(IArena arena) {
        File file = file(arena);
        if (!file.exists()) {
            return null;
        }
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(file);
        Quarter build = Quarter.byId(saved.getString("build-quarter", ""));
        int size = saved.getInt("size");
        if (!"quarter".equals(saved.getString("type")) || saved.getBoolean("middle") || build == null || size < 1) {
            return null;
        }
        return new Settings(size, build);
    }

    private void save(IArena arena, Settings settings) {
        YamlConfiguration saved = new YamlConfiguration();
        saved.options().setHeader(List.of("Map maker symmetry of arena " + arena.getArenaName()
                + " (/bw setBuildSymmetry). size: each quarter is size x size blocks around x 0, z 0"));
        saved.set("type", "quarter");
        saved.set("middle", false);
        saved.set("size", settings.size());
        saved.set("build-quarter", settings.build().id().toLowerCase(Locale.ROOT));
        File file = file(arena);
        file.getParentFile().mkdirs();
        try {
            saved.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}
