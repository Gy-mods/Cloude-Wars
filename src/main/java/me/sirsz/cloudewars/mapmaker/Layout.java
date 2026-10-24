package me.sirsz.cloudewars.mapmaker;

import java.util.List;

/**
 * A symmetry set up for an arena: the whole map (2 x size blocks each way around x = 0, z = 0), the part the builder
 * builds, the rest of the map, and the copy rules (copy 1, 2, ... of the build part).
 *
 * @param name how the build part is called in messages, e.g. "south-east quarter"
 */
record Layout(String name, Region map, Region build, List<Region> rest, List<CopyRule> copies) {

    /** Four quarters; the copies are the build quarter turned by 90, 180 and 270 degrees clockwise. */
    static Layout quarters(int size, Quarter build) {
        return new Layout(build.id() + " quarter", map(size), build.region(size),
                List.of(build.clockwise(1).region(size), build.clockwise(2).region(size), build.clockwise(3).region(size)),
                List.of(new CopyRule.Turn(1), new CopyRule.Turn(2), new CopyRule.Turn(3)));
    }

    /**
     * Two halves; the copy is the build half mirrored across the line between them, or turned by 180 degrees around
     * the middle (central symmetry: both teams have the same things on the same side, like the ender chest on
     * their right).
     */
    static Layout halves(int size, Half build, boolean mirror) {
        CopyRule copy = !mirror ? new CopyRule.Turn(2) : build.splitAtZ() ? new CopyRule.MirrorZ() : new CopyRule.MirrorX();
        return new Layout(build.id() + " half", map(size), build.region(size), List.of(build.opposite().region(size)),
                List.of(copy));
    }

    private static Region map(int size) {
        return new Region(-size, size - 1, -size, size - 1);
    }
}
