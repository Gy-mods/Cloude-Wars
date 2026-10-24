package me.sirsz.cloudewars.mapmaker;

import org.jspecify.annotations.Nullable;

/**
 * The two halves of a map, split between blocks at z = 0 (north and south halves) or at x = 0 (east and west). A half
 * of size n is 2n block columns along the line and n away from it.
 */
enum Half {
    SOUTH("south"),
    NORTH("north"),
    EAST("east"),
    WEST("west");

    private final String id;

    Half(String id) {
        this.id = id;
    }

    /** The half a block column is in, split at the line (z = 0 or x = 0) the column is farther from. */
    static Half of(int x, int z) {
        if (Math.abs(z + 0.5) >= Math.abs(x + 0.5)) {
            return z >= 0 ? SOUTH : NORTH;
        }
        return x >= 0 ? EAST : WEST;
    }

    static @Nullable Half byId(String id) {
        for (Half half : values()) {
            if (half.id.equalsIgnoreCase(id)) {
                return half;
            }
        }
        return null;
    }

    String id() {
        return id;
    }

    /** Whether the line between this half and the other one is z = 0. */
    boolean splitAtZ() {
        return this == SOUTH || this == NORTH;
    }

    Half opposite() {
        return switch (this) {
            case SOUTH -> NORTH;
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    Region region(int size) {
        return switch (this) {
            case SOUTH -> new Region(-size, size - 1, 0, size - 1);
            case NORTH -> new Region(-size, size - 1, -size, -1);
            case EAST -> new Region(0, size - 1, -size, size - 1);
            case WEST -> new Region(-size, -1, -size, size - 1);
        };
    }
}
