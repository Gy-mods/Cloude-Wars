package me.sirsz.cloudewars.proxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Game server mode behind a Velocity proxy: players who connect go straight into one arena, and are sent back to
 * the lobby server when they leave it (game over, leave item, /bw lobby). Admins are never moved.
 */
public final class ProxyMode implements Listener {

    public static final String ADMIN_PERMISSION = "cloudewars.proxy.admin";

    /** Velocity understands BungeeCord's plugin messages (bungee-plugin-message-channel in velocity.toml). */
    private static final String CHANNEL = "BungeeCord";
    /**
     * Wait after a player connects: New Bedwars moves players out of arena worlds one tick after joining, and the
     * proxy needs a moment to register the BungeeCord channel before it accepts a send-back.
     */
    private static final long JOIN_DELAY_TICKS = 10;

    private final JavaPlugin plugin;
    private final boolean enabled;
    private final String arenaName;
    private final String lobbyServer;

    public ProxyMode(JavaPlugin plugin, YamlConfiguration config) {
        this.plugin = plugin;
        this.enabled = config.getBoolean("proxy.enabled");
        this.arenaName = config.getString("proxy.arena");
        this.lobbyServer = config.getString("proxy.lobby-server");
    }

    public void enable() {
        if (!enabled) {
            return;
        }
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Proxy mode: players go into arena '" + arenaName + "' and back to server '"
                + lobbyServer + "'.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isAdmin(player)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> joinArena(player), JOIN_DELAY_TICKS);
    }

    private void joinArena(Player player) {
        if (!player.isOnline()) {
            return;
        }
        IArena arena = Arena.arenaByName.get(arenaName);
        if (arena == null || !arena.isEnabled()) {
            sendToLobby(player, "proxy-arena-unavailable");
            return;
        }
        // Back after a disconnect while their game is still running
        if (arena.isPlayerInArena(player) && arena.rejoin(player)) {
            return;
        }
        GameStatus status = arena.status();
        if ((status == GameStatus.waiting || status == GameStatus.starting) && arena.join(player)) {
            return;
        }
        boolean running = status == GameStatus.playing || status == GameStatus.ending;
        sendToLobby(player, running ? "proxy-game-running" : "proxy-arena-unavailable");
    }

    /** New Bedwars just moved the player out of an arena: on a game server that means back to the lobby server. */
    public void onArenaLeave(Player player) {
        // While the server shuts down, the plugin may not schedule tasks (it would abort New Bedwars' saving), and the
        // proxy moves everyone to the lobby server anyway
        if (!enabled || isAdmin(player) || !plugin.isEnabled()) {
            return;
        }
        // One tick later, so a player whose leave() came from quitting is already gone
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                connect(player, lobbyServer);
            }
        });
    }

    private boolean isAdmin(Player player) {
        return player.hasPermission(ADMIN_PERMISSION);
    }

    private void sendToLobby(Player player, String messageKey) {
        CloudeWars.send(player, messageKey);
        connect(player, lobbyServer);
    }

    private void connect(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }
}
