package me.sirsz.cloudewars.mapmaker;

import com.l299l.newbedwars.api.command.AddonCommand;
import com.l299l.newbedwars.api.view.ArenaView;
import java.util.List;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.entity.Player;

/** /bw syncSymmetry (setup mode): copies the build part (quarter or half) to the rest of the map. */
public final class SyncSymmetryCommand implements AddonCommand {

    @Override
    public String getName() {
        return "syncSymmetry";
    }

    @Override
    public String getDescription() {
        return "Copy your build quarter or half to the rest of the map, turned or mirrored the right way.";
    }

    @Override
    public String getSyntax() {
        return "/bw syncSymmetry";
    }

    @Override
    public String getExample() {
        return "/bw syncSymmetry";
    }

    @Override
    public void perform(Player player, String[] args, ArenaView arena) {
        BuildSymmetry symmetry = CloudeWars.symmetry();
        if (symmetry == null) {
            CloudeWars.send(player, "symmetry-none");
            return;
        }
        symmetry.sync(player, arena);
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return List.of();
    }
}
