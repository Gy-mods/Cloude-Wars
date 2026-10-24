package me.sirsz.cloudewars.mapmaker;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import java.util.List;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.entity.Player;

/** /bw setBuildSymmetry quarter nomiddle [size] (setup mode): build one quarter, /bw syncSymmetry copies it. */
public final class SetBuildSymmetryCommand extends SubCommand {

    private static final String QUARTER = "quarter";
    private static final String NO_MIDDLE = "nomiddle";

    @Override
    public String getName() {
        return "setBuildSymmetry";
    }

    @Override
    public String getDescription() {
        return "Build one quarter of the map (the one you stand in); /bw syncSymmetry copies it to the other three.";
    }

    @Override
    public String getSyntax() {
        return "/bw setBuildSymmetry quarter nomiddle [size]";
    }

    @Override
    public String getExample() {
        return "/bw setBuildSymmetry quarter nomiddle 60";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        BuildSymmetry symmetry = CloudeWars.symmetry();
        if (symmetry == null || args.length < 3 || args.length > 4) {
            CloudeWars.send(player, "symmetry-usage");
            return;
        }
        if (!args[1].equalsIgnoreCase(QUARTER) || !args[2].equalsIgnoreCase(NO_MIDDLE)) {
            CloudeWars.send(player, "symmetry-not-available");
            return;
        }
        int size = symmetry.defaultSize();
        if (args.length == 4) {
            try {
                size = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                size = 0;
            }
            if (size < 1 || size > BuildSymmetry.MAX_SIZE) {
                CloudeWars.send(player, "symmetry-bad-size", "max", String.valueOf(BuildSymmetry.MAX_SIZE));
                return;
            }
        }
        symmetry.setQuarters(player, arena, size);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        BuildSymmetry symmetry = CloudeWars.symmetry();
        return switch (args.length) {
            case 2 -> List.of(QUARTER);
            case 3 -> List.of(NO_MIDDLE);
            case 4 -> symmetry == null ? null : List.of(String.valueOf(symmetry.defaultSize()));
            default -> null;
        };
    }
}
