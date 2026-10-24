package me.sirsz.cloudewars.mapmaker;

import me.sirsz.cloudewars.BlockPos;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;

/**
 * How one copy of the build part is made: where each block goes, and how its block data turns or flips with it
 * (stairs, beds, logs, signs...). The map's middle is the block corner at x = 0, z = 0.
 */
sealed interface CopyRule {

    BlockPos move(BlockPos pos);

    /** Turns or flips block data in place (give it a clone). */
    void change(BlockData data);

    /** Turned clockwise around the middle by 90 degrees {@code steps} times (one turn: east becomes south). */
    record Turn(int steps) implements CopyRule {
        private static final StructureRotation[] ROTATIONS = {StructureRotation.NONE, StructureRotation.CLOCKWISE_90,
                StructureRotation.CLOCKWISE_180, StructureRotation.COUNTERCLOCKWISE_90};

        @Override
        public BlockPos move(BlockPos pos) {
            int x = pos.x();
            int z = pos.z();
            for (int i = 0; i < steps; i++) {
                int turnedX = -z - 1;
                z = x;
                x = turnedX;
            }
            return new BlockPos(x, pos.y(), z);
        }

        @Override
        public void change(BlockData data) {
            data.rotate(ROTATIONS[steps % 4]);
        }
    }

    /** Mirrored across the line z = 0 (north and south swap). */
    record MirrorZ() implements CopyRule {
        @Override
        public BlockPos move(BlockPos pos) {
            return new BlockPos(pos.x(), pos.y(), -pos.z() - 1);
        }

        @Override
        public void change(BlockData data) {
            data.mirror(Mirror.LEFT_RIGHT);
        }
    }

    /** Mirrored across the line x = 0 (east and west swap). */
    record MirrorX() implements CopyRule {
        @Override
        public BlockPos move(BlockPos pos) {
            return new BlockPos(-pos.x() - 1, pos.y(), pos.z());
        }

        @Override
        public void change(BlockData data) {
            data.mirror(Mirror.FRONT_BACK);
        }
    }
}
