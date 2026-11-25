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
import java.util.Locale;
import java.util.Set;
import me.sirsz.cloudewars.CloudeWars;
import me.sirsz.cloudewars.TeamColors;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Random team colors: before a round, every team gets a different random color from team-colors, and on New Bedwars
 * 2.8 and newer a display name that follows it ("Lime"; the team's own name stays). The color each team was set up
 * with is kept in the team's extra data ("base-color"): it numbers the teams for team blocks.
 */
@SuppressWarnings("deprecation") // New Bedwars' team colors are org.bukkit.ChatColor
public final class TeamColorRandomizer {

    private static final String BASE_COLOR = "base-color";

    private final JavaPlugin plugin;
    private final boolean enabled;
    private final List<String> colorNames;
    /** Arenas already warned about having more teams than team-colors has colors. */
    private final Set<String> warned = new HashSet<>();

    public TeamColorRandomizer(JavaPlugin plugin, boolean enabled, List<String> colorNames) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.colorNames = colorNames;
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
            String displayName = displayName(colors.get(i));
            editor.getTeam(team.getName()).ifPresent(teamEditor -> {
                teamEditor.setColor(color);
                if (CloudeWars.newBedwarsApiVersion() >= 1) {
                    teamEditor.setDisplayName(displayName); // New Bedwars 2.8-beta
                }
            });
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

    /** Paints every team's bed in the team's color when a game starts. */
    public void paintBeds(ArenaView arena) {
        if (!enabled) {
            return;
        }
        for (TeamView team : arena.getTeams()) {
            String colorName = TeamColors.name(team.getColor());
            Block bed = team.getBed() == null || colorName == null ? null : team.getBed().getBlock();
            if (bed != null && !(bed.getBlockData() instanceof Bed)) {
                bed = bed.getRelative(BlockFace.UP); // New Bedwars also accepts a bed one block above the saved spot
            }
            BlockData colored = bed == null ? null : TeamBlocks.coloredBed(bed.getBlockData(), colorName);
            if (colored != null) {
                TeamBlocks.setWithOtherHalf(bed, colored, otherHalf -> TeamBlocks.coloredBed(otherHalf, colorName));
            }
        }
    }
}
