package com.smogline.block.entity.custom;

import com.smogline.api.rotation.RotationNetworkHelper;
import com.smogline.api.rotation.RotationSource;
import com.smogline.api.rotation.Rotational;
import com.smogline.api.rotation.RotationalNode;
import com.smogline.block.custom.rotation.AdderBlock;
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

public class AdderBlockEntity extends BlockEntity implements RotationalNode {

    private long speed = 0;
    private long torque = 0;

    // Кеш собственного значения
    private RotationSource cachedSource;
    private long cacheTimestamp;
    private static final long CACHE_LIFETIME = 20;

    public AdderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADDER_BE.get(), pos, state);
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
     * Определяет, может ли этот блок предоставить источник вращения при запросе с указанного направления.
     * Для сумматора это выходная сторона (противоположная лицевой).
     */
    @Override
    public boolean canProvideSource(@Nullable Direction fromDir) {
        if (fromDir == null) return false;
        Direction facing = getBlockState().getValue(AdderBlock.FACING);
        Direction outputSide = facing.getOpposite();
        return fromDir == outputSide;
    }

    /**
     * Определяет направления для продолжения поиска.
     * Сумматор сам не участвует в распространении поиска дальше,
     * поэтому всегда возвращаем пустой массив.
     */
    @Override
    public Direction[] getPropagationDirections(@Nullable Direction fromDir) {
        return new Direction[0];
    }



    // ========== Тик ==========
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

        RotationSource sourceLeft = (left != null) ? findSourceOnSide(be, left) : null;
        RotationSource sourceRight = (right != null) ? findSourceOnSide(be, right) : null;

        long newSpeed = 0;
        long newTorque = 0;

        if (sourceLeft != null && sourceRight != null) {
            newSpeed = sourceLeft.speed() + sourceRight.speed();
            newTorque = sourceLeft.torque() + sourceRight.torque();
        } else if (sourceLeft != null) {
            newSpeed = sourceLeft.speed();
            newTorque = sourceLeft.torque();
        } else if (sourceRight != null) {
            newSpeed = sourceRight.speed();
            newTorque = sourceRight.torque();
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

    @Nullable
    private static RotationSource findSourceOnSide(AdderBlockEntity be, Direction side) {
        if (be.level == null) return null;
        BlockPos neighborPos = be.worldPosition.relative(side);
        BlockEntity neighbor = be.level.getBlockEntity(neighborPos);
        if (neighbor == null) return null;
        // Начинаем поиск с соседа, указывая направление, с которого мы пришли (противоположное side)
        return RotationNetworkHelper.findSource(neighbor, side.getOpposite(), new HashSet<>(), 0);
    }

    // ========== NBT и синхронизация ==========
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
        cachedSource = null;
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