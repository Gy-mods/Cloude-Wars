package me.sirsz.cloudewars.mapmaker;

import org.jspecify.annotations.Nullable;

/**
 * The four quarters of a map, split at x = 0 and z = 0 between blocks (so no block is in two quarters), in clockwise
 * order as seen from above. A quarter of size n is n x n block columns.
 */
enum Quarter {
    SOUTH_EAST("south-east", 1, 1),
    SOUTH_WEST("south-west", -1, 1),
    NORTH_WEST("north-west", -1, -1),
    NORTH_EAST("north-east", 1, -1);

    private final String id;
    private final int signX;
    private final int signZ;

    Quarter(String id, int signX, int signZ) {
        this.id = id;
        this.signX = signX;
        this.signZ = signZ;
    }

    /** The quarter a block column is in. */
    static Quarter of(int x, int z) {
        if (x >= 0) {
            return z >= 0 ? SOUTH_EAST : NORTH_EAST;
        }
        return z >= 0 ? SOUTH_WEST : NORTH_WEST;
    }

    static @Nullable Quarter byId(String id) {
        for (Quarter quarter : values()) {
            if (quarter.id.equalsIgnoreCase(id)) {
                return quarter;
            }
        }
        return null;
    }

    String id() {
        return id;
    }

    /** The quarter {@code steps} quarters further clockwise. */
    Quarter clockwise(int steps) {
        return values()[(ordinal() + steps) % 4];
    }

    Region region(int size) {
        return new Region(signX > 0 ? 0 : -size, signX > 0 ? size - 1 : -1, signZ > 0 ? 0 : -size, signZ > 0 ? size - 1 : -1);
    }
}
