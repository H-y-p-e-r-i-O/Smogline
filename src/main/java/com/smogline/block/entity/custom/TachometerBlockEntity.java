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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class TachometerBlockEntity extends BlockEntity implements Rotational {
    private long speed = 0;
    private long torque = 0;
    private boolean hasSource = false;

    private int multiplier = 1; // 1,2,3
    // mode хранится в BlockState, но для удобства можем дублировать, либо получать из стейта каждый раз

    private static final int MAX_SEARCH_DEPTH = 32;

    public TachometerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TACHOMETER_BE.get(), pos, state);
    }

    // Rotational
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = speed; setChanged(); sync(); }
    @Override public void setTorque(long torque) { this.torque = torque; setChanged(); sync(); }
    @Override public long getMaxSpeed() { return 0; }
    @Override public long getMaxTorque() { return 0; }

    public boolean hasSource() { return hasSource; }
    public int getMultiplier() { return multiplier; }
    public void setMultiplier(int multiplier) {
        if (multiplier < 1 || multiplier > 3) multiplier = 1;
        this.multiplier = multiplier;
        setChanged();
        sync();
        updateRedstone();
    }

    public Mode getMode() {
        return getBlockState().getValue(TachometerBlock.MODE);
    }

    // Вычисляем сигнал редстоуна (0-15)
    public int getRedstoneSignal() {
        if (!hasSource) return 0;
        long value = (getMode() == Mode.SPEED) ? speed : torque;
        // Пример: при value 100 и multiplier 3 даст 15, если делить на 20. Можно подобрать.
        // Пусть максимальное значение скорости/момента у мотора = 100. Тогда signal = min(15, value * multiplier / 20)
        // Чтобы при value=100, mult=3 дало 15: 100*3/20 = 15. При value=50, mult=1: 50/20=2.5 -> 2.
        long raw = value * multiplier / 20;
        return (int) Math.min(15, raw);
    }

    // ========== Поиск источника ==========
    @Nullable
    public ShaftIronBlockEntity.SourceInfo findSource(Set<BlockPos> visited, @Nullable Direction fromDir, int depth) {
        if (depth > MAX_SEARCH_DEPTH || visited.contains(worldPosition)) {
            return null;
        }
        visited.add(worldPosition);

        Direction facing = getBlockState().getValue(TachometerBlock.FACING);
        Direction left = TachometerBlock.getLeft(facing);
        Direction right = TachometerBlock.getRight(facing);

        Direction[] searchDirs;
        if (fromDir != null) {
            if (fromDir == left || fromDir == right) {
                searchDirs = new Direction[]{fromDir.getOpposite()};
            } else {
                return null; // пришли не сбоку
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
                ShaftIronBlockEntity.SourceInfo found = stopper.findSource(visited, dir.getOpposite(), depth + 1);
                if (found != null) return found;
            } else if (neighbor instanceof AdderBlockEntity adder) {
                ShaftIronBlockEntity.SourceInfo found = adder.findSource(visited, dir.getOpposite(), depth + 1);
                if (found != null) return found;
            } else if (neighbor instanceof TachometerBlockEntity tach) {
                ShaftIronBlockEntity.SourceInfo found = tach.findSource(visited, dir.getOpposite(), depth + 1);
                if (found != null) return found;
            } else if (neighbor instanceof WindGenFlugerBlockEntity windGen) {
                // Генератор сам является источником
                return new ShaftIronBlockEntity.SourceInfo(windGen.getSpeed(), windGen.getTorque());
            }
        }
        return null;
    }

    // ========== Тик ==========
    public static void tick(Level level, BlockPos pos, BlockState state, TachometerBlockEntity be) {
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
            be.updateRedstone();
        }
    }

    private void updateRedstone() {
        if (level != null && !level.isClientSide) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    // ========== NBT ==========
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        tag.putBoolean("HasSource", hasSource);
        tag.putInt("Multiplier", multiplier);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        hasSource = tag.getBoolean("HasSource");
        multiplier = tag.getInt("Multiplier");
        if (multiplier < 1 || multiplier > 3) multiplier = 1;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        tag.putBoolean("HasSource", hasSource);
        tag.putInt("Multiplier", multiplier);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        hasSource = tag.getBoolean("HasSource");
        multiplier = tag.getInt("Multiplier");
        if (multiplier < 1 || multiplier > 3) multiplier = 1;
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