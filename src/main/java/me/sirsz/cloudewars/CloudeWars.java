package me.sirsz.cloudewars;

import com.l299l.newbedwars.api.NewBedwarsAPI;
import com.l299l.newbedwars.api.command.CommandContext;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.sirsz.cloudewars.cosmetics.ArmorTrims;
import me.sirsz.cloudewars.cosmetics.TrimCommand;
import me.sirsz.cloudewars.items.SnowGolemLogic;
import me.sirsz.cloudewars.items.SnowGolems;
import me.sirsz.cloudewars.mapmaker.BuildSymmetry;
import me.sirsz.cloudewars.mapmaker.SetBuildSymmetryCommand;
import me.sirsz.cloudewars.mapmaker.SyncSymmetryCommand;
import me.sirsz.cloudewars.teams.GameListener;
import me.sirsz.cloudewars.teams.TeamBlockMarks;
import me.sirsz.cloudewars.teams.TeamBlocks;
import me.sirsz.cloudewars.teams.TeamColorRandomizer;
import me.sirsz.cloudewars.teams.TeamWood;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

/**
 * Cloude Wars, an addon for New Bedwars: armor trims, team wood, the map maker (team blocks, symmetry), random team
 * colors and the snow golem. It only uses New Bedwars' addon API ({@code com.l299l.newbedwars.api}).
 */
public final class CloudeWars extends JavaPlugin {

    private static final String CONFIG_FILE = "cloudewars.yml";
    /** The shop logic name of the snow golem egg ({@code itemLogic} in New Bedwars' data/items.yml). */
    public static final String SNOW_GOLEM_LOGIC = "SNOW_GOLEM";

    private static @Nullable CloudeWars plugin;
    private static YamlConfiguration config = new YamlConfiguration();
    private static @Nullable ArmorTrims trims;
    private static @Nullable TeamBlocks teamBlocks;
    private static @Nullable TeamBlockMarks teamBlockMarks;
    private static @Nullable TeamColorRandomizer randomizer;
    private static @Nullable SnowGolems snowGolems;
    private static @Nullable BuildSymmetry symmetry;

    @Override
    public void onEnable() {
        plugin = this;
        NewBedwarsAPI api = NewBedwarsAPI.get();
        reload();
        TrimCommand trim = new TrimCommand();
        api.registerCommand(this, trim, CommandContext.GAME);
        api.registerCommand(this, new SetBuildSymmetryCommand(), CommandContext.SETUP_BASIC);
        api.registerCommand(this, new SyncSymmetryCommand(), CommandContext.SETUP_BASIC);
        api.registerShopLogic(this, SNOW_GOLEM_LOGIC, new SnowGolemLogic());
        warnAboutOldData();
    }

    @Override
    public void onDisable() {
        plugin = null;
    }

    /** Loads cloudewars.yml and (re)creates the features with their listeners; also /cloudewars reload. */
    private void reload() {
        HandlerList.unregisterAll(this);
        config = loadConfig();
        teamBlocks = new TeamBlocks(this, section("team-wood"));
        teamBlockMarks = new TeamBlockMarks(this, teamBlocks, loadTeamNumbers());
        randomizer = new TeamColorRandomizer(this, config.getBoolean("random-team-colors"), loadTeamColors(),
                config.getInt("random-team-colors-bed-distance"));
        trims = new ArmorTrims(this, section("trims.materials"));
        snowGolems = new SnowGolems(this, config);
        symmetry = new BuildSymmetry(this, teamBlockMarks, config);
        for (Listener listener : List.of(teamBlockMarks, new TeamWood(this, teamBlocks), trims, new TrimCommand.LobbyCommand(),
                snowGolems, new GameListener(randomizer, teamBlockMarks))) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reload();
            send(sender, "reloaded");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("reload") : List.of();
    }

    public static CloudeWars plugin() {
        if (plugin == null) {
            throw new IllegalStateException("Cloude Wars is not enabled");
        }
        return plugin;
    }

    public static NewBedwarsAPI api() {
        return NewBedwarsAPI.get();
    }

    public static @Nullable ArmorTrims trims() {
        return trims;
    }

    public static @Nullable SnowGolems snowGolems() {
        return snowGolems;
    }

    public static @Nullable BuildSymmetry symmetry() {
        return symmetry;
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
    private List<String> loadTeamNumbers() {
        List<String> colors = new ArrayList<>();
        for (String name : config.getStringList("team-numbers")) {
            if (!TeamBlocks.isDyeColor(name)) {
                getLogger().warning("cloudewars.yml team-numbers: '" + name + "' is no block color; team "
                        + (colors.size() + 1) + " gets no team blocks");
            }
            colors.add(name.toLowerCase(Locale.ROOT));
        }
        return List.copyOf(colors);
    }

    /** team-colors: the colors the random team colors pick from. */
    private List<String> loadTeamColors() {
        List<String> names = new ArrayList<>();
        for (String name : config.getStringList("team-colors")) {
            if (TeamColors.fromName(name) == null) {
                getLogger().warning("cloudewars.yml team-colors: skipping unknown color '" + name + "'");
                continue;
            }
            names.add(name.toLowerCase(Locale.ROOT));
        }
        return List.copyOf(names);
    }

    /** A config section, from the jar's defaults when the server's cloudewars.yml does not have it yet. */
    private static @Nullable ConfigurationSection section(String path) {
        return config.get(path) instanceof ConfigurationSection section ? section : null;
    }

    /** cloudewars.yml from the data folder; keys missing there fall back to the copy in the jar. */
    private YamlConfiguration loadConfig() {
        File file = new File(getDataFolder(), CONFIG_FILE);
        if (!file.exists()) {
            saveResource(CONFIG_FILE, false);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        InputStream bundled = getResource(CONFIG_FILE);
        if (bundled != null) {
            loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(bundled, StandardCharsets.UTF_8)));
        }
        return loaded;
    }

    /**
     * Cloude Wars 1.x was a whole Bedwars plugin and kept New Bedwars' files (arenas, shop, stats...) in this data
     * folder. New Bedwars reads them from plugins/NewBedwars, so tell the admin to move them.
     */
    private void warnAboutOldData() {
        File oldArenas = new File(getDataFolder(), "data/arenas");
        String[] files = oldArenas.list();
        if (files != null && files.length > 0) {
            getLogger().warning("Found Cloude Wars 1.x Bedwars data in " + getDataFolder().getPath() + ". Since 2.0 Cloude Wars"
                    + " is an addon and New Bedwars keeps its own data: stop the server and move data/, generators/, language/, config.yml,"
                    + " bossBars.yml and scoreboards.yml from plugins/CloudeWars to plugins/NewBedwars (see the README).");
        }
    }
}
