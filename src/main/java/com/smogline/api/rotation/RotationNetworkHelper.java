package com.smogline.api.rotation;

import com.smogline.block.custom.rotation.MotorElectroBlock;
import com.smogline.block.entity.custom.MotorElectroBlockEntity;
import com.smogline.block.entity.custom.WindGenFlugerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class RotationNetworkHelper {
    private static final int MAX_SEARCH_DEPTH = 32;

    @Nullable
    public static RotationSource findSource(BlockEntity start, @Nullable Direction fromDir,
                                            Set<BlockPos> visited, int depth) {
        if (depth > MAX_SEARCH_DEPTH || visited.contains(start.getBlockPos())) {
            return null;
        }
        visited.add(start.getBlockPos());

        Level level = start.getLevel();
        if (level == null) return null;

        Direction[] nextDirs;
        if (start instanceof RotationalNode node) {
            nextDirs = node.getPropagationDirections(fromDir);
        } else {
            return null;
        }

        long currentTime = level.getGameTime();

        for (Direction dir : nextDirs) {
            BlockPos neighborPos = start.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null) continue;

            // 1. Прямые источники (мотор, ветряк)
            RotationSource direct = checkDirectSource(neighbor, dir);
            if (direct != null) return direct;

            // 2. Проверка, может ли сосед сам быть источником (например, выход сумматора)
            if (neighbor instanceof RotationalNode neighborNode) {
                if (neighborNode.canProvideSource(dir.getOpposite())) {
                    return new RotationSource(((Rotational) neighbor).getSpeed(), ((Rotational) neighbor).getTorque());
                }

                // 3. Кеш соседа
                RotationSource cached = neighborNode.getCachedSource();
                if (cached != null && neighborNode.isCacheValid(currentTime)) {
                    return cached;
                }

                // 4. Рекурсивный поиск
                RotationSource found = findSource(neighbor, dir.getOpposite(), visited, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Nullable
    private static RotationSource checkDirectSource(BlockEntity neighbor, Direction dir) {
        if (neighbor instanceof MotorElectroBlockEntity motor) {
            Direction motorFacing = motor.getBlockState().getValue(MotorElectroBlock.FACING);
            if (motorFacing == dir.getOpposite()) {
                return new RotationSource(motor.getSpeed(), motor.getTorque());
            }
        } else if (neighbor instanceof WindGenFlugerBlockEntity windGen) {
            return new RotationSource(windGen.getSpeed(), windGen.getTorque());
        }
        return null;
    }
}