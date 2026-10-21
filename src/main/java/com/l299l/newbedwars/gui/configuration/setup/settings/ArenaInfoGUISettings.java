package com.l299l.newbedwars.gui.configuration.setup.settings;

import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.gui.configuration.setup.guis.ArenaInfoGUI;
import com.l299l.newbedwars.gui.configuration.setup.guis.ArenaInfoTeamGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ArenaInfoGUISettings implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        try {
            if (e.getClickedInventory().getHolder() instanceof ArenaInfoGUI gui) {
                e.setCancelled(true);
                Team team = gui.getTeam(e.getSlot());
                if (team != null) {
                    // Open team detail GUI
                    player.openInventory(new ArenaInfoTeamGUI(gui.getArena(), team).getInventory());
                } else if (e.getSlot() == 35) {
                    // Navigation button - go previous if on last page, otherwise go next
                    if (gui.getPage() > 0) {
                        player.openInventory(new ArenaInfoGUI(gui.getArena(), gui.getPage() - 1).getInventory());
                    } else if (gui.getPage() < gui.getTotalPages() - 1) {
                        player.openInventory(new ArenaInfoGUI(gui.getArena(), gui.getPage() + 1).getInventory());
                    }
                }
                // If not a team item or navigation, just keep the GUI open (do nothing)
            } else if (e.getClickedInventory().getHolder() instanceof ArenaInfoTeamGUI gui) {
                e.setCancelled(true);
                // Back button or any click returns to main GUI
                player.openInventory(new ArenaInfoGUI(gui.getArena()).getInventory());
            }
        } catch (Exception exception) {
            e.setCancelled(true);
        }
    }
}
