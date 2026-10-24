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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Copies the build part of a map into the rest of it by the layout's copy rules, a part of it every tick so the
 * server keeps running. Every height is copied (air too, so the copies match), blocks turn or flip with the copy
 * (stairs, logs, beds...), and chests, signs and other block entities keep their contents.
 */
final class SymmetryCopy extends BukkitRunnable {

    /** How long one tick may copy. */
    private static final long NANOS_PER_TICK = 20_000_000;
    private static final BlockData AIR = Material.AIR.createBlockData();

    private final World world;
    private final Region build;
    private final List<CopyRule> copies;
    private final Player player;
    private final IntConsumer done;
    /** The build part's block entities, which are copied with their contents. */
    private final Set<BlockPos> blockEntities = new HashSet<>();
    /** Each copy's changed version of every block data seen so far. */
    private final List<Map<BlockData, BlockData>> changedData = new ArrayList<>();
    private int column;
    private int changed;

    /** {@code done} gets the number of blocks that changed. */
    SymmetryCopy(World world, Layout layout, Player player, IntConsumer done) {
        this.world = world;
        this.build = layout.build();
        this.copies = layout.copies();
        this.player = player;
        this.done = done;
        for (int i = 0; i < copies.size(); i++) {
            changedData.add(new HashMap<>());
        }
        for (int chunkX = build.minX() >> 4; chunkX <= build.maxX() >> 4; chunkX++) {
            for (int chunkZ = build.minZ() >> 4; chunkZ <= build.maxZ() >> 4; chunkZ++) {
                for (BlockState state : world.getChunkAt(chunkX, chunkZ).getTileEntities(false)) {
                    if (build.contains(state.getX(), state.getZ())) {
                        blockEntities.add(new BlockPos(state.getX(), state.getY(), state.getZ()));
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        long end = System.nanoTime() + NANOS_PER_TICK;
        int columns = build.columns();
        while (column < columns && System.nanoTime() < end) {
            copyColumn(build.minX() + column % build.width(), build.minZ() + column / build.width());
            column++;
        }
        if (column < columns) {
            if (player.isOnline()) {
                CloudeWars.actionBar(player, "symmetry-progress", "percent", String.valueOf(column * 100L / columns));
            }
            return;
        }
        cancel();
        done.accept(changed);
    }

    /** Copies one block column of the build part, from the bottom of the world to the highest block. */
    private void copyColumn(int x, int z) {
        List<BlockPos> targets = new ArrayList<>(copies.size());
        int top = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
        int highest = top;
        for (CopyRule copy : copies) {
            BlockPos target = copy.move(new BlockPos(x, 0, z));
            targets.add(target);
            highest = Math.max(highest, world.getHighestBlockYAt(target.x(), target.z(), HeightMap.WORLD_SURFACE));
        }
        for (int y = world.getMinHeight(); y <= highest; y++) {
            Block source = world.getBlockAt(x, y, z);
            BlockData data = y <= top ? source.getBlockData() : AIR;
            BlockState entity = blockEntities.contains(new BlockPos(x, y, z)) ? source.getState() : null;
            for (int i = 0; i < copies.size(); i++) {
                Block target = world.getBlockAt(targets.get(i).x(), y, targets.get(i).z());
                BlockData copyData = changed(data, i);
                if (entity != null) {
                    BlockState copy = entity.copy(target.getLocation());
                    copy.setBlockData(copyData);
                    copy.update(true, false);
                    changed++;
                } else if (!target.getBlockData().equals(copyData)) {
                    target.setBlockData(copyData, false);
                    changed++;
                }
            }
        }
    }

    private BlockData changed(BlockData data, int copy) {
        return changedData.get(copy).computeIfAbsent(data, original -> {
            BlockData result = original.clone();
            copies.get(copy).change(result);
            return result;
        });
    }
}
