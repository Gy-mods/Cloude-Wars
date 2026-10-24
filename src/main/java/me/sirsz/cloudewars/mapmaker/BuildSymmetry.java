package me.sirsz.cloudewars.mapmaker;

import com.l299l.newbedwars.arena.IArena;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
 * The map maker's symmetry: the builder builds one part of the map and {@code /bw syncSymmetry} copies it to the
 * rest, around the map's middle (the corner at x = 0, z = 0, where New Bedwars starts every setup). The kinds, all
 * with no middle block line between the parts: four quarters (turned copies), and two halves, either mirrored or
 * central (turned by 180 degrees). Each arena's choice is saved in symmetry/&lt;arena&gt;.yml.
 */
public final class BuildSymmetry {

    /** The largest size (bigger maps would take very long to copy). */
    public static final int MAX_SIZE = 1000;
    private static final String FOLDER = "symmetry";

    /** What /bw setBuildSymmetry chose: a quarter, or a half with mirror or central symmetry. */
    private record Settings(int size, @Nullable Quarter quarter, @Nullable Half half, boolean mirror) {
        Layout layout() {
            return quarter != null ? Layout.quarters(size, quarter) : Layout.halves(size, half, mirror);
        }

        String kind() {
            return quarter != null ? "quarter" : mirror ? "mirror" : "central";
        }
    }

    private final JavaPlugin plugin;
    private final TeamBlockMarks teamBlockMarks;
    private final int defaultSize;
    private final Material markerBlock;
    private final int markerY;
    /** Arenas whose build part is being copied right now. */
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

    /** /bw setBuildSymmetry quarter nomiddle: the quarter the player stands in is the one they build. */
    public void setQuarters(Player player, IArena arena, int size) {
        Location location = player.getLocation();
        set(player, arena, new Settings(size, Quarter.of(location.getBlockX(), location.getBlockZ()), null, false));
    }

    /**
     * /bw setBuildSymmetry half nomiddle mirror|central: the half the player stands in is the one they build, split at
     * the line (z = 0 or x = 0) they stand farther from.
     */
    public void setHalves(Player player, IArena arena, boolean mirror, int size) {
        Location location = player.getLocation();
        set(player, arena, new Settings(size, null, Half.of(location.getBlockX(), location.getBlockZ()), mirror));
    }

    /** Saves the choice and gives the rest of the map a marker floor (only where there is air, so nothing is lost). */
    private void set(Player player, IArena arena, Settings settings) {
        save(arena, settings);
        Layout layout = settings.layout();
        World world = arena.getArenaWorld();
        for (Region other : layout.rest()) {
            for (int x = other.minX(); x <= other.maxX(); x++) {
                for (int z = other.minZ(); z <= other.maxZ(); z++) {
                    Block block = world.getBlockAt(x, markerY, z);
                    if (block.isEmpty()) {
                        block.setType(markerBlock, false);
                    }
                }
            }
        }
        Region build = layout.build();
        String part = settings.half() == null ? layout.name() : layout.name() + " (" + settings.kind() + ")";
        CloudeWars.send(player, "symmetry-set", "part", part, "quarter", part,
                "x", build.minX() + " to " + build.maxX(), "z", build.minZ() + " to " + build.maxZ(),
                "block", markerBlock.getKey().getKey().replace('_', ' '), "y", String.valueOf(markerY));
    }

    /** /bw syncSymmetry: copies the build part's blocks and team blocks to the rest of the map. */
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
        Layout layout = settings.layout();
        int teamBlocks = copyTeamBlocks(player, arena, layout);
        new SymmetryCopy(arena.getArenaWorld(), layout, player, changed -> {
            copying.remove(arena.getArenaName());
            if (player.isOnline()) {
                CloudeWars.send(player, "symmetry-synced", "blocks", String.valueOf(changed), "teamblocks", String.valueOf(teamBlocks));
            }
        }).runTaskTimer(plugin, 1, 1);
    }

    /**
     * Gives the copies the build part's team blocks, moved like the blocks and on to the next teams: with teams 1
     * and 2 in the build part, the first copy gets teams 3 and 4, the next 5 and 6, ... The rest of the map's old
     * team blocks are removed first. Returns how many team blocks were added.
     */
    private int copyTeamBlocks(Player player, IArena arena, Layout layout) {
        Map<BlockPos, Integer> marks = teamBlockMarks.marks(arena);
        Map<BlockPos, Integer> built = new HashMap<>();
        marks.entrySet().removeIf(mark -> {
            BlockPos pos = mark.getKey();
            if (!layout.map().contains(pos.x(), pos.z())) {
                return false;
            }
            if (layout.build().contains(pos.x(), pos.z())) {
                built.put(pos, mark.getValue());
                return false;
            }
            return true;
        });
        int teamsPerPart = built.values().stream().max(Integer::compare).orElse(0);
        int added = 0;
        int skipped = 0;
        for (Map.Entry<BlockPos, Integer> mark : built.entrySet()) {
            for (int i = 0; i < layout.copies().size(); i++) {
                int team = mark.getValue() + (i + 1) * teamsPerPart;
                if (team > teamBlockMarks.teamCount()) {
                    skipped++;
                    continue;
                }
                marks.put(layout.copies().get(i).move(mark.getKey()), team);
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
        int size = saved.getInt("size");
        if (saved.getBoolean("middle") || size < 1) {
            return null;
        }
        String type = saved.getString("type", "");
        if (type.equals("quarter")) {
            Quarter quarter = Quarter.byId(saved.getString("build-quarter", ""));
            return quarter == null ? null : new Settings(size, quarter, null, false);
        }
        Half half = Half.byId(saved.getString("build-half", ""));
        String kind = saved.getString("half", "");
        if (!type.equals("half") || half == null || !(kind.equals("mirror") || kind.equals("central"))) {
            return null;
        }
        return new Settings(size, null, half, kind.equals("mirror"));
    }

    private void save(IArena arena, Settings settings) {
        YamlConfiguration saved = new YamlConfiguration();
        saved.options().setHeader(List.of("Map maker symmetry of arena " + arena.getArenaName()
                + " (/bw setBuildSymmetry). The map is 2 x size blocks each way around x 0, z 0"));
        saved.set("type", settings.quarter() != null ? "quarter" : "half");
        saved.set("middle", false);
        saved.set("size", settings.size());
        if (settings.quarter() != null) {
            saved.set("build-quarter", settings.quarter().id());
        } else {
            saved.set("build-half", settings.half().id());
            saved.set("half", settings.kind());
        }
        File file = file(arena);
        file.getParentFile().mkdirs();
        try {
            saved.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}
