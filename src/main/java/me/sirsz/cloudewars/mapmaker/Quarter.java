package me.sirsz.cloudewars.mapmaker;

import me.sirsz.cloudewars.BlockPos;
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

    int minX(int size) {
        return signX > 0 ? 0 : -size;
    }

    int maxX(int size) {
        return signX > 0 ? size - 1 : -1;
    }

    int minZ(int size) {
        return signZ > 0 ? 0 : -size;
    }

    int maxZ(int size) {
        return signZ > 0 ? size - 1 : -1;
    }

    /** Whether a block column is inside the map of quarters this size (in any quarter). */
    static boolean inMap(int x, int z, int size) {
        return x >= -size && x < size && z >= -size && z < size;
    }

    /**
     * Where a block goes when the map turns clockwise by 90 degrees {@code steps} times around the middle (the corner
     * at x = 0, z = 0). One turn moves a quarter to the next one clockwise, like east to south.
     */
    static BlockPos turn(BlockPos pos, int steps) {
        int x = pos.x();
        int z = pos.z();
        for (int i = 0; i < steps; i++) {
            int turnedX = -z - 1;
            z = x;
            x = turnedX;
        }
        return new BlockPos(x, pos.y(), z);
    }
}
