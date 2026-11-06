package me.sirsz.cloudewars.teams;

import com.l299l.newbedwars.api.events.GameStartEvent;
import com.l299l.newbedwars.api.events.PlayerJoinArenaEvent;
import com.l299l.newbedwars.api.view.ArenaState;
import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** New Bedwars' round events: new random team colors before a round, team beds and team blocks when it starts. */
public final class GameListener implements Listener {

    private final TeamColorRandomizer randomizer;
    private final TeamBlockMarks teamBlockMarks;

    public GameListener(TeamColorRandomizer randomizer, TeamBlockMarks teamBlockMarks) {
        this.randomizer = randomizer;
        this.teamBlockMarks = teamBlockMarks;
    }

    /** The first player of a waiting arena starts a new round: its teams get new colors before anyone is in a team. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinArenaEvent event) {
        ArenaView arena = event.getArena();
        if (!event.isRejoin() && arena.getState() == ArenaState.WAITING && arena.getPlayers().isEmpty()) {
            randomizer.randomize(arena);
        }
    }

    @EventHandler
    public void onGameStart(GameStartEvent event) {
        randomizer.paintBeds(event.getArena());
        teamBlockMarks.recolor(event.getArena());
    }
}
