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

public class AdderBlockEntity extends BlockEntity implements RotationalNode {

    private long speed = 0;
    private long torque = 0;

    // Инициализируем нулями, чтобы не было null
    private RotationSource cachedSource = new RotationSource(0, 0);

    public AdderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADDER_BE.get(), pos, state);
    }

    // ========== Rotational ==========
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = speed; updateCache(); setChanged(); sync(); invalidateNeighborCaches(); }
    @Override public void setTorque(long torque) { this.torque = torque; updateCache(); setChanged(); sync(); invalidateNeighborCaches(); }
    @Override public long getMaxSpeed() { return 0; }
    @Override public long getMaxTorque() { return 0; }

    // ========== RotationalNode ==========

    // ВАЖНО: Сумматор всегда отдает свою текущую скорость как "кешированный источник"
    @Override
    public RotationSource getCachedSource() {
        return cachedSource;
    }

    // ВАЖНО: Кеш сумматора всегда валиден, так как он сам источник
    @Override
    public boolean isCacheValid(long currentTime) {
        return true;
    }

    @Override
    public void setCachedSource(@Nullable RotationSource source, long gameTime) {
        // Игнорируем внешнюю установку кеша, мы сами управляем им
    }

    private void updateCache() {
        this.cachedSource = new RotationSource(speed, torque);
    }

    @Override
    public void invalidateCache() {
        // Пусто, чтобы избежать рекурсии. Мы сами обновляем соседей через tick.
    }

    private void invalidateNeighborCaches() {
        if (level == null || level.isClientSide) return;
        Direction facing = getBlockState().getValue(AdderBlock.FACING);
        Direction outputSide = facing.getOpposite();
        BlockPos neighborPos = worldPosition.relative(outputSide);
        if (level.getBlockEntity(neighborPos) instanceof RotationalNode node) {
            node.invalidateCache();
        }
    }

    @Override
    public boolean canProvideSource(@Nullable Direction fromDir) {
        Direction facing = getBlockState().getValue(AdderBlock.FACING);
        Direction outputSide = facing.getOpposite();
        return fromDir == outputSide;
    }

    @Override
    public Direction[] getPropagationDirections(@Nullable Direction fromDir) {
        return new Direction[0];
    }




    // ========== Тик ==========
    public static void tick(Level level, BlockPos pos, BlockState state, AdderBlockEntity be) {
        if (level.isClientSide) return;

        Direction facing = state.getValue(AdderBlock.FACING);
        Direction outputSide = facing.getOpposite();

        long totalSpeed = 0;
        long totalTorque = 0;

        for (Direction dir : Direction.values()) {
            if (dir == outputSide) continue;

            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null) continue;

            RotationSource src = null;

            if (neighbor instanceof RotationalNode node && node.canProvideSource(dir.getOpposite())) {
                if (neighbor instanceof Rotational rot) {
                    src = new RotationSource(rot.getSpeed(), rot.getTorque());
                }
            }

            if (src == null) {
                src = RotationNetworkHelper.findSource(neighbor, dir.getOpposite());
            }

            if (src != null) {
                totalSpeed += src.speed();
                totalTorque = Math.max(totalTorque, src.torque());
            }
        }

        boolean changed = false;
        if (be.speed != totalSpeed) { be.speed = totalSpeed; changed = true; }
        if (be.torque != totalTorque) { be.torque = totalTorque; changed = true; }

        if (changed) {
            be.updateCache(); // Обновляем наш "источник"
            be.setChanged();
            be.sync();
            be.invalidateNeighborCaches(); // Будим выходной вал
        }
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