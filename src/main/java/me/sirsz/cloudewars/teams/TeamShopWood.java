package me.sirsz.cloudewars.teams;

import com.l299l.newbedwars.api.events.ShopItemStackEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Team wood on New Bedwars 2.8-beta and newer: the shop shows and gives the buyer's team planks instead of oak planks
 * (a red player sees and gets mangrove planks). Older New Bedwars uses {@link TeamWood}.
 */
public final class TeamShopWood implements Listener {

    private final TeamBlocks teamBlocks;

    private TeamShopWood(TeamBlocks teamBlocks) {
        this.teamBlocks = teamBlocks;
    }

    /** Only call this on New Bedwars 2.8-beta or newer: the event class does not exist before. */
    public static Listener create(TeamBlocks teamBlocks) {
        return new TeamShopWood(teamBlocks);
    }

    @EventHandler
    public void onShopItem(ShopItemStackEvent event) {
        if (event.getItem().getType() != Material.OAK_PLANKS || event.getTeam().isEmpty()) {
            return;
        }
        Material planks = teamBlocks.forTeam(Material.OAK_PLANKS, event.getTeam().get().getColor());
        if (planks != Material.OAK_PLANKS) {
            event.setItem(event.getItem().withType(planks)); // withType keeps the meta New Bedwars recognises its items by
        }
    }
}
