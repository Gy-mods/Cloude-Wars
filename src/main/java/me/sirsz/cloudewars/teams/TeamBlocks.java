package me.sirsz.cloudewars.teams;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.sirsz.cloudewars.TeamColors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

/**
 * Team versions of blocks. White blocks are the placeholders of dye-colored blocks (white wool becomes red wool) and
 * oak blocks the placeholders of wood (oak stairs become mangrove stairs); colors are named as in /bw createTeam.
 */
@SuppressWarnings("deprecation") // New Bedwars' team colors are org.bukkit.ChatColor
public final class TeamBlocks {

    private static final String DYE_PLACEHOLDER = "WHITE_";
    private static final String WOOD_PLACEHOLDER = "OAK_";
    private static final String STRIPPED = "STRIPPED_";
    /** Blocks whose block entity holds data a color swap would lose (sign text, banner patterns, contents). */
    private static final List<String> NOT_SWAPPED = List.of("SIGN", "BANNER", "SHULKER_BOX", "SHELF");

    /** Each color's wood type, by lower-case color name. */
    private final Map<String, String> woods = new HashMap<>();
    /** The color name of each New Bedwars team color that has a wood type. */
    private final Map<ChatColor, String> colorNames = new EnumMap<>(ChatColor.class);

    public TeamBlocks(JavaPlugin plugin, @Nullable ConfigurationSection woodConfig) {
        if (woodConfig == null) {
            return;
        }
        for (String colorName : woodConfig.getKeys(false)) {
            String wood = woodConfig.getString(colorName, "").toUpperCase(Locale.ROOT);
            ChatColor color = TeamColors.fromName(colorName);
            if (color == null || Material.matchMaterial(wood + "_PLANKS") == null) {
                plugin.getLogger().warning("cloudewars.yml team-wood: skipping '" + colorName + ": " + wood
                        + "' (unknown team color or wood type)");
                continue;
            }
            woods.put(colorName.toLowerCase(Locale.ROOT), wood);
            colorNames.put(color, colorName.toLowerCase(Locale.ROOT));
        }
    }

    /** Whether a color name can color blocks (it is a dye color, e.g. "lime"). */
    public static boolean isDyeColor(String colorName) {
        return Material.matchMaterial(colorName.toUpperCase(Locale.ROOT) + "_WOOL") != null;
    }

    /** Shop blocks: oak planks become the team color's planks; anything else stays as it is. */
    public Material forTeam(Material material, ChatColor teamColor) {
        String colorName = colorNames.get(teamColor);
        if (material != Material.OAK_PLANKS || colorName == null) {
            return material;
        }
        Material planks = teamMaterial(material, colorName);
        return planks != null ? planks : material;
    }

    /** The color's version of a placeholder block, keeping its direction and shape; null if it has none. */
    public @Nullable BlockData teamData(BlockData placeholder, String colorName) {
        Material target = teamMaterial(placeholder.getMaterial(), colorName);
        if (target == null) {
            return null;
        }
        String states = placeholder.getAsString().substring(placeholder.getMaterial().getKey().toString().length());
        try {
            return Bukkit.createBlockData(target, states);
        } catch (IllegalArgumentException e) {
            return target.createBlockData();
        }
    }

    /** The color's version of a placeholder material, or null if the material is no placeholder or has none. */
    public @Nullable Material teamMaterial(Material material, String colorName) {
        String name = material.name();
        if (NOT_SWAPPED.stream().anyMatch(name::contains)) {
            return null;
        }
        if (name.startsWith(DYE_PLACEHOLDER)) {
            return find(colorName.toUpperCase(Locale.ROOT) + "_" + name.substring(DYE_PLACEHOLDER.length()));
        }
        String stripped = name.startsWith(STRIPPED) ? STRIPPED : "";
        String rest = name.substring(stripped.length());
        String wood = woods.get(colorName.toLowerCase(Locale.ROOT));
        if (!rest.startsWith(WOOD_PLACEHOLDER) || wood == null) {
            return null;
        }
        String part = rest.substring(WOOD_PLACEHOLDER.length());
        Material same = find(stripped + wood + "_" + part);
        if (same != null || !(part.equals("LOG") || part.equals("WOOD"))) {
            return same;
        }
        // Crimson and warped have stems and hyphae, bamboo has blocks instead of logs and wood
        Material nether = find(stripped + wood + "_" + (part.equals("LOG") ? "STEM" : "HYPHAE"));
        return nether != null ? nether : find(stripped + wood + "_BLOCK");
    }

    private static @Nullable Material find(String name) {
        Material material = Material.matchMaterial(name);
        return material != null && material.isBlock() ? material : null;
    }
}
