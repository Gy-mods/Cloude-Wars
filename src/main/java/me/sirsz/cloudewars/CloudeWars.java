package me.sirsz.cloudewars;

import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.arena.shops.customitems.PriceType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.OnBuyEvent;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import me.sirsz.cloudewars.cosmetics.ArmorTrims;
import me.sirsz.cloudewars.cosmetics.TrimCommand;
import me.sirsz.cloudewars.items.SnowGolemLogic;
import me.sirsz.cloudewars.items.SnowGolems;
import me.sirsz.cloudewars.proxy.ProxyMode;
import me.sirsz.cloudewars.teams.TeamBlockMarks;
import me.sirsz.cloudewars.teams.TeamBlocks;
import me.sirsz.cloudewars.teams.TeamColorRandomizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

/**
 * Cloude Wars' own features. New Bedwars' code calls in here from a few hook points, so its files stay close
 * to upstream and New Bedwars updates merge cleanly.
 */
public final class CloudeWars {

    private static final String CONFIG_FILE = "cloudewars.yml";

    private static YamlConfiguration config = new YamlConfiguration();
    private static @Nullable ProxyMode proxyMode;
    private static @Nullable ArmorTrims trims;
    private static @Nullable TeamBlocks teamBlocks;
    private static @Nullable TeamBlockMarks teamBlockMarks;
    private static @Nullable TeamColorRandomizer randomizer;
    private static @Nullable SnowGolems snowGolems;
    private static List<String> teamColorNames = List.of();
    private static Set<ChatColor> teamColors = EnumSet.noneOf(ChatColor.class);
    /** The team-colors name of each team color, e.g. GOLD -> "orange". */
    private static Map<ChatColor, String> colorNamesByColor = new EnumMap<>(ChatColor.class);

    private CloudeWars() {
    }

    /**
     * Hook at the end of {@code NewBedwars.reloadAll()}: runs on enable and on /bw reload, after New Bedwars
     * unregistered every listener of the plugin.
     */
    public static void reload(JavaPlugin plugin) {
        config = loadConfig(plugin);
        loadTeamColors(plugin);
        randomizer = new TeamColorRandomizer(plugin, config.getBoolean("random-team-colors"), teamColorNames);
        teamBlocks = new TeamBlocks(plugin, section("team-wood"));
        teamBlockMarks = new TeamBlockMarks(plugin, teamBlocks, loadTeamNumbers(plugin));
        plugin.getServer().getPluginManager().registerEvents(teamBlockMarks, plugin);
        trims = new ArmorTrims(plugin, section("trims.materials"));
        snowGolems = new SnowGolems(plugin, config);
        plugin.getServer().getPluginManager().registerEvents(snowGolems, plugin);
        proxyMode = new ProxyMode(plugin, config);
        proxyMode.enable();
    }

    /**
     * Hook at the end of {@code ItemsDataManager.load()}: adds Cloude Wars' shop items that the item list does not
     * have yet (data/items.yml of an existing server), so they can be put into the shop pages.
     */
    public static void addItems(CustomItemManager items) {
        if (items.getCustomItem(SnowGolems.ITEM_NAME) == null) {
            items.createCustomItem(SnowGolems.ITEM_NAME, Material.SNOW_GOLEM_SPAWN_EGG, 5, PriceType.GOLD, 1,
                    new SnowGolemLogic(), OnBuyEvent.GIVE_ITEM);
        }
    }

    public static @Nullable SnowGolems snowGolems() {
        return snowGolems;
    }

    /** Hook in {@code MainCommand.setSubCommands()}: adds Cloude Wars' /bw subcommands for players. */
    public static void addPlayerCommands(List<SubCommand> lobbyCommands, List<SubCommand> gameCommands) {
        TrimCommand trim = new TrimCommand();
        lobbyCommands.add(trim);
        gameCommands.add(trim);
    }

    /**
     * Hook in {@code Arena.start()}, before the teams start: gives the teams their random colors for this round,
     * then turns the map's team blocks into their teams' versions.
     */
    public static void onGameStart(IArena arena) {
        if (randomizer != null) {
            randomizer.randomize(arena);
        }
        if (teamBlockMarks != null) {
            teamBlockMarks.recolor(arena);
        }
    }

    /** Hook at the end of {@code Arena.disable()}: setup shows the teams with their saved colors again. */
    public static void onArenaDisabled(IArena arena) {
        if (randomizer != null) {
            randomizer.clear(arena);
        }
    }

    /** The team-colors name of a team color ("orange" for GOLD), or null if it is none of them. */
    @SuppressWarnings("deprecation")
    public static @Nullable String colorName(ChatColor color) {
        return colorNamesByColor.get(color);
    }

    /** Hook in {@code Arena.leave()}, right after New Bedwars moved the player to this server's lobby location. */
    public static void onArenaLeave(Player player) {
        if (proxyMode != null) {
            proxyMode.onArenaLeave(player);
        }
    }

    /** Hook in {@code ArmorContents.loadPlayerArmorContents()}: decorates the armor New Bedwars puts on a player. */
    @SuppressWarnings("deprecation") // New Bedwars' team colors are org.bukkit.ChatColor
    public static ItemStack[] decorateArmor(Player player, ChatColor teamColor, ItemStack[] armor) {
        return trims == null ? armor : trims.decorate(player, teamColor, armor);
    }

    /**
     * Hook in {@code CustomItem.getItem()} and {@code getIcon(Player)}: the team version of a shop block for the
     * player's team (oak planks become the team's planks), or the block itself outside a team.
     */
    public static Material teamMaterial(Player player, Material material) {
        IArena arena = Arena.arenaByWorld.get(player.getWorld());
        Team team = arena == null ? null : arena.getTeam(player);
        return teamBlocks == null || team == null ? material : teamBlocks.forTeam(material, team.getColor());
    }

    /**
     * Hook in {@code CreateTeamCommand.perform()}: whether a new team may get this color. If not, tells the player
     * which colors are supported.
     */
    @SuppressWarnings("deprecation")
    public static boolean checkTeamColor(Player player, ChatColor color) {
        if (teamColors.isEmpty() || teamColors.contains(color)) {
            return true;
        }
        send(player, "team-color-not-allowed", "colors", String.join(", ", teamColorNames));
        return false;
    }

    /** Hook in {@code CreateTeamCommand.getSubcommandArguments()}: the colors /bw createTeam suggests. */
    public static List<String> teamColorNames() {
        return teamColorNames.stream().map(name -> name.toUpperCase(Locale.ROOT)).toList();
    }

    public static @Nullable ArmorTrims trims() {
        return trims;
    }

    /** Sends messages.&lt;key&gt; from cloudewars.yml; replacements are name, value pairs for {name} placeholders. */
    public static void send(CommandSender to, String key, String... replacements) {
        to.sendMessage(message(key, replacements));
    }

    /** Like {@link #send}, but shown above the hotbar instead of in chat. */
    public static void actionBar(Player to, String key, String... replacements) {
        to.sendActionBar(message(key, replacements));
    }

    private static Component message(String key, String... replacements) {
        String text = config.getString("messages." + key); // without a fallback argument, the jar's default is used
        if (text == null) {
            text = key;
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /** team-numbers: the color names of team 1, 2, ... (only names that can color blocks). */
    private static List<String> loadTeamNumbers(JavaPlugin plugin) {
        List<String> colors = new ArrayList<>();
        for (String name : config.getStringList("team-numbers")) {
            if (!TeamBlocks.isDyeColor(name)) {
                plugin.getLogger().warning("cloudewars.yml team-numbers: '" + name + "' is no block color; team "
                        + (colors.size() + 1) + " gets no team blocks");
            }
            colors.add(name.toLowerCase(Locale.ROOT));
        }
        return List.copyOf(colors);
    }

    @SuppressWarnings("deprecation")
    private static void loadTeamColors(JavaPlugin plugin) {
        List<String> names = new ArrayList<>();
        Set<ChatColor> colors = EnumSet.noneOf(ChatColor.class);
        Map<ChatColor, String> namesByColor = new EnumMap<>(ChatColor.class);
        for (String name : config.getStringList("team-colors")) {
            ChatColor color = TeamColors.fromName(name);
            if (color == null) {
                plugin.getLogger().warning("cloudewars.yml team-colors: skipping unknown color '" + name + "'");
                continue;
            }
            names.add(name);
            colors.add(color);
            namesByColor.put(color, name.toLowerCase(Locale.ROOT));
        }
        teamColorNames = List.copyOf(names);
        teamColors = colors;
        colorNamesByColor = namesByColor;
    }

    /** A config section, from the jar's defaults when the server's cloudewars.yml does not have it yet. */
    private static @Nullable ConfigurationSection section(String path) {
        return config.get(path) instanceof ConfigurationSection section ? section : null;
    }

    /** cloudewars.yml from the data folder; keys missing there fall back to the copy in the jar. */
    private static YamlConfiguration loadConfig(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), CONFIG_FILE);
        if (!file.exists()) {
            plugin.saveResource(CONFIG_FILE, false);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        InputStream bundled = plugin.getResource(CONFIG_FILE);
        if (bundled != null) {
            loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(bundled, StandardCharsets.UTF_8)));
        }
        return loaded;
    }
}
