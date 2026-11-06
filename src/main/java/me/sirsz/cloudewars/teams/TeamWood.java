package me.sirsz.cloudewars.teams;

import com.l299l.newbedwars.api.events.ShopPurchaseEvent;
import com.l299l.newbedwars.api.view.ArenaState;
import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.GamePlayerView;
import com.l299l.newbedwars.api.view.TeamView;
import java.util.ArrayList;
import java.util.List;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

/**
 * Team wood: oak planks from the shop become the buyer's team planks (a red player gets mangrove planks). New
 * Bedwars' API cannot change what the shop gives, so the planks are swapped right after the purchase; the shop
 * itself still shows oak planks.
 */
public final class TeamWood implements Listener {

    private final JavaPlugin plugin;
    private final TeamBlocks teamBlocks;

    public TeamWood(JavaPlugin plugin, TeamBlocks teamBlocks) {
        this.plugin = plugin;
        this.teamBlocks = teamBlocks;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPurchase(ShopPurchaseEvent event) {
        if (event.getKind() == ShopPurchaseEvent.Kind.ITEM) {
            Player player = event.getPlayer();
            plugin.getServer().getScheduler().runTask(plugin, () -> swapPlanks(player)); // the shop gives them after the event
        }
    }

    /** Planks that did not fit the inventory are dropped at the buyer's feet; they turn when picked up. */
    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        Item item = event.getItem();
        if (event.getEntity() instanceof Player player && item.getItemStack().getType() == Material.OAK_PLANKS) {
            Material planks = teamPlanks(player);
            if (planks != null) {
                item.setItemStack(item.getItemStack().withType(planks));
            }
        }
    }

    private void swapPlanks(Player player) {
        Material planks = player.isOnline() ? teamPlanks(player) : null;
        if (planks == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> swapped = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() == Material.OAK_PLANKS) {
                swapped.add(item.withType(planks));
                inventory.setItem(slot, null);
            }
        }
        // Added back so they stack with the team planks the player already has
        for (ItemStack leftover : inventory.addItem(swapped.toArray(ItemStack[]::new)).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    /** The planks of the player's team in a running game, or null when they are not playing or oak is their wood. */
    private @Nullable Material teamPlanks(Player player) {
        ArenaView arena = CloudeWars.api().getArena(player).orElse(null);
        TeamView team = arena == null || arena.getState() != ArenaState.PLAYING ? null
                : arena.getGamePlayer(player.getUniqueId()).flatMap(GamePlayerView::getTeam).orElse(null);
        Material planks = team == null ? null : teamBlocks.forTeam(Material.OAK_PLANKS, team.getColor());
        return planks == Material.OAK_PLANKS ? null : planks;
    }
}
