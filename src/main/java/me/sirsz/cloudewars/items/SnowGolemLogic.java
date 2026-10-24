package me.sirsz.cloudewars.items;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.entity.Player;

/** The shop's snow golem egg: spawns a {@link SnowGolems} snow golem for the player's team. */
public final class SnowGolemLogic implements CustomLogic {

    @Override
    public boolean perform(Player player, IArena arena) {
        SnowGolems golems = CloudeWars.snowGolems();
        return golems != null && golems.spawn(player, arena);
    }

    @Override
    public LogicType getType() {
        return LogicType.SNOW_GOLEM;
    }
}
