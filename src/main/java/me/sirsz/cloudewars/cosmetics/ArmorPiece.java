package me.sirsz.cloudewars.cosmetics;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/** The four armor slots, by their index in Bukkit's armor contents array (boots first). */
public enum ArmorPiece {
    BOOTS,
    LEGGINGS,
    CHESTPLATE,
    HELMET;

    /** The name players type, e.g. "helmet". */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static @Nullable ArmorPiece byId(String id) {
        for (ArmorPiece piece : values()) {
            if (piece.id().equalsIgnoreCase(id)) {
                return piece;
            }
        }
        return null;
    }
}
