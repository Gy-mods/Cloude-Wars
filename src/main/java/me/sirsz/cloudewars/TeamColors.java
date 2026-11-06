package me.sirsz.cloudewars;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.jspecify.annotations.Nullable;

/** Team colors by the names admins type in /bw createTeam ("orange", "lime", "blue", ...). */
@SuppressWarnings("deprecation") // New Bedwars' team colors are org.bukkit.ChatColor
public final class TeamColors {

    /** The 14 team colors that have a dye color, by their block color name. */
    private static final List<String> DYE_NAMES = List.of("white", "orange", "pink", "light_blue", "yellow", "lime",
            "gray", "light_gray", "cyan", "purple", "blue", "green", "red", "black");
    private static final Map<ChatColor, String> NAMES = new EnumMap<>(ChatColor.class);

    static {
        for (String name : DYE_NAMES) {
            NAMES.put(fromName(name), name);
        }
    }

    private TeamColors() {
    }

    /** The team color for a name, translated the same way as New Bedwars' /bw createTeam (TeamColor.parse). */
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
                    ChatColor color = ChatColor.valueOf(upper);
                    yield color.isColor() ? color : null;
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }

    /** The block color name of a team color ("orange" for GOLD), or null for colors without one (dark red...). */
    public static @Nullable String name(ChatColor color) {
        return NAMES.get(color);
    }
}
