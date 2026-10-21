package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpectateSelectGUI implements InventoryHolder {
    private final Inventory inv;
    private final Map<Integer, IArena> slotToArena = new HashMap<>();

    public SpectateSelectGUI(Player player) {
        List<IArena> arenas = Arena.arenaByName.values().stream()
                .filter(IArena::isEnabled)
                .sorted((a, b) -> a.getArenaName().compareToIgnoreCase(b.getArenaName()))
                .toList();

        int size = Math.max(9, (int) Math.ceil(arenas.size() / 9.0) * 9);
        size = Math.min(size, 54);
        inv = Bukkit.createInventory(this, size, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Spectate Arena");

        for (int i = 0; i < arenas.size() && i < size; i++) {
            IArena arena = arenas.get(i);
            inv.setItem(i, buildArenaItem(arena));
            slotToArena.put(i, arena);
        }
    }

    private boolean canSpectate(IArena arena) {
        return (arena.status() == GameStatus.playing || arena.status() == GameStatus.ending)
                && arena.getGamerules() != null && arena.getGamerules().AllowSpectators;
    }

    private ItemStack buildArenaItem(IArena arena) {
        GameStatus status = arena.status();
        boolean spectatable = canSpectate(arena);

        Material mat;
        String statusLabel;
        switch (status) {
            case waiting  -> { mat = Material.LIME_WOOL;   statusLabel = ChatColor.GREEN + "Waiting"; }
            case starting -> { mat = Material.YELLOW_WOOL; statusLabel = ChatColor.YELLOW + "Starting"; }
            case playing  -> { mat = Material.RED_WOOL;    statusLabel = ChatColor.RED + "In Progress"; }
            case ending   -> { mat = Material.ORANGE_WOOL; statusLabel = ChatColor.GOLD + "Ending"; }
            default       -> { mat = Material.GRAY_WOOL;   statusLabel = ChatColor.GRAY + "Restarting"; }
        }

        if (!spectatable) {
            mat = Material.GRAY_WOOL;
        }

        int players = arena.getActivePlayerCount();
        int spectators = arena.getSpectators().size();
        int max = arena.getTeams().size() * arena.getMaxInTeam();

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (spectatable) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + arena.getArenaName());
        } else {
            meta.setDisplayName(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + arena.getArenaName());
        }
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Status: " + statusLabel);
        lore.add(ChatColor.GRAY + "Players: " + ChatColor.WHITE + players + ChatColor.GRAY + "/" + ChatColor.WHITE + max);
        lore.add(ChatColor.GRAY + "Spectators: " + ChatColor.WHITE + spectators);
        lore.add("");
        if (spectatable) {
            lore.add(ChatColor.GREEN + "Click to spectate!");
        } else if (arena.getGamerules() == null || !arena.getGamerules().AllowSpectators) {
            lore.add(ChatColor.RED + "Spectating is disabled for this arena.");
        } else {
            lore.add(ChatColor.RED + "Game is not in progress yet.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public IArena getArena(int slot) {
        return slotToArena.get(slot);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
