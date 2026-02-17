package com.smogline.block.entity.custom;

import com.smogline.api.rotation.RotationNetworkHelper;
import com.smogline.api.rotation.RotationSource;
import com.smogline.api.rotation.Rotational;
import com.smogline.api.rotation.RotationalNode;
import com.smogline.block.custom.rotation.TachometerBlock;
import com.smogline.block.custom.rotation.Mode;
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

public class TachometerBlockEntity extends BlockEntity implements RotationalNode {

    private long speed = 0;
    private long torque = 0;
    private int multiplier = 1; // 1,2,3

    // Кеш найденного источника
    private RotationSource cachedSource;
    private long cacheTimestamp;
    private static final long CACHE_LIFETIME = 10; // тиков (1 секунда)

    public TachometerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TACHOMETER_BE.get(), pos, state);
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
        if (this.cachedSource != null) {
            this.cachedSource = null;
            if (level != null && !level.isClientSide) {
                Direction facing = getBlockState().getValue(TachometerBlock.FACING);
                Direction left = TachometerBlock.getLeft(facing);
                Direction right = TachometerBlock.getRight(facing);
                for (Direction dir : new Direction[]{left, right}) {
                    if (dir != null) {
                        BlockPos neighborPos = worldPosition.relative(dir);
                        if (level.getBlockEntity(neighborPos) instanceof RotationalNode node) {
                            node.invalidateCache();
                        }
                    }
                }
            }
        }
    }

    /**
     * Тахометр сам по себе не является источником вращения.
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
        Direction facing = getBlockState().getValue(TachometerBlock.FACING);
        Direction left = TachometerBlock.getLeft(facing);
        Direction right = TachometerBlock.getRight(facing);

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

    // ========== Специфичные методы ==========
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

    /**
     * Вычисляет сигнал редстоуна (0-15) на основе текущего источника, multiplier и mode.
     */
    public int getRedstoneSignal() {
        if (cachedSource == null) return 0;
        long value = (getMode() == Mode.SPEED) ? cachedSource.speed() : cachedSource.torque();
        long raw = value * multiplier / 20;
        return (int) Math.min(15, raw);
    }

    // ========== Тик ==========
    public static void tick(Level level, BlockPos pos, BlockState state, TachometerBlockEntity be) {
        if (level.isClientSide) return;

        long currentTime = level.getGameTime();

        // Если кеш устарел или отсутствует – выполняем поиск
        if (!be.isCacheValid(currentTime)) {
            RotationSource source = RotationNetworkHelper.findSource(be, null, new HashSet<>(), 0);
            be.setCachedSource(source, currentTime);
            // При изменении источника обновляем редстоун
            be.updateRedstone();
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
            be.updateRedstone(); // возможно, изменение значений тоже требует обновления редстоуна
        }
    }

    private void updateRedstone() {
        if (level != null && !level.isClientSide) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    // ========== NBT и синхронизация ==========
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        tag.putInt("Multiplier", multiplier);
        // Кеш не сохраняем
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        multiplier = tag.getInt("Multiplier");
        if (multiplier < 1 || multiplier > 3) multiplier = 1;
        // Игнорируем старый тег HasSource, кеш сбрасывается
        cachedSource = null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        tag.putInt("Multiplier", multiplier);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
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

    /**
     * Вспомогательный метод для проверки наличия источника (для удобства).
     */
    public boolean hasSource() {
        return cachedSource != null;
    }
}