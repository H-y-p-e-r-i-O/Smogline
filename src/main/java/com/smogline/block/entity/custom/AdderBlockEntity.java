package com.smogline.block.entity.custom;

import com.smogline.api.rotation.Rotational;
import com.smogline.block.custom.rotation.*;
import com.smogline.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class AdderBlockEntity extends BlockEntity implements Rotational {
    private long speed = 0;
    private long torque = 0;

    private static final int MAX_SEARCH_DEPTH = 32;

    public AdderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADDER_BE.get(), pos, state);
    }

    // Rotational
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = speed; setChanged(); sync(); }
    @Override public void setTorque(long torque) { this.torque = torque; setChanged(); sync(); }
    @Override public long getMaxSpeed() { return 0; }
    @Override public long getMaxTorque() { return 0; }

    // Внешний поиск источника (вызывается из валов и других блоков, когда они подходят к выходу)
    @Nullable
    public ShaftIronBlockEntity.SourceInfo findSource(Set<BlockPos> visited, @Nullable Direction fromDir, int depth) {
        if (depth > MAX_SEARCH_DEPTH || visited.contains(worldPosition)) {
            return null;
        }
        visited.add(worldPosition);

        Direction facing = getBlockState().getValue(AdderBlock.FACING);
        Direction outputSide = facing.getOpposite();

        // Если запрос пришёл с выходной стороны, возвращаем текущие значения
        if (fromDir == outputSide) {
            return new ShaftIronBlockEntity.SourceInfo(speed, torque);
        }

        // В остальных случаях (запрос с других сторон) не пропускаем
        return null;
    }

    // Перегрузка для внешнего вызова без depth
    @Nullable
    public ShaftIronBlockEntity.SourceInfo findSource(Set<BlockPos> visited, @Nullable Direction fromDir) {
        return findSource(visited, fromDir, 0);
    }

    // Внутренний поиск источника на конкретной стороне (для левого/правого порта)
    @Nullable
    private ShaftIronBlockEntity.SourceInfo findSourceOnSide(Direction side) {
        BlockPos neighborPos = worldPosition.relative(side);
        BlockEntity neighbor = level.getBlockEntity(neighborPos);

        if (neighbor == null) return null;

        Set<BlockPos> visited = new HashSet<>();
        visited.add(worldPosition);

        if (neighbor instanceof MotorElectroBlockEntity motor) {
            Direction motorFacing = motor.getBlockState().getValue(MotorElectroBlock.FACING);
            if (motorFacing == side.getOpposite()) {
                return new ShaftIronBlockEntity.SourceInfo(motor.getSpeed(), motor.getTorque());
            }
        } else if (neighbor instanceof ShaftIronBlockEntity shaft) {
            Direction shaftFacing = shaft.getBlockState().getValue(ShaftIronBlock.FACING);
            if (shaftFacing == side || shaftFacing == side.getOpposite()) {
                return shaft.findSource(visited, side.getOpposite());
            }
        } else if (neighbor instanceof RotationMeterBlockEntity meter) {
            return meter.findSource(visited, side.getOpposite(), 0);
        } else if (neighbor instanceof GearPortBlockEntity gear) {
            return gear.findSource(visited, side.getOpposite());
        } else if (neighbor instanceof StopperBlockEntity stopper) {
            return stopper.findSource(visited, side.getOpposite(), 0);
        } else if (neighbor instanceof TachometerBlockEntity tacho) {   // <-- НОВАЯ СТРОКА
            return tacho.findSource(visited, side.getOpposite(), 0);    // <-- НОВАЯ СТРОКА
        } else if (neighbor instanceof WindGenFlugerBlockEntity windGen) {
            // Генератор сам является источником
            return new ShaftIronBlockEntity.SourceInfo(windGen.getSpeed(), windGen.getTorque());
        }
        return null;
    }

    // Тик
    public static void tick(Level level, BlockPos pos, BlockState state, AdderBlockEntity be) {
        if (level.isClientSide) return;

        Direction facing = state.getValue(AdderBlock.FACING);
        Direction left, right;
        switch (facing) {
            case NORTH:
                left = Direction.WEST;
                right = Direction.EAST;
                break;
            case SOUTH:
                left = Direction.EAST;
                right = Direction.WEST;
                break;
            case EAST:
                left = Direction.NORTH;
                right = Direction.SOUTH;
                break;
            case WEST:
                left = Direction.SOUTH;
                right = Direction.NORTH;
                break;
            default:
                left = right = null;
        }

        ShaftIronBlockEntity.SourceInfo sourceLeft = (left != null) ? be.findSourceOnSide(left) : null;
        ShaftIronBlockEntity.SourceInfo sourceRight = (right != null) ? be.findSourceOnSide(right) : null;

        long newSpeed = 0;
        long newTorque = 0;

        if (sourceLeft != null && sourceRight != null) {
            // Суммируем
            newSpeed = sourceLeft.speed + sourceRight.speed;
            newTorque = sourceLeft.torque + sourceRight.torque;
        } else if (sourceLeft != null) {
            newSpeed = sourceLeft.speed;
            newTorque = sourceLeft.torque;
        } else if (sourceRight != null) {
            newSpeed = sourceRight.speed;
            newTorque = sourceRight.torque;
        }

        boolean changed = false;
        if (be.speed != newSpeed) {
            be.speed = newSpeed;
            changed = true;
        }
        if (be.torque != newTorque) {
            be.torque = newTorque;
            changed = true;
        }

        if (changed) {
            be.setChanged();
            be.sync();
        }
    }

    // NBT и синхронизация
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}