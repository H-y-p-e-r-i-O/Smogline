package com.smogline.block.entity.custom;

import com.smogline.api.rotation.RotationNetworkHelper;
import com.smogline.api.rotation.RotationSource;
import com.smogline.api.rotation.Rotational;
import com.smogline.api.rotation.RotationalNode;
import com.smogline.block.custom.rotation.RotationMeterBlock;
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

public class RotationMeterBlockEntity extends BlockEntity implements RotationalNode {

    private long speed = 0;
    private long torque = 0;

    // Кеш найденного источника
    private RotationSource cachedSource;
    private long cacheTimestamp;
    private static final long CACHE_LIFETIME = 20; // тиков (1 секунда)

    public RotationMeterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROTATION_METER_BE.get(), pos, state);
    }

    // ========== Rotational ==========
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = speed; setChanged(); sync(); }
    @Override public void setTorque(long torque) { this.torque = torque; setChanged(); sync(); }
    @Override public long getMaxSpeed() { return 0; }
    @Override public long getMaxTorque() { return 0; }

    // ========== RotationalNode ==========
    @Override
    @Nullable
    public RotationSource getCachedSource() { return cachedSource; }

    @Override
    public void setCachedSource(@Nullable RotationSource source, long gameTime) {
        this.cachedSource = source;
        this.cacheTimestamp = gameTime;
    }

    @Override
    public boolean isCacheValid(long currentTime) {
        return cachedSource != null && (currentTime - cacheTimestamp) <= CACHE_LIFETIME;
    }

    @Override
    public void invalidateCache() {
        this.cachedSource = null;
    }

    /**
     * RotationMeter сам по себе не является источником вращения.
     */
    @Override
    public boolean canProvideSource(@Nullable Direction fromDir) {
        return false;
    }

    /**
     * Определяет направления для продолжения поиска: левая и правая стороны относительно лицевой.
     */
    @Override
    public Direction[] getPropagationDirections(@Nullable Direction fromDir) {
        Direction facing = getBlockState().getValue(RotationMeterBlock.FACING);
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

        if (fromDir != null) {
            // Если пришли с левой или правой стороны, продолжаем в противоположном направлении
            if (fromDir == left || fromDir == right) {
                return new Direction[]{fromDir.getOpposite()};
            } else {
                return new Direction[0];
            }
        } else {
            // Начало поиска — проверяем обе стороны
            return new Direction[]{left, right};
        }
    }

    // ========== Тик ==========
    public static void tick(Level level, BlockPos pos, BlockState state, RotationMeterBlockEntity be) {
        if (level.isClientSide) return;

        long currentTime = level.getGameTime();

        // Если кеш устарел или отсутствует – выполняем поиск
        if (!be.isCacheValid(currentTime)) {
            // Поиск начинаем с самого метра, fromDir = null (обе стороны)
            RotationSource source = RotationNetworkHelper.findSource(be, null, new HashSet<>(), 0);
            be.setCachedSource(source, currentTime);
        }

        RotationSource src = be.getCachedSource();
        long newSpeed = (src != null) ? src.speed() : 0;
        long newTorque = (src != null) ? src.torque() : 0;

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

    // ========== NBT и синхронизация ==========
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        // Кеш не сохраняем
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        cachedSource = null; // сброс кеша
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

    /**
     * Вспомогательный метод для проверки наличия источника (для удобства).
     */
    public boolean hasSource() {
        return cachedSource != null;
    }
}