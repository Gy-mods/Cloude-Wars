package me.sirsz.cloudewars.teams;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.arena.team.TeamBed;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import me.sirsz.cloudewars.TeamColors;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Random team colors: when a game starts, every team gets a different random color from team-colors, and its name
 * on screen follows ("Lime"). The arena keeps its saved colors; they only number the teams for team blocks.
 */
@SuppressWarnings("deprecation") // New Bedwars' team colors are org.bukkit.ChatColor
public final class TeamColorRandomizer {

    private final JavaPlugin plugin;
    private final boolean enabled;
    private final List<String> colorNames;

    public TeamColorRandomizer(JavaPlugin plugin, boolean enabled, List<String> colorNames) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.colorNames = colorNames;
    }

    /** Gives every team of the arena a different random color and paints the team beds in it. */
    public void randomize(IArena arena) {
        if (!enabled) {
            return;
        }
        List<Team> teams = new ArrayList<>(arena.getTeams().values());
        if (teams.size() > colorNames.size()) {
            plugin.getLogger().warning("Arena " + arena.getArenaName() + " has " + teams.size() + " teams but team-colors only "
                    + colorNames.size() + " colors, so its teams keep their saved colors.");
            return;
        }
        teams.sort(Comparator.comparing(Team::getName));
        List<String> colors = new ArrayList<>(colorNames);
        Collections.shuffle(colors);
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            String colorName = colors.get(i);
            team.setRoundColor(TeamColors.fromName(colorName), displayName(colorName));
            paintBed(team, colorName);
        }
    }

    /** Back to the saved colors and names, e.g. when an arena is disabled to be set up. */
    public void clear(IArena arena) {
        arena.getTeams().values().forEach(Team::clearRoundColor);
    }

    private static void paintBed(Team team, String colorName) {
        TeamBed teamBed = team.getTeamBed();
        if (teamBed == null) {
            return;
        }
        Block block = teamBed.getLocation().getBlock();
        if (!(block.getBlockData() instanceof Bed)) {
            block = block.getRelative(BlockFace.UP); // New Bedwars also accepts a bed one block above the saved spot
        }
        BlockData colored = TeamBlocks.coloredBed(block.getBlockData(), colorName);
        if (colored != null) {
            TeamBlocks.setWithOtherHalf(block, colored, otherHalf -> TeamBlocks.coloredBed(otherHalf, colorName));
        }
    }

    /** "light_blue" becomes "Light Blue". */
    private static String displayName(String colorName) {
        StringBuilder name = new StringBuilder();
        for (String word : colorName.toLowerCase(Locale.ROOT).split("_")) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }
}
