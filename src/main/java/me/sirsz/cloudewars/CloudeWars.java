package me.sirsz.cloudewars;

import com.l299l.newbedwars.commands.bedwars.SubCommand;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import me.sirsz.cloudewars.cosmetics.ArmorTrims;
import me.sirsz.cloudewars.cosmetics.TrimCommand;
import me.sirsz.cloudewars.proxy.ProxyMode;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
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

    private CloudeWars() {
    }

    /**
     * Hook at the end of {@code NewBedwars.reloadAll()}: runs on enable and on /bw reload, after New Bedwars
     * unregistered every listener of the plugin.
     */
    public static void reload(JavaPlugin plugin) {
        config = loadConfig(plugin);
        trims = new ArmorTrims(plugin, section("trims.materials"));
        proxyMode = new ProxyMode(plugin, config);
        proxyMode.enable();
    }

    /** Hook in {@code MainCommand.setSubCommands()}: adds Cloude Wars' /bw subcommands for players. */
    public static void addPlayerCommands(List<SubCommand> lobbyCommands, List<SubCommand> gameCommands) {
        TrimCommand trim = new TrimCommand();
        lobbyCommands.add(trim);
        gameCommands.add(trim);
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

    public static @Nullable ArmorTrims trims() {
        return trims;
    }

    /** Sends messages.&lt;key&gt; from cloudewars.yml; replacements are name, value pairs for {name} placeholders. */
    public static void send(CommandSender to, String key, String... replacements) {
        String text = config.getString("messages." + key); // without a fallback argument, the jar's default is used
        if (text == null) {
            text = key;
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        to.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
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
