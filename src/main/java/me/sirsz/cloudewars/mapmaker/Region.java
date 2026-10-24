package me.sirsz.cloudewars.mapmaker;

/** A box of block columns, from min to max (both included), at every height. */
record Region(int minX, int maxX, int minZ, int maxZ) {

    boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    int width() {
        return maxX - minX + 1;
    }

    int columns() {
        return width() * (maxZ - minZ + 1);
    }
}
