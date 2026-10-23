package me.sirsz.cloudewars.cosmetics;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.trim.TrimPattern;

/** /bw trim &lt;helmet|chestplate|leggings|boots&gt; &lt;trim|none&gt;: picks the trim pattern of one armor piece. */
public final class TrimCommand extends SubCommand {

    private static final String NONE = "none";

    @Override
    public String getName() {
        return "trim";
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
    public void perform(Player player, String[] args, IArena arena) {
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
            CloudeWars.send(player, "trim-set", "piece", piece.id(), "trim", args[2].toLowerCase());
        }
        trims.refresh(player);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        ArmorTrims trims = CloudeWars.trims();
        if (args.length == 2) {
            return Arrays.stream(ArmorPiece.values()).map(ArmorPiece::id).toList();
        }
        if (args.length == 3 && trims != null) {
            List<String> names = new ArrayList<>(trims.patternNames());
            names.add(NONE);
            return names;
        }
        return null;
    }
}
