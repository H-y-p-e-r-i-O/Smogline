package com.smogline.block.entity.custom;

import com.smogline.api.rotation.RotationNetworkHelper;
import com.smogline.api.rotation.RotationSource;
import com.smogline.api.rotation.RotationalNode;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class GearPortBlockEntity extends BlockEntity implements RotationalNode {

    private long speed = 0;
    private long torque = 0;

    private Direction firstPort = null;
    private Direction secondPort = null;

    private RotationSource cachedSource;
    private long cacheTimestamp;
    private static final long CACHE_LIFETIME = 10;

    public GearPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEAR_PORT_BE.get(), pos, state);
    }

    // ... handleScrewdriverClick и геттеры портов без изменений ...
    public String handleScrewdriverClick(Direction face, boolean shift) {
        // (Твой код handleScrewdriverClick)
        // ...
        // ВАЖНО: После изменения портов нужно инвалидировать кеш!
        invalidateCache();
        return "Порт изменен"; // Упростил для примера, оставь свою логику
    }
    public Direction getFirstPort() { return firstPort; }
    public Direction getSecondPort() { return secondPort; }

    // ========== Rotational ==========
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = speed; setChanged(); sync(); invalidateNeighborCaches(); }
    @Override public void setTorque(long torque) { this.torque = torque; setChanged(); sync(); invalidateNeighborCaches(); }
    @Override public long getMaxSpeed() { return 0; }
    @Override public long getMaxTorque() { return 0; }

    // ========== RotationalNode ==========
    @Override @Nullable public RotationSource getCachedSource() { return cachedSource; }
    @Override public void setCachedSource(@Nullable RotationSource source, long gameTime) {
        this.cachedSource = source;
        this.cacheTimestamp = gameTime;
    }
    @Override public boolean isCacheValid(long currentTime) {
        return cachedSource != null && (currentTime - cacheTimestamp) <= CACHE_LIFETIME;
    }

    @Override
    public void invalidateCache() {
        // Проверка обязательна, чтобы избежать бесконечной рекурсии!
        if (this.cachedSource != null) {
            this.cachedSource = null;
            if (level != null && !level.isClientSide) {
                invalidateNeighborCaches();
            }
        }
    }

    private void invalidateNeighborCaches() {
        if (level == null || level.isClientSide) return;
        if (firstPort != null) invalidateNodeAt(firstPort);
        if (secondPort != null) invalidateNodeAt(secondPort);
    }

    private void invalidateNodeAt(Direction dir) {
        BlockPos neighborPos = worldPosition.relative(dir);
        if (level.getBlockEntity(neighborPos) instanceof RotationalNode node) {
            node.invalidateCache();
        }
    }

    @Override
    public boolean canProvideSource(@Nullable Direction fromDir) {
        return false;
    }

    @Override
    public Direction[] getPropagationDirections(@Nullable Direction fromDir) {
        if (fromDir == null) {
            List<Direction> list = new ArrayList<>();
            if (firstPort != null) list.add(firstPort);
            if (secondPort != null) list.add(secondPort);
            return list.toArray(new Direction[0]);
        } else {
            if (fromDir.equals(firstPort) && secondPort != null) return new Direction[]{secondPort};
            else if (fromDir.equals(secondPort) && firstPort != null) return new Direction[]{firstPort};
            else return new Direction[0];
        }
    }

    // ========== Тик ==========
    public static void tick(Level level, BlockPos pos, BlockState state, GearPortBlockEntity be) {
        if (level.isClientSide) return;

        if (be.firstPort == null || be.secondPort == null) {
            if (be.speed != 0) be.setSpeed(0);
            if (be.torque != 0) be.setTorque(0);
            return;
        }

        long currentTime = level.getGameTime();
        if (!be.isCacheValid(currentTime)) {
            // Ищем источники с обоих портов
            RotationSource s1 = RotationNetworkHelper.findSource(be, be.firstPort); // Притворяемся, что пришли с 1 порта
            RotationSource s2 = RotationNetworkHelper.findSource(be, be.secondPort);

            // Логика конфликта
            RotationSource finalSource = null;
            if (s1 != null && s2 != null) {
                // Конфликт -> 0
                finalSource = new RotationSource(0, 0);
            } else if (s1 != null) {
                finalSource = s1;
            } else if (s2 != null) {
                finalSource = s2;
            }

            be.setCachedSource(finalSource, currentTime);
        }

        RotationSource src = be.getCachedSource();
        long newSpeed = (src != null) ? src.speed() : 0;
        long newTorque = (src != null) ? src.torque() : 0;

        boolean changed = false;
        if (be.speed != newSpeed) { be.speed = newSpeed; changed = true; }
        if (be.torque != newTorque) { be.torque = newTorque; changed = true; }

        if (changed) {
            be.setChanged();
            be.sync();
            be.invalidateNeighborCaches();
        }
    }

    // ========== NBT и синхронизация ==========
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        if (firstPort != null) tag.putInt("FirstPort", firstPort.ordinal());
        if (secondPort != null) tag.putInt("SecondPort", secondPort.ordinal());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        firstPort = tag.contains("FirstPort") ? Direction.from3DDataValue(tag.getInt("FirstPort")) : null;
        secondPort = tag.contains("SecondPort") ? Direction.from3DDataValue(tag.getInt("SecondPort")) : null;
        cachedSource = null; // сброс кеша
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        if (firstPort != null) tag.putInt("FirstPort", firstPort.ordinal());
        if (secondPort != null) tag.putInt("SecondPort", secondPort.ordinal());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        firstPort = tag.contains("FirstPort") ? Direction.from3DDataValue(tag.getInt("FirstPort")) : null;
        secondPort = tag.contains("SecondPort") ? Direction.from3DDataValue(tag.getInt("SecondPort")) : null;
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

    public boolean hasPortOnSide(Direction side) {
        return side.equals(firstPort) || side.equals(secondPort);
    }
}