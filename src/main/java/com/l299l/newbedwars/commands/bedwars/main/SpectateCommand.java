package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.game.guis.SpectateSelectGUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;

public class SpectateCommand extends SubCommand {
    private final Messages msg;

    public SpectateCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override public String getName() { return "spectate"; }
    @Override public String getDescription() { return "Join a running arena as spectator."; }
    @Override public String getSyntax() { return "/bw spectate <arena>"; }
    @Override public String getExample() { return "/bw spectate myArena"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length < 2) {
            player.openInventory(new SpectateSelectGUI(player).getInventory());
            return;
        }
        String arenaName = args[1];
        IArena target = Arena.arenaByName.get(arenaName);
        if (target == null) {
            msg.send(player, "ArenaNotExists");
            return;
        }
        if (!target.isEnabled()) {
            msg.send(player, "ArenaNotEnabled", new HashMap<>() {{
                put("/arenaname/", target.getArenaName());
            }});
            return;
        }
        if (!target.joinAsSpectator(player)) {
            if (target.status() != GameStatus.playing && target.status() != GameStatus.ending) {
                msg.send(player, "ArenaNotRunningForSpectate");
            } else if (target.getGamerules() == null || !target.getGamerules().AllowSpectators) {
                msg.send(player, "SpectatingDisabled");
            } else {
                msg.send(player, "SpectateFailed");
            }
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (IArena a : Arena.arenaByName.values()) {
                if ((a.status() == GameStatus.playing || a.status() == GameStatus.ending)
                        && a.getGamerules() != null && a.getGamerules().AllowSpectators) {
                    names.add(a.getArenaName());
                }
            }
            return names;
        }
        return null;
    }
}
