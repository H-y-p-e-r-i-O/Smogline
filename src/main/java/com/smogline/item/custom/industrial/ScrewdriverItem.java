package com.smogline.item.custom.industrial;

import com.smogline.block.custom.rotation.GearPortBlock;
import com.smogline.block.custom.rotation.ShaftIronBlock;
import com.smogline.block.entity.custom.GearPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ScrewdriverItem extends Item {

    public ScrewdriverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        boolean isSneaking = player.isShiftKeyDown();

        // Обработка вала
        if (state.getBlock() instanceof ShaftIronBlock) {
            Direction currentFacing = state.getValue(ShaftIronBlock.FACING);
            Direction newFacing;

            if (isSneaking) {
                newFacing = currentFacing.getOpposite();
            } else {
                Direction lookDir = getLookDirection(player);
                newFacing = rotate90(currentFacing, lookDir);
            }

            BlockState newState = state.setValue(ShaftIronBlock.FACING, newFacing);
            level.setBlock(pos, newState, 3);
            syncWithNeighbors(level, pos, newState);

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Обработка порта
        else if (state.getBlock() instanceof GearPortBlock) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof GearPortBlockEntity gear) {
                    Direction face = context.getClickedFace();
                    String message = gear.handleScrewdriverClick(face, isSneaking);
                    if (message != null) {
                        player.displayClientMessage(Component.literal(message), false);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    /**
     * Проверяет соседние валы и разворачивает текущий, если нужно (фронт к фронту, зад к заду)
     * Логика: вал должен смотреть в ту же сторону что и сосед (продолжать линию)
     */
    private void syncWithNeighbors(Level level, BlockPos pos, BlockState state) {
        Direction myFacing = state.getValue(ShaftIronBlock.FACING);

        // Проверяем соседей вдоль оси вала (фронт и тыл)
        for (Direction dir : new Direction[]{myFacing, myFacing.getOpposite()}) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof ShaftIronBlock) {
                Direction neighborFacing = neighborState.getValue(ShaftIronBlock.FACING);

                // Если сосед смотрит в ту же сторону — всё ок, ничего не делаем
                // Если сосед смотрит в противоположную сторону — разворачиваемся
                if (neighborFacing == myFacing.getOpposite()) {
                    Direction correctedFacing = myFacing.getOpposite();
                    BlockState correctedState = state.setValue(ShaftIronBlock.FACING, correctedFacing);
                    level.setBlock(pos, correctedState, 3);
                    return;
                }
            }
        }
    }

    /**
     * Поворачивает направление на 90°
     * - Горизонтальный взгляд → вращение вокруг Y
     * - Вертикальный взгляд → вращение вокруг X
     */
    private Direction rotate90(Direction current, Direction lookDir) {
        if (lookDir.getAxis() == Direction.Axis.Y) {
            // Смотрим вверх/вниз - вращаем вокруг X
            return rotateAroundX(current);
        } else {
            // Смотрим горизонтально - вращаем вокруг Y
            return rotateAroundY(current);
        }
    }

    private Direction rotateAroundY(Direction dir) {
        return switch (dir) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case UP, DOWN -> dir;
        };
    }

    private Direction rotateAroundX(Direction dir) {
        return switch (dir) {
            case UP -> Direction.SOUTH;
            case SOUTH -> Direction.DOWN;
            case DOWN -> Direction.NORTH;
            case NORTH -> Direction.UP;
            case EAST, WEST -> dir;
        };
    }

    private Direction getLookDirection(Player player) {
        float pitch = player.getXRot();
        float yaw = player.getYRot();

        if (pitch < -45) return Direction.UP;
        if (pitch > 45) return Direction.DOWN;

        float normalizedYaw = (yaw % 360 + 360) % 360;

        if (normalizedYaw < 45 || normalizedYaw >= 315) return Direction.SOUTH;
        if (normalizedYaw < 135) return Direction.WEST;
        if (normalizedYaw < 225) return Direction.NORTH;
        return Direction.EAST;
    }
}