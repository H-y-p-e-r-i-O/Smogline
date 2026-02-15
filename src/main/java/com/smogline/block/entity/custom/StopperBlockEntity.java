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

public class StopperBlockEntity extends BlockEntity implements Rotational {
    private long speed = 0;
    private long torque = 0;
    private boolean hasSource = false;

    private static final int MAX_SEARCH_DEPTH = 32;

    public StopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STOPPER_BE.get(), pos, state);
    }

    // Rotational
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = speed; setChanged(); sync(); }
    @Override public void setTorque(long torque) { this.torque = torque; setChanged(); sync(); }
    @Override public long getMaxSpeed() { return 0; }
    @Override public long getMaxTorque() { return 0; }

    public boolean hasSource() { return hasSource; }

    // Поиск источника вращения
    @Nullable
    public ShaftIronBlockEntity.SourceInfo findSource(Set<BlockPos> visited, @Nullable Direction fromDir, int depth) {
        // Если блок в состоянии "enabled=false" (не пропускает), обрываем цепь
        if (!level.getBlockState(worldPosition).getValue(StopperBlock.ENABLED)) {
            return null;
        }

        if (depth > MAX_SEARCH_DEPTH || visited.contains(worldPosition)) {
            return null;
        }
        visited.add(worldPosition);

        Direction facing = getBlockState().getValue(StopperBlock.FACING);
        // Лево и право относительно лицевой стороны (аналогично RotationMeter)
        Direction left, right;
        switch (facing) {
            case NORTH: left = Direction.WEST;  right = Direction.EAST; break;
            case SOUTH: left = Direction.EAST;  right = Direction.WEST; break;
            case EAST:  left = Direction.NORTH; right = Direction.SOUTH; break;
            case WEST:  left = Direction.SOUTH; right = Direction.NORTH; break;
            case UP:    left = Direction.NORTH; right = Direction.SOUTH; break; // для вертикальных можно оставить как есть, но валы к верху/низу не подключаются? Пока оставим.
            case DOWN:  left = Direction.SOUTH; right = Direction.NORTH; break;
            default: left = right = null;
        }

        Direction[] searchDirs;
        if (fromDir != null) {
            if (fromDir == left || fromDir == right) {
                searchDirs = new Direction[]{fromDir.getOpposite()};
            } else {
                return null;
            }
        } else {
            searchDirs = new Direction[]{left, right};
        }

        for (Direction dir : searchDirs) {
            if (dir == null) continue;
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            if (neighbor instanceof MotorElectroBlockEntity motor) {
                Direction motorFacing = motor.getBlockState().getValue(MotorElectroBlock.FACING);
                if (motorFacing == dir.getOpposite()) {
                    return new ShaftIronBlockEntity.SourceInfo(motor.getSpeed(), motor.getTorque());
                }
            } else if (neighbor instanceof ShaftIronBlockEntity shaft) {
                Direction shaftFacing = shaft.getBlockState().getValue(ShaftIronBlock.FACING);
                if (shaftFacing == dir || shaftFacing == dir.getOpposite()) {
                    ShaftIronBlockEntity.SourceInfo found = shaft.findSource(visited, dir.getOpposite(), depth + 1);
                    if (found != null) return found;
                }
            } else if (neighbor instanceof RotationMeterBlockEntity meter) {
                ShaftIronBlockEntity.SourceInfo found = meter.findSource(visited, dir.getOpposite(), depth + 1);
                if (found != null) return found;
            } else if (neighbor instanceof GearPortBlockEntity gear) {
                ShaftIronBlockEntity.SourceInfo found = gear.findSource(visited, dir.getOpposite());
                if (found != null) return found;
            } else if (neighbor instanceof StopperBlockEntity stopper) {
                // Обрабатываем следующий Stopper
                ShaftIronBlockEntity.SourceInfo found = stopper.findSource(visited, dir.getOpposite(), depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    // Тик
    public static void tick(Level level, BlockPos pos, BlockState state, StopperBlockEntity be) {
        if (level.isClientSide) return;

        ShaftIronBlockEntity.SourceInfo source = be.findSource(new HashSet<>(), null, 0);
        boolean nowHasSource = (source != null);
        long newSpeed = nowHasSource ? source.speed : 0;
        long newTorque = nowHasSource ? source.torque : 0;

        boolean changed = false;
        if (be.hasSource != nowHasSource) {
            be.hasSource = nowHasSource;
            changed = true;
        }
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
        tag.putBoolean("HasSource", hasSource);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        hasSource = tag.getBoolean("HasSource");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        tag.putBoolean("HasSource", hasSource);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        hasSource = tag.getBoolean("HasSource");
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