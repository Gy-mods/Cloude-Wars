package me.sirsz.cloudewars;

import java.util.Locale;
import org.bukkit.ChatColor;
import org.jspecify.annotations.Nullable;

/** Team colors by the names admins type in /bw createTeam ("orange", "lime", "blue", ...). */
@SuppressWarnings("deprecation") // New Bedwars' team colors are org.bukkit.ChatColor
public final class TeamColors {

    private TeamColors() {
    }

    /** The team color for a name, translated the same way as New Bedwars' CreateTeamCommand.parseColor(). */
    public static @Nullable ChatColor fromName(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "PINK" -> ChatColor.LIGHT_PURPLE;
            case "ORANGE" -> ChatColor.GOLD;
            case "LIME" -> ChatColor.GREEN;
            case "LIGHT_GRAY" -> ChatColor.GRAY;
            case "GRAY" -> ChatColor.DARK_GRAY;
            case "CYAN" -> ChatColor.DARK_AQUA;
            case "LIGHT_BLUE" -> ChatColor.AQUA;
            case "BLUE" -> ChatColor.DARK_BLUE;
            case "PURPLE" -> ChatColor.DARK_PURPLE;
            case "GREEN" -> ChatColor.DARK_GREEN;
            default -> {
                try {
                    yield ChatColor.valueOf(upper);
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
}
