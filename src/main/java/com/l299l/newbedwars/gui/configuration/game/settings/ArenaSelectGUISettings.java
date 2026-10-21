package com.l299l.newbedwars.gui.configuration.game.settings;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.game.guis.ArenaSelectGUI;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.parties.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.List;

public class ArenaSelectGUISettings implements Listener {
    private final Messages msg;

    public ArenaSelectGUISettings() {
        msg = NewBedwars.plugin.getMessages();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        if (!(e.getInventory().getHolder() instanceof ArenaSelectGUI gui)) {
            return;
        }
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) {
            return;
        }
        IArena target = gui.getArena(e.getRawSlot());
        if (target == null) {
            return;
        }
        p.closeInventory();

        PartyManager pm = NewBedwars.plugin.getPartyManager();
        Party party = pm.getParty(p);
        if (party != null && !party.isAdmin(p.getUniqueId())) {
            Player admin = Bukkit.getPlayer(party.getAdmin());
            String adminName = admin != null ? admin.getName() : "?";
            msg.send(p, "PartyOnlyAdminCanJoin", new HashMap<>() {{ put("/player/", adminName); }});
            return;
        }

        if (target.isPlayerInArena(p)) {
            if (target.rejoin(p)) {
                msg.send(p, "Rejoined");
            } else {
                msg.send(p, "RejoinFailed");
            }
            return;
        }

        GameStatus status = target.status();
        if (status != GameStatus.waiting && status != GameStatus.starting) {
            if (status == GameStatus.playing) {
                msg.send(p, "ArenaRunningError");
            } else if (status == GameStatus.ending) {
                msg.send(p, "ArenaEndingError");
            } else {
                msg.send(p, "ArenaJoinError");
            }
            return;
        }

        if (party != null && target.getGamerules().AllowParties) {
            List<Player> members = pm.getOnlineMembers(party);
            if (members.size() > target.getMaxInTeam()) {
                msg.send(p, "PartyTooBig", new HashMap<>() {{ put("/maxteam/", String.valueOf(target.getMaxInTeam())); }});
                return;
            }
            if (!target.joinParty(members)) {
                msg.send(p, "ArenaIsFullError");
            }
        } else if (party != null && !target.getGamerules().AllowParties) {
            msg.send(p, "PartyNotAllowedInArena");
        } else {
            if (!target.join(p)) {
                msg.send(p, "ArenaIsFullError");
            }
        }
    }
}
