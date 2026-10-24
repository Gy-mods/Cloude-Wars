package me.sirsz.cloudewars.mapmaker;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import java.util.List;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.entity.Player;

/**
 * /bw setBuildSymmetry quarter nomiddle [size] or /bw setBuildSymmetry half nomiddle &lt;mirror|central&gt; [size]
 * (setup mode): build one part of the map, /bw syncSymmetry copies it.
 */
public final class SetBuildSymmetryCommand extends SubCommand {

    private static final String QUARTER = "quarter";
    private static final String HALF = "half";
    private static final String NO_MIDDLE = "nomiddle";
    private static final String MIRROR = "mirror";
    private static final String CENTRAL = "central";

    @Override
    public String getName() {
        return "setBuildSymmetry";
    }

    @Override
    public String getDescription() {
        return "Build one quarter or half of the map (the one you stand in); /bw syncSymmetry copies it to the rest.";
    }

    @Override
    public String getSyntax() {
        return "/bw setBuildSymmetry <quarter nomiddle|half nomiddle <mirror|central>> [size]";
    }

    @Override
    public String getExample() {
        return "/bw setBuildSymmetry half nomiddle central 60";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        BuildSymmetry symmetry = CloudeWars.symmetry();
        if (symmetry == null || args.length < 3) {
            CloudeWars.send(player, "symmetry-usage");
            return;
        }
        boolean half = args[1].equalsIgnoreCase(HALF);
        if (!(half || args[1].equalsIgnoreCase(QUARTER)) || !args[2].equalsIgnoreCase(NO_MIDDLE)) {
            CloudeWars.send(player, "symmetry-not-available");
            return;
        }
        int sizeArgument = half ? 4 : 3;
        if (half && (args.length < 4 || !(args[3].equalsIgnoreCase(MIRROR) || args[3].equalsIgnoreCase(CENTRAL)))) {
            CloudeWars.send(player, "symmetry-half-type");
            return;
        }
        if (args.length > sizeArgument + 1) {
            CloudeWars.send(player, "symmetry-usage");
            return;
        }
        int size = symmetry.defaultSize();
        if (args.length == sizeArgument + 1) {
            try {
                size = Integer.parseInt(args[sizeArgument]);
            } catch (NumberFormatException e) {
                size = 0;
            }
            if (size < 1 || size > BuildSymmetry.MAX_SIZE) {
                CloudeWars.send(player, "symmetry-bad-size", "max", String.valueOf(BuildSymmetry.MAX_SIZE));
                return;
            }
        }
        if (half) {
            symmetry.setHalves(player, arena, args[3].equalsIgnoreCase(MIRROR), size);
        } else {
            symmetry.setQuarters(player, arena, size);
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        BuildSymmetry symmetry = CloudeWars.symmetry();
        String size = symmetry == null ? "" : String.valueOf(symmetry.defaultSize());
        boolean half = args.length > 1 && args[1].equalsIgnoreCase(HALF);
        return switch (args.length) {
            case 2 -> List.of(QUARTER, HALF);
            case 3 -> List.of(NO_MIDDLE);
            case 4 -> half ? List.of(MIRROR, CENTRAL) : List.of(size);
            case 5 -> half ? List.of(size) : null;
            default -> null;
        };
    }
}
