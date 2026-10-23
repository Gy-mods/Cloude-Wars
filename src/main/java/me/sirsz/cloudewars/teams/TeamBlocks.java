package me.sirsz.cloudewars.teams;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import me.sirsz.cloudewars.TeamColors;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

/** Team versions of blocks: oak is the placeholder wood, and each team color has its own wood type. */
@SuppressWarnings("deprecation") // New Bedwars' team colors are org.bukkit.ChatColor
public final class TeamBlocks {

    private static final Material PLACEHOLDER_PLANKS = Material.OAK_PLANKS;

    /** The planks of each team color's wood type. */
    private final Map<ChatColor, Material> planks = new EnumMap<>(ChatColor.class);

    public TeamBlocks(JavaPlugin plugin, @Nullable ConfigurationSection woodConfig) {
        if (woodConfig == null) {
            return;
        }
        for (String colorName : woodConfig.getKeys(false)) {
            String wood = woodConfig.getString(colorName, "");
            ChatColor color = TeamColors.fromName(colorName);
            Material woodPlanks = Material.matchMaterial(wood.toUpperCase(Locale.ROOT) + "_PLANKS");
            if (color == null || woodPlanks == null) {
                plugin.getLogger().warning("cloudewars.yml team-wood: skipping '" + colorName + ": " + wood
                        + "' (unknown team color or wood type)");
                continue;
            }
            planks.put(color, woodPlanks);
        }
    }

    /** The team version of a block for this team color, or the block itself if it has none. */
    public Material forTeam(Material material, ChatColor teamColor) {
        if (material == PLACEHOLDER_PLANKS) {
            return planks.getOrDefault(teamColor, material);
        }
        return material;
    }
}
