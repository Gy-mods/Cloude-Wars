package me.sirsz.cloudewars.teams;

import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.l299l.newbedwars.arena.team.Team;
import me.sirsz.cloudewars.CloudeWars;
import me.sirsz.cloudewars.TeamColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

/**
 * The map maker's team blocks. A builder in setup mode places a block named "#&lt;team&gt;#..." (renamed in an anvil);
 * its position and team number are saved in teamblocks/&lt;arena&gt;.yml, and when a game starts the block turns into
 * that team's version. The arena world is reloaded from disk after every game, which brings the placeholders back.
 */
public final class TeamBlockMarks implements Listener {

    private static final Pattern TEAM_NAME = Pattern.compile("^#(\\d+)#");
    private static final String FOLDER = "teamblocks";

    private record BlockPos(int x, int y, int z) {
        static BlockPos of(Block block) {
            return new BlockPos(block.getX(), block.getY(), block.getZ());
        }

        static @Nullable BlockPos parse(String text) {
            String[] parts = text.split(",");
            try {
                return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } catch (RuntimeException e) {
                return null;
            }
        }

        @Override
        public String toString() {
            return x + "," + y + "," + z;
        }
    }

    private final JavaPlugin plugin;
    private final TeamBlocks teamBlocks;
    /** The color name of each team number: index 0 is team 1. */
    private final List<String> teamColors;
    /** The marks of each arena (by arena name), loaded when first needed. */
    private final Map<String, Map<BlockPos, Integer>> marks = new HashMap<>();

    public TeamBlockMarks(JavaPlugin plugin, TeamBlocks teamBlocks, List<String> teamColors) {
        this.plugin = plugin;
        this.teamBlocks = teamBlocks;
        this.teamColors = teamColors;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        IArena arena = setupArena(player);
        Integer team = arena == null ? null : teamNumber(event.getItemInHand());
        if (team == null) {
            return;
        }
        if (team < 1 || team > teamColors.size()) {
            CloudeWars.send(player, "team-block-no-team", "team", String.valueOf(team), "max", String.valueOf(teamColors.size()));
            return;
        }
        if (teamBlocks.teamMaterial(event.getBlockPlaced().getType(), teamColors.get(team - 1)) == null) {
            CloudeWars.send(player, "team-block-no-variant");
            return;
        }
        arenaMarks(arena).put(BlockPos.of(event.getBlockPlaced()), team);
        save(arena);
        CloudeWars.actionBar(player, "team-block-marked", "team", String.valueOf(team));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        IArena arena = setupArena(event.getPlayer());
        if (arena == null) {
            return;
        }
        Integer team = arenaMarks(arena).remove(BlockPos.of(event.getBlock()));
        if (team != null) {
            save(arena);
            CloudeWars.actionBar(event.getPlayer(), "team-block-unmarked", "team", String.valueOf(team));
        }
    }

    /** Turns the arena's marked blocks into their teams' versions; called when its game starts. */
    public void recolor(IArena arena) {
        World world = arena.getArenaWorld();
        arenaMarks(arena).forEach((pos, team) -> {
            if (team >= 1 && team <= teamColors.size()) {
                recolor(world.getBlockAt(pos.x(), pos.y(), pos.z()), colorOfTeamNumber(arena, team));
            }
        });
    }

    /**
     * The color team &lt;number&gt; has this round: team 1 is the arena team created with the first color of
     * team-numbers, whatever color the randomizer gave it. Without such a team the number keeps that first color.
     */
    @SuppressWarnings("deprecation") // New Bedwars' team colors are org.bukkit.ChatColor
    private String colorOfTeamNumber(IArena arena, int number) {
        String numberColor = teamColors.get(number - 1);
        ChatColor baseColor = TeamColors.fromName(numberColor);
        for (Team team : arena.getTeams().values()) {
            if (team.getBaseColor() == baseColor) {
                String roundColor = CloudeWars.colorName(team.getColor());
                return roundColor != null ? roundColor : numberColor;
            }
        }
        return numberColor;
    }

    private void recolor(Block block, String colorName) {
        BlockData team = teamBlocks.teamData(block.getBlockData(), colorName);
        if (team == null) {
            return; // the placeholder was removed or replaced since it was marked
        }
        TeamBlocks.setWithOtherHalf(block, team, otherHalf -> teamBlocks.teamData(otherHalf, colorName));
    }

    /** The arena the player is setting up in this world, or null if they are not in setup mode here. */
    private static @Nullable IArena setupArena(Player player) {
        IArena arena = Arena.arenaByWorld.get(player.getWorld());
        return arena != null && arena.getPlayersOnSetup().contains(player.getUniqueId()) ? arena : null;
    }

    /** The team number in an item's name ("#2#..." is team 2), or null if it has none. */
    private static @Nullable Integer teamNumber(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        Component name = meta == null ? null : meta.customName();
        if (name == null) {
            return null;
        }
        Matcher matcher = TEAM_NAME.matcher(PlainTextComponentSerializer.plainText().serialize(name).trim());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private Map<BlockPos, Integer> arenaMarks(IArena arena) {
        return marks.computeIfAbsent(arena.getArenaName(), this::load);
    }

    private File file(String arenaName) {
        return new File(new File(plugin.getDataFolder(), FOLDER), arenaName + ".yml");
    }

    private Map<BlockPos, Integer> load(String arenaName) {
        Map<BlockPos, Integer> loaded = new HashMap<>();
        File file = file(arenaName);
        if (!file.exists()) {
            return loaded;
        }
        ConfigurationSection blocks = YamlConfiguration.loadConfiguration(file).getConfigurationSection("blocks");
        if (blocks != null) {
            for (String key : blocks.getKeys(false)) {
                BlockPos pos = BlockPos.parse(key);
                if (pos != null) {
                    loaded.put(pos, blocks.getInt(key));
                }
            }
        }
        return loaded;
    }

    private void save(IArena arena) {
        YamlConfiguration saved = new YamlConfiguration();
        saved.options().setHeader(List.of("Team blocks of arena " + arena.getArenaName()
                + ", placed with #<team>#-named items: \"x,y,z\": team number"));
        arenaMarks(arena).forEach((pos, team) -> saved.set("blocks." + pos, team));
        File file = file(arena.getArenaName());
        file.getParentFile().mkdirs();
        try {
            saved.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}
