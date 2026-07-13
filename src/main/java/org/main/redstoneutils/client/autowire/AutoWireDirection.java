package org.main.redstoneutils.client.autowire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

public final class AutoWireDirection {

    private AutoWireDirection() {
    }

    public static Direction fromTo(BlockPos from, BlockPos to) {
        if (from == null || to == null) return Direction.NORTH;
        return fromDelta(to.subtract(from));
    }

    public static Direction fromDelta(Vec3i delta) {
        if (delta == null) return Direction.NORTH;

        int x = delta.getX();
        int z = delta.getZ();
        if (x == 0 && z == 0) return Direction.NORTH;

        if (Math.abs(x) >= Math.abs(z)) {
            return x > 0 ? Direction.WEST : Direction.EAST;
        }

        return z > 0 ? Direction.NORTH : Direction.SOUTH;
    }
}
