package me.sirsz.cloudewars.cosmetics;

import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import me.sirsz.cloudewars.TeamColors;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

/**
 * Armor trim cosmetics: every player picks a trim pattern per armor piece (/bw trim), and the trim material comes
 * from their team's color. The choices are saved in trims.yml in the data folder.
 */
@SuppressWarnings("deprecation") // New Bedwars' team colors are org.bukkit.ChatColor
public final class ArmorTrims {

    private static final String SAVE_FILE = "trims.yml";

    private final JavaPlugin plugin;
    private final File saveFile;
    private final Registry<TrimPattern> patterns = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN);
    private final Map<ChatColor, TrimMaterial> materials = new EnumMap<>(ChatColor.class);
    private final Map<UUID, Map<ArmorPiece, TrimPattern>> choices = new HashMap<>();

    public ArmorTrims(JavaPlugin plugin, @Nullable ConfigurationSection materialsConfig) {
        this.plugin = plugin;
        this.saveFile = new File(plugin.getDataFolder(), SAVE_FILE);
        loadMaterials(materialsConfig);
        loadChoices();
    }

    /** The pattern with this name ("sentry" or "minecraft:sentry"), or null if there is none. */
    public @Nullable TrimPattern pattern(String name) {
        NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
        return key == null ? null : patterns.get(key);
    }

    /** Every pattern's name, as players type it. */
    public List<String> patternNames() {
        return StreamSupport.stream(patterns.spliterator(), false).map(ArmorTrims::name).sorted().toList();
    }

    /** Sets the player's pattern for one armor piece (null removes it) and saves the choices. */
    public void setTrim(UUID player, ArmorPiece piece, @Nullable TrimPattern pattern) {
        Map<ArmorPiece, TrimPattern> chosen = choices.computeIfAbsent(player, id -> new EnumMap<>(ArmorPiece.class));
        if (pattern == null) {
            chosen.remove(piece);
        } else {
            chosen.put(piece, pattern);
        }
        saveChoices();
    }

    /**
     * Puts the player's trims on armor in Bukkit's order (boots first), in the team color's material. Pieces without
     * a chosen pattern, or a team color without a material, get no trim.
     */
    public ItemStack[] decorate(Player player, @Nullable ChatColor teamColor, ItemStack[] armor) {
        TrimMaterial material = teamColor == null ? null : materials.get(teamColor);
        Map<ArmorPiece, TrimPattern> chosen = choices.getOrDefault(player.getUniqueId(), Map.of());
        ArmorPiece[] pieces = ArmorPiece.values();
        for (int i = 0; i < Math.min(armor.length, pieces.length); i++) {
            ItemStack item = armor[i];
            if (item == null || !(item.getItemMeta() instanceof ArmorMeta meta)) {
                continue;
            }
            TrimPattern pattern = chosen.get(pieces[i]);
            meta.setTrim(material != null && pattern != null ? new ArmorTrim(material, pattern) : null);
            item.setItemMeta(meta);
        }
        return armor;
    }

    /** Updates the armor the player wears right now, if they are playing a game. */
    public void refresh(Player player) {
        IArena arena = Arena.arenaByWorld.get(player.getWorld());
        if (arena == null || arena.status() != GameStatus.playing || !arena.isPlayerInArena(player)) {
            return;
        }
        Team team = arena.getTeam(player);
        if (team != null) {
            player.getInventory().setArmorContents(decorate(player, team.getColor(), player.getInventory().getArmorContents()));
        }
    }

    private void loadMaterials(@Nullable ConfigurationSection config) {
        if (config == null) {
            return;
        }
        Registry<TrimMaterial> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL);
        for (String colorName : config.getKeys(false)) {
            String materialName = config.getString(colorName, "");
            NamespacedKey key = NamespacedKey.fromString(materialName.toLowerCase(Locale.ROOT));
            TrimMaterial material = key == null ? null : registry.get(key);
            ChatColor color = TeamColors.fromName(colorName);
            if (color == null || material == null) {
                plugin.getLogger().warning("cloudewars.yml trims.materials: skipping '" + colorName + ": " + materialName
                        + "' (unknown team color or trim material)");
                continue;
            }
            materials.put(color, material);
        }
    }

    private void loadChoices() {
        if (!saveFile.exists()) {
            return;
        }
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(saveFile);
        for (String playerId : saved.getKeys(false)) {
            ConfigurationSection pieces = saved.getConfigurationSection(playerId);
            UUID uuid = parseUuid(playerId);
            if (pieces == null || uuid == null) {
                continue;
            }
            Map<ArmorPiece, TrimPattern> chosen = new EnumMap<>(ArmorPiece.class);
            for (String pieceId : pieces.getKeys(false)) {
                ArmorPiece piece = ArmorPiece.byId(pieceId);
                TrimPattern pattern = pattern(pieces.getString(pieceId, ""));
                if (piece != null && pattern != null) {
                    chosen.put(piece, pattern);
                }
            }
            choices.put(uuid, chosen);
        }
    }

    private void saveChoices() {
        YamlConfiguration saved = new YamlConfiguration();
        choices.forEach((uuid, chosen) -> chosen.forEach((piece, pattern) -> saved.set(uuid + "." + piece.id(), name(pattern))));
        try {
            saved.save(saveFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + SAVE_FILE + ": " + e.getMessage());
        }
    }

    private static String name(TrimPattern pattern) {
        NamespacedKey key = pattern.getKey();
        return key.getNamespace().equals(NamespacedKey.MINECRAFT) ? key.getKey() : key.asString();
    }

    private static @Nullable UUID parseUuid(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
