package me.sirsz.cloudewars.teams;

import com.l299l.newbedwars.api.edit.ArenaEditor;
import com.l299l.newbedwars.api.view.ArenaState;
import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.ExtraData;
import com.l299l.newbedwars.api.view.TeamView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.sirsz.cloudewars.CloudeWars;
import me.sirsz.cloudewars.TeamColors;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

/**
 * Random team colors: before a round, every team gets a different random color from team-colors. New Bedwars' API
 * can change a team's color but not its name, so the names stay as the arena was set up. The color each team was set
 * up with is kept in the team's extra data ("base-color"): it numbers the teams for team blocks.
 */
@SuppressWarnings("deprecation") // New Bedwars' team colors are org.bukkit.ChatColor
public final class TeamColorRandomizer {

    private static final String BASE_COLOR = "base-color";

    private final JavaPlugin plugin;
    private final boolean enabled;
    private final List<String> colorNames;
    private final int bedDistance;
    /** Arenas already warned about having more teams than team-colors has colors. */
    private final Set<String> warned = new HashSet<>();

    public TeamColorRandomizer(JavaPlugin plugin, boolean enabled, List<String> colorNames, int bedDistance) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.colorNames = colorNames;
        this.bedDistance = bedDistance;
    }

    /** The color the team was set up with, whatever color the randomizer gave it since. */
    public static ChatColor baseColor(TeamView team) {
        String saved = team.getExtra(CloudeWars.plugin()).getString(BASE_COLOR, "");
        try {
            return saved.isEmpty() ? team.getColor() : ChatColor.valueOf(saved);
        } catch (IllegalArgumentException e) {
            return team.getColor();
        }
    }

    /** Gives every team of a waiting arena a different random color (saved with the arena, like a setup change). */
    public void randomize(ArenaView arena) {
        if (!enabled || arena.getState() != ArenaState.WAITING) {
            return;
        }
        List<TeamView> teams = new ArrayList<>(arena.getTeams());
        if (teams.size() > colorNames.size()) {
            if (warned.add(arena.getName())) {
                plugin.getLogger().warning("Arena " + arena.getName() + " has " + teams.size() + " teams but team-colors only "
                        + colorNames.size() + " colors, so its teams keep their colors.");
            }
            return;
        }
        teams.sort(Comparator.comparing(TeamView::getName));
        List<String> colors = new ArrayList<>(colorNames);
        Collections.shuffle(colors);
        ArenaEditor editor = CloudeWars.api().edit(arena);
        for (int i = 0; i < teams.size(); i++) {
            TeamView team = teams.get(i);
            ExtraData extra = team.getExtra(plugin);
            if (!extra.has(BASE_COLOR)) {
                extra.set(BASE_COLOR, team.getColor().name());
            }
            ChatColor color = TeamColors.fromName(colors.get(i));
            editor.getTeam(team.getName()).ifPresent(teamEditor -> teamEditor.setColor(color));
        }
    }

    /**
     * Paints every team's bed in the team's color when a game starts. The API does not tell where a team's bed is, so
     * it is the bed nearest to the team spawn (at most random-team-colors-bed-distance blocks away).
     */
    public void paintBeds(ArenaView arena) {
        if (!enabled) {
            return;
        }
        for (TeamView team : arena.getTeams()) {
            String colorName = TeamColors.name(team.getColor());
            Block bed = team.getSpawn() == null || colorName == null ? null : nearestBed(team.getSpawn());
            BlockData colored = bed == null ? null : TeamBlocks.coloredBed(bed.getBlockData(), colorName);
            if (colored != null) {
                TeamBlocks.setWithOtherHalf(bed, colored, otherHalf -> TeamBlocks.coloredBed(otherHalf, colorName));
            }
        }
    }

    private @Nullable Block nearestBed(Location spawn) {
        World world = spawn.getWorld();
        int chunks = (bedDistance >> 4) + 1;
        Block nearest = null;
        double nearestDistance = (double) bedDistance * bedDistance;
        for (int chunkX = (spawn.getBlockX() >> 4) - chunks; chunkX <= (spawn.getBlockX() >> 4) + chunks; chunkX++) {
            for (int chunkZ = (spawn.getBlockZ() >> 4) - chunks; chunkZ <= (spawn.getBlockZ() >> 4) + chunks; chunkZ++) {
                for (BlockState state : world.getChunkAt(chunkX, chunkZ).getTileEntities(false)) {
                    double distance = state.getLocation().add(0.5, 0, 0.5).distanceSquared(spawn);
                    if (state.getBlockData() instanceof Bed && distance <= nearestDistance) {
                        nearest = state.getBlock();
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }
}
