package me.sirsz.cloudewars;

import org.bukkit.block.Block;
import org.jspecify.annotations.Nullable;

/** A block position, written as "x,y,z" in Cloude Wars' data files. */
public record BlockPos(int x, int y, int z) {

    public static BlockPos of(Block block) {
        return new BlockPos(block.getX(), block.getY(), block.getZ());
    }

    public static @Nullable BlockPos parse(String text) {
        String[] parts = text.split(",");
        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}
