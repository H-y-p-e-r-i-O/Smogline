package com.smogline.api.rotation;

import com.smogline.block.custom.rotation.MotorElectroBlock;
import com.smogline.block.entity.custom.MotorElectroBlockEntity;
import com.smogline.block.entity.custom.WindGenFlugerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class RotationNetworkHelper {
    private static final int MAX_SEARCH_DEPTH = 64;

    @Nullable
    public static RotationSource findSource(BlockEntity startNode, @Nullable Direction fromDir) {
        return findSourceInternal(startNode, fromDir, new HashSet<>(), 0);
    }

    @Nullable
    private static RotationSource findSourceInternal(BlockEntity start, @Nullable Direction fromDir,
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

            // 1. Сначала проверяем, является ли сосед ИСТОЧНИКОМ (Мотор, Ветряк)
            // dir - направление ОТ нас К соседу.
            // Сосед должен смотреть НА нас (то есть его facing == dir.getOpposite())
            RotationSource directSource = getDirectSource(neighbor, dir);
            if (directSource != null) {
                return directSource;
            }

            // 2. Если не источник, проверяем, является ли он узлом сети (Вал)
            if (neighbor instanceof RotationalNode neighborNode) {
                // Оптимизация: верим кешу соседа, если он валиден
                if (neighborNode.isCacheValid(currentTime)) {
                    RotationSource cached = neighborNode.getCachedSource();
                    if (cached != null) return cached;
                }

                // Рекурсивный поиск
                RotationSource found = findSourceInternal(neighbor, dir.getOpposite(), visited, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Nullable
    private static RotationSource getDirectSource(BlockEntity neighbor, Direction dir) {
        if (neighbor instanceof MotorElectroBlockEntity motor) {
            // Строгая проверка: Мотор должен смотреть в сторону вала.
            // dir - направление от Вала к Мотору.
            // dir.getOpposite() - направление от Мотора к Валу.
            // Мотор отдает энергию только ПЕРЕД собой.
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