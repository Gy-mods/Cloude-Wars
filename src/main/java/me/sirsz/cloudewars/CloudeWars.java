package me.sirsz.cloudewars;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import me.sirsz.cloudewars.proxy.ProxyMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Cloude Wars' own features. New Bedwars' code calls in here from a few hook points, so its files stay close
 * to upstream and New Bedwars updates merge cleanly.
 */
public final class CloudeWars {

    private static final String CONFIG_FILE = "cloudewars.yml";

    private static ProxyMode proxyMode;

    private CloudeWars() {
    }

    /**
     * Hook at the end of {@code NewBedwars.reloadAll()}: runs on enable and on /bw reload, after New Bedwars
     * unregistered every listener of the plugin.
     */
    public static void reload(JavaPlugin plugin) {
        YamlConfiguration config = loadConfig(plugin);
        proxyMode = new ProxyMode(plugin, config);
        proxyMode.enable();
    }

    /** Hook in {@code Arena.leave()}, right after New Bedwars moved the player to this server's lobby location. */
    public static void onArenaLeave(Player player) {
        if (proxyMode != null) {
            proxyMode.onArenaLeave(player);
        }
    }

    /** cloudewars.yml from the data folder; keys missing there fall back to the copy in the jar. */
    private static YamlConfiguration loadConfig(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), CONFIG_FILE);
        if (!file.exists()) {
            plugin.saveResource(CONFIG_FILE, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        InputStream bundled = plugin.getResource(CONFIG_FILE);
        if (bundled != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(bundled, StandardCharsets.UTF_8)));
        }
        return config;
    }
}
