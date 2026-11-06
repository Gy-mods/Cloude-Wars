package me.sirsz.cloudewars.cosmetics;

import com.l299l.newbedwars.api.command.AddonCommand;
import com.l299l.newbedwars.api.view.ArenaView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;

/** /bw trim &lt;helmet|chestplate|leggings|boots&gt; &lt;trim|none&gt;: picks the trim pattern of one armor piece. */
public final class TrimCommand implements AddonCommand {

    private static final String NAME = "trim";
    private static final String NONE = "none";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Choose the armor trim pattern of each armor piece (the material comes from your team color).";
    }

    @Override
    public String getSyntax() {
        return "/bw trim <helmet|chestplate|leggings|boots> <trim|none>";
    }

    @Override
    public String getExample() {
        return "/bw trim chestplate sentry";
    }

    @Override
    public void perform(Player player, String[] args, ArenaView arena) {
        ArmorTrims trims = CloudeWars.trims();
        ArmorPiece piece = args.length == 3 ? ArmorPiece.byId(args[1]) : null;
        if (trims == null || piece == null) {
            CloudeWars.send(player, "trim-usage");
            return;
        }
        if (args[2].equalsIgnoreCase(NONE)) {
            trims.setTrim(player.getUniqueId(), piece, null);
            CloudeWars.send(player, "trim-removed", "piece", piece.id());
        } else {
            TrimPattern pattern = trims.pattern(args[2]);
            if (pattern == null) {
                CloudeWars.send(player, "trim-unknown", "trim", args[2], "trims", String.join(", ", trims.patternNames()));
                return;
            }
            trims.setTrim(player.getUniqueId(), piece, pattern);
            CloudeWars.send(player, "trim-set", "piece", piece.id(), "trim", args[2].toLowerCase(Locale.ROOT));
        }
        trims.refresh(player);
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        ArmorTrims trims = CloudeWars.trims();
        if (args.length == 2) {
            return Arrays.stream(ArmorPiece.values()).map(ArmorPiece::id).toList();
        }
        if (args.length == 3 && trims != null) {
            List<String> names = new ArrayList<>(trims.patternNames());
            names.add(NONE);
            return names;
        }
        return List.of();
    }

    /**
     * /bw trim outside arenas. New Bedwars lets a command name be in one place only, so trim is its in-game command
     * and this listener answers it (and tab-completes it) in the lobby.
     */
    public static final class LobbyCommand implements Listener {

        private final TrimCommand command = new TrimCommand();

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onCommand(PlayerCommandPreprocessEvent event) {
            String[] args = bwArgs(event.getMessage().trim());
            if (args != null && args.length > 0 && args[0].equalsIgnoreCase(NAME) && inLobby(event.getPlayer())) {
                event.setCancelled(true);
                command.perform(event.getPlayer(), args, null);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onTabComplete(TabCompleteEvent event) {
            String[] args = bwArgs(event.getBuffer());
            if (args == null || args.length == 0 || !(event.getSender() instanceof Player player) || !inLobby(player)) {
                return;
            }
            String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
            if (args.length == 1) {
                if (NAME.startsWith(typed) && !event.getCompletions().contains(NAME)) {
                    List<String> completions = new ArrayList<>(event.getCompletions());
                    completions.add(NAME);
                    event.setCompletions(completions);
                }
            } else if (args[0].equalsIgnoreCase(NAME)) {
                event.setCompletions(command.tabComplete(player, args).stream()
                        .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList());
            }
        }

        private static boolean inLobby(Player player) {
            return CloudeWars.api().getArena(player).isEmpty();
        }

        /** The arguments of a /bw command line ("" as last one when it ends with a space), or null for other commands. */
        private static String[] bwArgs(String line) {
            String[] words = line.split(" ", -1);
            String label = words[0].toLowerCase(Locale.ROOT);
            if (!label.equals("/bw") && !label.equals("/newbedwars:bw")) {
                return null;
            }
            return Arrays.copyOfRange(words, 1, words.length);
        }
    }
}
