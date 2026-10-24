package me.sirsz.cloudewars.mapmaker;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import java.util.List;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.entity.Player;

/** /bw syncSymmetry (setup mode): copies the build quarter to the other three quarters. */
public final class SyncSymmetryCommand extends SubCommand {

    @Override
    public String getName() {
        return "syncSymmetry";
    }

    @Override
    public String getDescription() {
        return "Copy your build quarter to the other three quarters, turned to face the right way.";
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
    public void perform(Player player, String[] args, IArena arena) {
        BuildSymmetry symmetry = CloudeWars.symmetry();
        if (symmetry == null) {
            CloudeWars.send(player, "symmetry-none");
            return;
        }
        symmetry.sync(player, arena);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
