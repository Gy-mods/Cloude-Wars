package me.sirsz.cloudewars.items;

import com.l299l.newbedwars.api.registry.ShopLogic;
import com.l299l.newbedwars.api.view.ArenaView;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.entity.Player;

/**
 * The shop's snow golem egg ({@code itemLogic: SNOW_GOLEM} in New Bedwars' data/items.yml): spawns a
 * {@link SnowGolems} snow golem for the player's team and uses up the egg.
 */
public final class SnowGolemLogic implements ShopLogic {

    @Override
    public boolean perform(Player player, ArenaView arena) {
        SnowGolems golems = CloudeWars.snowGolems();
        return golems != null && golems.spawn(player, arena);
    }
}
