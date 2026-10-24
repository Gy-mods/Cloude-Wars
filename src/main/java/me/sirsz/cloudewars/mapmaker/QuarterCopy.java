package me.sirsz.cloudewars.mapmaker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import me.sirsz.cloudewars.BlockPos;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Copies the build quarter into the other three quarters, turned around the map's middle, a part of it every tick so
 * the server keeps running. Every height is copied (air too, so the copies match), blocks turn with the map (stairs,
 * logs, beds...), and chests, signs and other block entities keep their contents.
 */
final class QuarterCopy extends BukkitRunnable {

    /** How long one tick may copy. */
    private static final long NANOS_PER_TICK = 20_000_000;
    /** The block turn of each copy: copy 1 is one quarter clockwise, ... */
    private static final StructureRotation[] TURNS = {StructureRotation.NONE, StructureRotation.CLOCKWISE_90,
            StructureRotation.CLOCKWISE_180, StructureRotation.COUNTERCLOCKWISE_90};
    private static final BlockData AIR = Material.AIR.createBlockData();

    private final World world;
    private final Quarter build;
    private final int size;
    private final Player player;
    private final IntConsumer done;
    /** The build quarter's block entities, which are copied with their contents. */
    private final Set<BlockPos> blockEntities = new HashSet<>();
    /** Each copy's turned version of every block data seen so far. */
    private final List<Map<BlockData, BlockData>> turned = new ArrayList<>();
    private int column;
    private int changed;

    /** {@code done} gets the number of blocks that changed. */
    QuarterCopy(World world, Quarter build, int size, Player player, IntConsumer done) {
        this.world = world;
        this.build = build;
        this.size = size;
        this.player = player;
        this.done = done;
        for (int steps = 0; steps < 4; steps++) {
            turned.add(new HashMap<>());
        }
        for (int chunkX = build.minX(size) >> 4; chunkX <= build.maxX(size) >> 4; chunkX++) {
            for (int chunkZ = build.minZ(size) >> 4; chunkZ <= build.maxZ(size) >> 4; chunkZ++) {
                for (BlockState state : world.getChunkAt(chunkX, chunkZ).getTileEntities(false)) {
                    if (Quarter.of(state.getX(), state.getZ()) == build && Quarter.inMap(state.getX(), state.getZ(), size)) {
                        blockEntities.add(new BlockPos(state.getX(), state.getY(), state.getZ()));
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        long end = System.nanoTime() + NANOS_PER_TICK;
        int columns = size * size;
        while (column < columns && System.nanoTime() < end) {
            copyColumn(build.minX(size) + column / size, build.minZ(size) + column % size);
            column++;
        }
        if (column < columns) {
            if (player.isOnline()) {
                CloudeWars.actionBar(player, "symmetry-progress", "percent", String.valueOf(column * 100 / columns));
            }
            return;
        }
        cancel();
        done.accept(changed);
    }

    /** Copies one block column of the build quarter, from the bottom of the world to the highest block. */
    private void copyColumn(int x, int z) {
        BlockPos[] targets = new BlockPos[4];
        int top = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
        int highest = top;
        for (int steps = 1; steps < 4; steps++) {
            targets[steps] = Quarter.turn(new BlockPos(x, 0, z), steps);
            highest = Math.max(highest, world.getHighestBlockYAt(targets[steps].x(), targets[steps].z(), HeightMap.WORLD_SURFACE));
        }
        for (int y = world.getMinHeight(); y <= highest; y++) {
            Block source = world.getBlockAt(x, y, z);
            BlockData data = y <= top ? source.getBlockData() : AIR;
            BlockState entity = blockEntities.contains(new BlockPos(x, y, z)) ? source.getState() : null;
            for (int steps = 1; steps < 4; steps++) {
                Block target = world.getBlockAt(targets[steps].x(), y, targets[steps].z());
                BlockData turnedData = turned(data, steps);
                if (entity != null) {
                    BlockState copy = entity.copy(target.getLocation());
                    copy.setBlockData(turnedData);
                    copy.update(true, false);
                    changed++;
                } else if (!target.getBlockData().equals(turnedData)) {
                    target.setBlockData(turnedData, false);
                    changed++;
                }
            }
        }
    }

    private BlockData turned(BlockData data, int steps) {
        return turned.get(steps).computeIfAbsent(data, original -> {
            BlockData copy = original.clone();
            copy.rotate(TURNS[steps]);
            return copy;
        });
    }
}
